package one.mixin.android.messenger

import android.database.sqlite.SQLiteBlobTooBigException
import androidx.room.InvalidationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertNoReplace
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.job.DecryptCallMessage
import one.mixin.android.job.DecryptMessage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.pendingMessageStatusLruCache
import one.mixin.android.session.CurrentUserScopeManager
import one.mixin.android.session.Session
import one.mixin.android.util.FLOOD_THREAD
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.PENDING_DB_THREAD
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.isMine
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import timber.log.Timber
import java.io.IOException

class HedwigImp(
    private val currentUserScopeManager: CurrentUserScopeManager,
    private val conversationService: ConversationService,
    private val circleService: CircleService,
    private val jobManager: MixinJobManager,
    private val callState: CallStateLiveData,
    private val lifecycleScope: CoroutineScope,
) : Hedwig {
    private fun mixinDatabase(): MixinDatabase = currentUserScopeManager.getMixinDatabase()

    private fun pendingDatabase(): PendingDatabase = currentUserScopeManager.getPendingDatabase()

    override fun takeOff() {
        startObserveFlood()
        startObservePending()
    }

    override fun land() {
        stopObserveFlood()
        stopObservePending()
        floodJob?.cancel()
        pendingJob?.cancel()
    }

    private fun messageDao() = mixinDatabase().messageDao()

    private fun conversationDao() = mixinDatabase().conversationDao()

    private fun participantDao() = mixinDatabase().participantDao()

    private fun participantSessionDao() = mixinDatabase().participantSessionDao()

    private fun circleConversationDao() = mixinDatabase().circleConversationDao()

    private fun circleDao() = mixinDatabase().circleDao()

    private fun conversationExtDao() = mixinDatabase().conversationExtDao()

    private fun remoteMessageStatusDao() = mixinDatabase().remoteMessageStatusDao()

    private fun jobDao() = pendingDatabase().jobDao()

    private var floodJob: Job? = null
    private var floodObservedDatabase: PendingDatabase? = null
    private val floodObserver =
        object : InvalidationTracker.Observer("flood_messages") {
            override fun onInvalidated(tables: Set<String>) {
                runFloodJob()
            }
        }

    private fun startObserveFlood() {
        runFloodJob()
        val db = pendingDatabase()
        floodObservedDatabase = db
        db.addObserver(floodObserver)
    }

    private fun stopObserveFlood() {
        floodObservedDatabase?.removeObserver(floodObserver)
        floodObservedDatabase = null
    }

    @Synchronized
    private fun runFloodJob() {
        if (floodJob?.isActive == true) {
            return
        }
        floodJob =
            lifecycleScope.launch(FLOOD_THREAD) {
                try {
                    processFloodMessage()
                } catch (e: NullPointerException) {
                    // Hackfix: Samsung mobile phone throws NullPointerException equals SQLiteBlobTooBigException
                    handleBlobTooBigError(e)
                } catch (e: SQLiteBlobTooBigException) {
                    handleBlobTooBigError(e)
                } catch (e: Exception) {
                    Timber.e(e)
                    reportException(e)
                    runFloodJob()
                }
            }
    }

    private suspend fun handleBlobTooBigError(e: Exception) {
        val db = pendingDatabase()
        val messageIds = db.findMessageIdsLimit10()
        val maxLengthId = db.findMaxLengthMessageId(messageIds)
        if (maxLengthId != null) {
            db.deleteFloodMessageById(maxLengthId)
            jobDao().insertNoReplace(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(maxLengthId, MessageStatus.DELIVERED.name)))
        }
        Timber.e(e)
        runFloodJob()
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    private val messageDecrypt by lazy { DecryptMessage(lifecycleScope) }
    private val callMessageDecrypt by lazy { DecryptCallMessage(callState, lifecycleScope) }

    private tailrec suspend fun processFloodMessage(): Boolean {
        val db = pendingDatabase()
        val messages = db.findFloodMessages()
        return if (messages.isNotEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                db.deleteFloodMessage(message)
                pendingMessageStatusLruCache.remove(data.messageId)
            }
            processFloodMessage()
        } else {
            false
        }
    }

    private var pendingJob: Job? = null
    private var pendingObservedDatabase: PendingDatabase? = null
    private val pendingObserver =
        object : InvalidationTracker.Observer("pending_messages") {
            override fun onInvalidated(tables: Set<String>) {
                runPendingJob()
            }
        }

    private fun startObservePending() {
        runPendingJob()
        val db = pendingDatabase()
        pendingObservedDatabase = db
        db.addObserver(pendingObserver)
    }

    private fun stopObservePending() {
        pendingObservedDatabase?.removeObserver(pendingObserver)
        pendingObservedDatabase = null
    }

    @Synchronized
    private fun runPendingJob() {
        if (pendingJob?.isActive == true) {
            return
        }
        pendingJob =
            lifecycleScope.launch(PENDING_DB_THREAD) {
                try {
                    val db = pendingDatabase()
                    val list = db.getPendingMessages()
                    list.groupBy { it.conversationId }.filter { (conversationId, _) ->
                        conversationId != SYSTEM_USER && conversationId != Session.getAccountId() && checkConversation(conversationId) != null
                    }.forEach { (conversationId, messages) ->
                        messageDao().insertList(messages)
                        db.deletePendingMessageByIds(messages.map { it.messageId })
                        conversationExtDao().increment(conversationId, messages.size)
                        messages.filter { message ->
                            !message.isMine() && message.status != MessageStatus.READ.name && (pendingMessageStatusLruCache[message.messageId] != MessageStatus.READ.name)
                        }.map { message ->
                            RemoteMessageStatus(message.messageId, message.conversationId, MessageStatus.DELIVERED.name)
                        }.let { remoteMessageStatus ->
                            remoteMessageStatusDao().insertList(remoteMessageStatus)
                        }
                        messages.last().let { message ->
                            conversationDao().updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
                        }
                        remoteMessageStatusDao().updateConversationUnseen(conversationId)
                        MessageFlow.insert(conversationId, messages.map { it.messageId })
                    }
                    if (list.size == 100) {
                        runPendingJob()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    runPendingJob()
                }
            }
    }

    private fun checkConversation(conversationId: String): Conversation? {
        var conversation = conversationDao().findConversationById(conversationId)
        if (conversation == null) conversation = refreshConversation(conversationId)
        return conversation
    }

    private fun refreshConversation(conversationId: String): Conversation? {
        try {
            val call = conversationService.getConversation(conversationId).execute()
            val response = call.body()
            if (response != null && response.isSuccess) {
                response.data?.let { conversationData ->
                    val status =
                        if (conversationData.participants.find { Session.getAccountId() == it.userId } != null) {
                            ConversationStatus.SUCCESS.ordinal
                        } else {
                            ConversationStatus.QUIT.ordinal
                        }
                    var ownerId: String = conversationData.creatorId
                    if (conversationData.category == ConversationCategory.CONTACT.name) {
                        ownerId = conversationData.participants.find { it.userId != Session.getAccountId() }!!.userId
                    } else if (conversationData.category == ConversationCategory.GROUP.name) {
                        jobManager.addJobInBackground(RefreshUserJob(listOf(conversationData.creatorId)))
                    }
                    conversationDao().upsert(
                        ConversationBuilder(conversationData.conversationId, conversationData.createdAt, status)
                            .setOwnerId(ownerId)
                            .setCategory(conversationData.category)
                            .setName(conversationData.name)
                            .setAnnouncement(conversationData.announcement)
                            .setMuteUntil(conversationData.muteUntil)
                            .setExpireIn(conversationData.expireIn).build(),
                    )
                    val remote = mutableListOf<Participant>()
                    val conversationUserIds = mutableListOf<String>()
                    for (p in conversationData.participants) {
                        remote.add(Participant(conversationId, p.userId, p.role, p.createdAt!!))
                        conversationUserIds.add(p.userId)
                    }
                    participantDao().replaceAll(conversationId, remote)

                    if (conversationUserIds.isNotEmpty()) {
                        jobManager.addJobInBackground(RefreshUserJob(conversationUserIds, conversationId))
                    }

                    val sessionParticipants =
                        conversationData.participantSessions?.map {
                            ParticipantSession(conversationId, it.userId, it.sessionId, publicKey = it.publicKey)
                        }
                    sessionParticipants?.let {
                        participantSessionDao().replaceAll(conversationId, it)
                    }

                    conversationData.circles?.let { circles ->
                        circles.forEach {
                            val circle = circleDao().findCircleById(it.circleId)
                            if (circle == null) {
                                val circleResponse = circleService.getCircle(it.circleId).execute().body()
                                if (circleResponse?.isSuccess == true) {
                                    circleResponse.data?.let { item ->
                                        circleDao().insert(item)
                                    }
                                }
                            }
                            circleConversationDao().insertUpdate(it)
                        }
                    }
                }
                return conversationDao().findConversationById(conversationId)
            } else {
                return null
            }
        } catch (_: IOException) {
            return null
        }
    }
}
