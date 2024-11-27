package one.mixin.android.job

import android.os.SystemClock
import com.google.gson.JsonElement
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.EncryptedProtocol
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.db.RatchetSenderKeyDao
import one.mixin.android.db.AppDao
import one.mixin.android.db.CircleConversationDao
import one.mixin.android.db.CircleDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.HyperlinkDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.ResendSessionMessageDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TraceDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.pending.PendingMessageDao
import one.mixin.android.di.ApplicationScope
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.User
import one.mixin.android.vo.createConversation
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.ChatWebSocket
import java.io.IOException
import javax.inject.Inject

open class Injector {
    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var databaseProvider: DatabaseProvider

    @Inject
    lateinit var circleService: CircleService

    @Inject
    lateinit var chatWebSocket: ChatWebSocket

    @Inject
    lateinit var signalProtocol: SignalProtocol

    @Inject
    lateinit var encryptedProtocol: EncryptedProtocol

    @Inject
    lateinit var conversationService: ConversationService

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var ratchetSenderKeyDao: RatchetSenderKeyDao

    @Inject
    lateinit var resendMessageDao: ResendSessionMessageDao

    @ApplicationScope
    @Transient
    @Inject
    lateinit var applicationScope: CoroutineScope

    val database: MixinDatabase by lazy { databaseProvider.getMixinDatabase() }
    val ftsDatabase: FtsDatabase by lazy { databaseProvider.getFtsDatabase() }

    val messageDao: MessageDao by lazy { databaseProvider.getMixinDatabase().messageDao() }
    val pendingMessagesDao: PendingMessageDao by lazy { databaseProvider.getPendingDatabase().pendingMessageDao() }
    val messageHistoryDao: MessageHistoryDao by lazy { databaseProvider.getMixinDatabase().messageHistoryDao() }
    val userDao: UserDao by lazy { databaseProvider.getMixinDatabase().userDao() }
    val appDao: AppDao by lazy { databaseProvider.getMixinDatabase().appDao() }
    val jobDao: JobDao by lazy { databaseProvider.getMixinDatabase().jobDao() }
    val conversationDao: ConversationDao by lazy { databaseProvider.getMixinDatabase().conversationDao() }
    val conversationExtDao: ConversationExtDao by lazy { databaseProvider.getMixinDatabase().conversationExtDao() }
    val participantDao: ParticipantDao by lazy { databaseProvider.getMixinDatabase().participantDao() }
    val participantSessionDao: ParticipantSessionDao by lazy { databaseProvider.getMixinDatabase().participantSessionDao() }
    val snapshotDao: SnapshotDao by lazy { databaseProvider.getMixinDatabase().snapshotDao() }
    val safeSnapshotDao: SafeSnapshotDao by lazy { databaseProvider.getMixinDatabase().safeSnapshotDao() }
    val tokenDao: TokenDao by lazy { databaseProvider.getMixinDatabase().tokenDao() }
    val circleDao: CircleDao by lazy { databaseProvider.getMixinDatabase().circleDao() }
    val circleConversationDao: CircleConversationDao by lazy { databaseProvider.getMixinDatabase().circleConversationDao() }
    val traceDao: TraceDao by lazy { databaseProvider.getMixinDatabase().traceDao() }
    val stickerDao: StickerDao by lazy { databaseProvider.getMixinDatabase().stickerDao() }
    val messageMentionDao: MessageMentionDao by lazy { databaseProvider.getMixinDatabase().messageMentionDao() }
    val hyperlinkDao: HyperlinkDao by lazy { databaseProvider.getMixinDatabase().hyperlinkDao() }
    val transcriptMessageDao: TranscriptMessageDao by lazy { databaseProvider.getMixinDatabase().transcriptDao() }
    val pinMessageDao: PinMessageDao by lazy { databaseProvider.getMixinDatabase().pinMessageDao() }
    val remoteMessageStatusDao: RemoteMessageStatusDao by lazy { databaseProvider.getMixinDatabase().remoteMessageStatusDao() }
    val expiredMessageDao: ExpiredMessageDao by lazy { databaseProvider.getMixinDatabase().expiredMessageDao() }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface InjectorEntryPoint {
        fun inject(injector: Injector)
    }

    init {
        val entryPoint = EntryPointAccessors.fromApplication(MixinApplication.get().applicationContext, InjectorEntryPoint::class.java)
        entryPoint.inject(this)
    }

    protected tailrec fun signalKeysChannel(blazeMessage: BlazeMessage): JsonElement? {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            SystemClock.sleep(SLEEP_MILLIS)
            return signalKeysChannel(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == ErrorHandler.FORBIDDEN) {
                null
            } else {
                SystemClock.sleep(SLEEP_MILLIS)
                return signalKeysChannel(blazeMessage)
            }
        }
        return bm.data
    }

    protected fun syncUser(
        userId: String,
        conversationId: String? = null,
        forceSync: Boolean = true,
    ): User? {
        var user = userDao.findUser(userId)
        if (user == null && forceSync) {
            try {
                val call = userService.getUserById(userId).execute()
                val response = call.body()
                if (response != null && response.isSuccess && response.data != null) {
                    response.data?.let { u ->
                        userDao.insertUpdate(u, appDao)
                        user = u
                    }
                }
            } catch (e: IOException) {
            }
        }
        if (user == null) {
            jobManager.addJobInBackground(RefreshUserJob(arrayListOf(userId), conversationId = conversationId))
        }
        return user
    }

    protected fun syncConversation(data: BlazeMessageData) {
        if (data.conversationId == SYSTEM_USER || data.conversationId == Session.getAccountId()) {
            return
        }
        var conversation = conversationDao.findConversationById(data.conversationId)
        var status = conversation?.status ?: ConversationStatus.START.ordinal
        if (conversation == null) {
            conversation = createConversation(data.conversationId, null, data.userId, ConversationStatus.START.ordinal)
            conversationDao.upsert(conversation)
            status = refreshConversation(data.conversationId)
        }
        if (status == ConversationStatus.START.ordinal) {
            jobManager.addJobInBackground(RefreshConversationJob(data.conversationId))
        }
    }

    protected open fun isExistMessage(messageId: String): Boolean =
        messageDao.findMessageIdById(messageId) != null || messageHistoryDao.findMessageHistoryById(messageId) != null

    private fun refreshConversation(conversationId: String): Int {
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
                        syncUser(conversationData.creatorId)
                    }
                    conversationDao.updateConversation(
                        conversationData.conversationId,
                        ownerId,
                        conversationData.category,
                        conversationData.name,
                        conversationData.announcement,
                        conversationData.muteUntil,
                        conversationData.createdAt,
                        conversationData.expireIn,
                        status,
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
                    return status
                }
            }
        } catch (_: IOException) {
        }
        return ConversationStatus.START.ordinal
    }
}
