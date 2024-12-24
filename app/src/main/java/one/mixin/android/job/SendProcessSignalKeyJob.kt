package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.RxBus
import one.mixin.android.db.clearParticipant
import one.mixin.android.event.SenderKeyChange
import one.mixin.android.session.Session
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.websocket.BlazeMessageData
import java.util.UUID

class SendProcessSignalKeyJob(
    val data: BlazeMessageData,
    val action: ProcessSignalKeyAction,
    val participantId: String? = null,
    priority: Int = PRIORITY_SEND_MESSAGE,
) : MixinJob(
        Params(priority).groupBy("send_message_group").requireWebSocketConnected().persist(),
        UUID.randomUUID().toString(),
    ) {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        if (action == ProcessSignalKeyAction.RESEND_KEY) {
            val result = sendSenderKey(data.conversationId, data.userId, data.sessionId)
            if (!result) {
                sendNoKeyMessage(data.conversationId, data.userId)
            }
        } else if (action == ProcessSignalKeyAction.REMOVE_PARTICIPANT) {
            Session.getAccountId()?.let {
                database().clearParticipant(data.conversationId, participantId!!)
                signalProtocol.clearSenderKey(data.conversationId, it)
                RxBus.publish(SenderKeyChange(data.conversationId))
            }
        } else if (action == ProcessSignalKeyAction.ADD_PARTICIPANT) {
            val response = userService.fetchSessions(arrayListOf(participantId!!)).execute().body()
            if (response != null && response.isSuccess) {
                val ps =
                    response.data?.map { item ->
                        ParticipantSession(data.conversationId, item.userId, item.sessionId, publicKey = item.publicKey)
                    }
                if (!ps.isNullOrEmpty()) {
                    participantSessionDao().insertList(ps)
                }
                RxBus.publish(SenderKeyChange(data.conversationId, participantId))
            }
        }
    }

    override fun cancel() {
    }
}

enum class ProcessSignalKeyAction { ADD_PARTICIPANT, REMOVE_PARTICIPANT, RESEND_KEY }
