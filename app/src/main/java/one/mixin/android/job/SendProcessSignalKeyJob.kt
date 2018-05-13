package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.clearParticipant
import one.mixin.android.util.Session
import one.mixin.android.websocket.BlazeMessageData
import java.util.UUID

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
            val result = redirectSendSenderKey(data.conversationId, data.userId)
            if (!result) {
                sendNoKeyMessage(data.conversationId, data.userId)
            }
        } else if (action == ProcessSignalKeyAction.REMOVE_PARTICIPANT) {
            val accountId = Session.getAccountId()
            appDatabase.clearParticipant(data.conversationId, participantId!!)
            signalProtocol.clearSenderKey(data.conversationId, accountId!!)
        } else if (action == ProcessSignalKeyAction.ADD_PARTICIPANT) {
            sendSenderKey(data.conversationId, participantId!!)
        }
    }

    override fun cancel() {
    }
}

enum class ProcessSignalKeyAction { ADD_PARTICIPANT, REMOVE_PARTICIPANT, RESEND_KEY }