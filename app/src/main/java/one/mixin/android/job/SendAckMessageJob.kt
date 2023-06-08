package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.websocket.BlazeAckMessage
import ulid.ULID

class SendAckMessageJob(
    private val ack: List<BlazeAckMessage>,
    priority: Int = PRIORITY_ACK_MESSAGE,
) : MixinJob(Params(priority).groupBy("send_ack_message").requireWebSocketConnected().persist(), ULID.randomULID()) {

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
