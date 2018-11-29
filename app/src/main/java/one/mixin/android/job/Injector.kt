package one.mixin.android.job

import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.JsonElement
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.db.RatchetSenderKeyDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ResendMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.UserDao
import one.mixin.android.di.Injectable
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.createConversation
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.worker.RefreshConversationWorker
import java.io.IOException
import javax.inject.Inject

open class Injector : Injectable {
    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var messageHistoryDao: MessageHistoryDao
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var jobDao: JobDao
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var conversationDao: ConversationDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var snapshotDao: SnapshotDao
    @Inject
    lateinit var assetDao: AssetDao
    @Inject
    lateinit var chatWebSocket: ChatWebSocket
    @Inject
    lateinit var stickerDao: StickerDao
    @Inject
    lateinit var signalProtocol: SignalProtocol
    @Inject
    lateinit var ratchetSenderKeyDao: RatchetSenderKeyDao
    @Inject
    lateinit var resendMessageDao: ResendMessageDao
    @Inject
    lateinit var userApi: UserService
    @Inject
    lateinit var conversationService: ConversationService

    init {
        MixinApplication.get().appComponent.inject(this)
    }

    protected fun signalKeysChannel(blazeMessage: BlazeMessage): JsonElement? {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            Thread.sleep(SLEEP_MILLIS)
            return signalKeysChannel(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == ErrorHandler.FORBIDDEN) {
                null
            } else {
                Thread.sleep(SLEEP_MILLIS)
                signalKeysChannel(blazeMessage)
            }
        }
        return bm.data
    }

    protected fun syncConversation(data: BlazeMessageData) {
        var conversation = conversationDao.getConversation(data.conversationId)
        if (conversation == null) {
            conversation = createConversation(data.conversationId, null, data.userId, ConversationStatus.START.ordinal)
            conversationDao.insert(conversation)
            refreshConversation(data.conversationId)
        }
        if (conversation.status == ConversationStatus.START.ordinal) {
            WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshConversationWorker>(
                workDataOf(RefreshConversationWorker.CONVERSATION_ID to data.conversationId))
        }
    }

    protected fun isExistMessage(messageId: String): Boolean {
        val id = messageDao.findMessageIdById(messageId)
        val messageHistory = messageHistoryDao.findMessageHistoryById(messageId)
        return id != null || messageHistory != null
    }

    private fun refreshConversation(conversationId: String) {
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
                    }
                    conversationDao.updateConversation(conversationData.conversationId, ownerId, conversationData.category, conversationData.name,
                        conversationData.announcement, conversationData.muteUntil, conversationData.createdAt, status)
                }
            }
        } catch (e: IOException) {
        }
    }
}