package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.websocket.BlazeMessage

class SendPlaintextJob(
    private val blazeMessage: BlazeMessage,
    priority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(priority).groupBy("send_message_group")
        .requireWebSocketConnected().persist(), blazeMessage.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        deliverNoThrow(blazeMessage)
    }

    override fun cancel() {
    }
}
