package one.mixin.android.messenger

import android.database.sqlite.SQLiteBlobTooBigException
import androidx.room.InvalidationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.CircleConversationDao
import one.mixin.android.db.CircleDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertNoReplace
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.job.DecryptCallMessage
import one.mixin.android.job.DecryptMessage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.pendingMessageStatusLruCache
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
    private val databaseProvider: DatabaseProvider,
    private val conversationService: ConversationService,
    private val circleService: CircleService,
    private val jobManager: MixinJobManager,
    private val callState: CallStateLiveData,
    private val lifecycleScope: CoroutineScope,
) : Hedwig {
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

    private val messageDao: MessageDao
        get() = databaseProvider.getMixinDatabase().messageDao()

    private val conversationDao: ConversationDao
        get() = databaseProvider.getMixinDatabase().conversationDao()
    private val conversationExtDao: ConversationExtDao
        get() = databaseProvider.getMixinDatabase().conversationExtDao()
    private val participantDao: ParticipantDao
        get() = databaseProvider.getMixinDatabase().participantDao()
    private val participantSessionDao: ParticipantSessionDao
        get() = databaseProvider.getMixinDatabase().participantSessionDao()
    private val circleConversationDao: CircleConversationDao
        get() = databaseProvider.getMixinDatabase().circleConversationDao()
    private val circleDao: CircleDao
        get() = databaseProvider.getMixinDatabase().circleDao()

    private val remoteMessageStatusDao: RemoteMessageStatusDao
        get() = databaseProvider.getMixinDatabase().remoteMessageStatusDao()

    private val jobDao: JobDao
        get() = databaseProvider.getPendingDatabase().jobDao()

    private var floodJob: Job? = null
    private val floodObserver =
        object : InvalidationTracker.Observer("flood_messages") {
            override fun onInvalidated(tables: Set<String>) {
                runFloodJob()
            }
        }

    private fun startObserveFlood() {
        runFloodJob()
        databaseProvider.getPendingDatabase().addObserver(floodObserver)
    }

    private fun stopObserveFlood() {
        databaseProvider.getPendingDatabase().removeObserver(floodObserver)
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
        val messageIds = databaseProvider.getPendingDatabase().findMessageIdsLimit10()
        val maxLengthId = databaseProvider.getPendingDatabase().findMaxLengthMessageId(messageIds)
        if (maxLengthId != null) {
            databaseProvider.getPendingDatabase().deleteFloodMessageById (maxLengthId)
            jobDao.insertNoReplace(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(maxLengthId, MessageStatus.DELIVERED.name)))
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
        val messages = databaseProvider.getPendingDatabase().findFloodMessages()
        return if (messages.isNotEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                databaseProvider.getPendingDatabase().deleteFloodMessage(message)
                pendingMessageStatusLruCache.remove(data.messageId)
            }
            processFloodMessage()
        } else {
            false
        }
    }

    private var pendingJob: Job? = null
    private val pendingObserver =
        object : InvalidationTracker.Observer("pending_messages") {
            override fun onInvalidated(tables: Set<String>) {
                runPendingJob()
            }
        }

    private fun startObservePending() {
        runPendingJob()
        databaseProvider.getPendingDatabase().addObserver(pendingObserver)
    }

    private fun stopObservePending() {
        databaseProvider.getPendingDatabase().removeObserver(pendingObserver)
    }

    @Synchronized
    private fun runPendingJob() {
        if (pendingJob?.isActive == true) {
            return
        }
        pendingJob =
            lifecycleScope.launch(PENDING_DB_THREAD) {
                try {
                    val list = databaseProvider.getPendingDatabase().getPendingMessages()
                    list.groupBy { it.conversationId }.filter { (conversationId, _) ->
                        conversationId != SYSTEM_USER && conversationId != Session.getAccountId() && checkConversation(conversationId) != null
                    }.forEach { (conversationId, messages) ->
                        messageDao.insertList(messages)
                        databaseProvider.getPendingDatabase().deletePendingMessageByIds(messages.map { it.messageId })
                        conversationExtDao.increment(conversationId, messages.size)
                        messages.filter { message ->
                            !message.isMine() && message.status != MessageStatus.READ.name && (pendingMessageStatusLruCache[message.messageId] != MessageStatus.READ.name)
                        }.map { message ->
                            RemoteMessageStatus(message.messageId, message.conversationId, MessageStatus.DELIVERED.name)
                        }.let { remoteMessageStatus ->
                            remoteMessageStatusDao.insertList(remoteMessageStatus)
                        }
                        messages.last().let { message ->
                            conversationDao.updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
                        }
                        remoteMessageStatusDao.updateConversationUnseen(conversationId)
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
        var conversation = conversationDao.findConversationById(conversationId)
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
                    conversationDao.upsert(
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
                    participantDao.replaceAll(conversationId, remote)

                    if (conversationUserIds.isNotEmpty()) {
                        jobManager.addJobInBackground(RefreshUserJob(conversationUserIds, conversationId))
                    }

                    val sessionParticipants =
                        conversationData.participantSessions?.map {
                            ParticipantSession(conversationId, it.userId, it.sessionId, publicKey = it.publicKey)
                        }
                    sessionParticipants?.let {
                        participantSessionDao.replaceAll(conversationId, it)
                    }

                    conversationData.circles?.let { circles ->
                        circles.forEach {
                            val circle = circleDao.findCircleById(it.circleId)
                            if (circle == null) {
                                val circleResponse = circleService.getCircle(it.circleId).execute().body()
                                if (circleResponse?.isSuccess == true) {
                                    circleResponse.data?.let { item ->
                                        circleDao.insert(item)
                                    }
                                }
                            }
                            circleConversationDao.insertUpdate(it)
                        }
                    }
                }
                return conversationDao.findConversationById(conversationId)
            } else {
                return null
            }
        } catch (_: IOException) {
            return null
        }
    }
}
