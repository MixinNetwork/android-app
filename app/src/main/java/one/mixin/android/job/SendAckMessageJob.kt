package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import java.util.UUID
import kotlinx.coroutines.runBlocking
import one.mixin.android.websocket.BlazeAckMessage

class SendAckMessageJob(
    private val ack: List<BlazeAckMessage>,
    priority: Int = PRIORITY_ACK_MESSAGE
) : MixinJob(Params(priority).groupBy("send_ack_message").requireWebSocketConnected().persist(), UUID.randomUUID().toString()) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() = runBlocking {
        messageService.acknowledgements(ack)
        return@runBlocking
    }

    override fun cancel() {
    }
}
