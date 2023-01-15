package one.mixin.android.messenger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.job.DecryptCallMessage
import one.mixin.android.job.DecryptMessage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.pendingMessageStatusMap
import one.mixin.android.session.Session
import one.mixin.android.util.FLOOD_THREAD
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.PENDING_DB_THREAD
import one.mixin.android.util.chat.InvalidateFlow
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
import one.mixin.android.vo.isMine
import one.mixin.android.websocket.BlazeMessageData
import timber.log.Timber
import java.io.IOException

class HedwigImp(
    private val mixinDatabase: MixinDatabase,
    private val pendingDatabase: PendingDatabase,
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
    }

    private val messageDao by lazy {
        mixinDatabase.messageDao()
    }

    private val conversationDao by lazy {
        mixinDatabase.conversationDao()
    }
    private val participantDao by lazy {
        mixinDatabase.participantDao()
    }
    private val participantSessionDao by lazy {
        mixinDatabase.participantSessionDao()
    }
    private val circleConversationDao by lazy {
        mixinDatabase.circleConversationDao()
    }
    private val circleDao by lazy {
        mixinDatabase.circleDao()
    }

    private val conversationExtDao by lazy {
        mixinDatabase.conversationExtDao()
    }

    private val remoteMessageStatusDao by lazy {
        mixinDatabase.remoteMessageStatusDao()
    }

    private var floodJob: Job? = null

    private fun startObserveFlood() {
        runFloodJob()
    }

    private fun stopObserveFlood() {
        floodJob?.cancel()
    }

    @Synchronized
    private fun runFloodJob() {
        if (floodJob?.isActive == true) return
        floodJob = lifecycleScope.launch(FLOOD_THREAD) {
            pendingDatabase.collectFloodMessages { messages ->
                Timber.e("collect flood ${messages.size}")
                messages.forEach { message ->
                    val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                    if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                        callMessageDecrypt.onRun(data)
                    } else {
                        messageDecrypt.onRun(data)
                    }
                    pendingDatabase.deleteFloodMessage(message)
                    pendingMessageStatusMap.remove(data.messageId)
                }
            }
        }
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    private val messageDecrypt by lazy { DecryptMessage(lifecycleScope) }
    private val callMessageDecrypt by lazy { DecryptCallMessage(callState, lifecycleScope) }

    private var pendingJob: Job? = null
    private fun startObservePending() {
        runPendingJob()
    }

    private fun stopObservePending() {
        pendingJob?.cancel()
    }
    private fun runPendingJob() {
        if (pendingJob?.isActive == true) return
        pendingJob = lifecycleScope.launch(PENDING_DB_THREAD) {
            pendingDatabase.collectPendingMessages { list ->
                Timber.e("collect pending ${list.size}")
                try {
                    list.groupBy { it.conversationId }.filter { (conversationId, _) ->
                        conversationId != SYSTEM_USER && conversationId != Session.getAccountId() && checkConversation(conversationId) != null
                    }.forEach { (conversationId, messages) ->
                        messageDao.insertList(messages)
                        pendingDatabase.deletePendingMessageByIds(messages.map { it.messageId })
                        conversationExtDao.increment(conversationId, messages.size)
                        messages.filter { message -> !message.isMine() }.groupBy { m ->
                            m.status == MessageStatus.READ.name
                        }.forEach { (b, messages) ->
                            if (b) {
                                messages.map { it.messageId }.let { ids ->
                                    remoteMessageStatusDao.deleteByMessageIds(ids)
                                }
                            } else {
                                messages.map { message ->
                                    RemoteMessageStatus(message.messageId, message.conversationId, MessageStatus.DELIVERED.name)
                                }.let { remoteMessageStatus ->
                                    remoteMessageStatusDao.insertList(remoteMessageStatus)
                                }
                            }
                        }
                        remoteMessageStatusDao.updateConversationUnseen(conversationId)
                        messages.last().let { message ->
                            conversationDao.updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
                        }
                        InvalidateFlow.emit(conversationId)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
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
                    val status = if (conversationData.participants.find { Session.getAccountId() == it.userId } != null) {
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
                    conversationDao.insert(
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

                    val sessionParticipants = conversationData.participantSessions?.map {
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
