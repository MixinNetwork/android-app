package one.mixin.android.job

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
import one.mixin.android.util.ErrorHandler
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.ChatWebSocket
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
    lateinit var jobDao: JobDao
    @Inject
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
    lateinit var conversationApi: ConversationService
    @Inject
    lateinit var userApi: UserService

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
}