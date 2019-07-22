package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.websocket.BlazeMessage

class SendAckMessageJob(private val blazeMessage: BlazeMessage, priority: Int = PRIORITY_ACK_MESSAGE) :
    MixinJob(Params(priority).addTags(blazeMessage.id).groupBy("send_ack_message")
        .requireWebSocketConnected().persist(), blazeMessage.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        deliver(blazeMessage)
    }

    override fun cancel() {
    }
}
