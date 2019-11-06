package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import java.util.UUID
import one.mixin.android.db.clearParticipant
import one.mixin.android.util.Session
import one.mixin.android.websocket.BlazeMessageData

class SendProcessSignalKeyJob(
    val data: BlazeMessageData,
    val action: ProcessSignalKeyAction,
    val participantId: String? = null,
    priority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(priority).groupBy("send_message_group").requireWebSocketConnected().persist(),
    UUID.randomUUID().toString()) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        if (action == ProcessSignalKeyAction.RESEND_KEY) {
            val result = sendSenderKey(data.conversationId, data.userId, data.sessionId, true)
            if (!result) {
                sendNoKeyMessage(data.conversationId, data.userId)
            }
        } else if (action == ProcessSignalKeyAction.REMOVE_PARTICIPANT) {
            val accountId = Session.getAccountId()
            appDatabase.clearParticipant(data.conversationId, participantId!!)
            signalProtocol.clearSenderKey(data.conversationId, accountId!!)
        } else if (action == ProcessSignalKeyAction.ADD_PARTICIPANT) {
            sendSenderKey(data.conversationId, participantId!!)
        } else if (action == ProcessSignalKeyAction.SESSION_SYNC) {
            participantId?.let {
                sendSessionSyncMessage(it)
            }
        }
    }

    override fun cancel() {
    }
}

enum class ProcessSignalKeyAction { ADD_PARTICIPANT, REMOVE_PARTICIPANT, RESEND_KEY, SESSION_SYNC }
