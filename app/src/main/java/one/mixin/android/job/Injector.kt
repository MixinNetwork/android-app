package one.mixin.android.job

import com.google.gson.Gson
import com.google.gson.JsonElement
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.db.RatchetSenderKeyDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ResendMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.UserDao
import one.mixin.android.di.Injectable
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.TransferPlainData
import java.util.UUID
import javax.inject.Inject

open class Injector : Injectable {
    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var messageHistoryDao: MessageHistoryDao
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var conversationDao: ConversationDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var snapshotDao: SnapshotDao
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

    protected fun sendNoKeyMessage(conversationId: String, recipientId: String) {
        val plainText = Gson().toJson(TransferPlainData(PlainDataAction.NO_KEY.name))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val params = BlazeMessageParam(conversationId, recipientId, UUID.randomUUID().toString(),
            MessageCategory.PLAIN_JSON.name, encoded, MessageStatus.SENDING.name)
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, params)
        signalKeysChannel(bm)
    }
}