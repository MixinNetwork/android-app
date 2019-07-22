package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.websocket.BlazeMessage

class SendSessionStatusMessageJob(
    private val bm: BlazeMessage,
    priority: Int = PRIORITY_SEND_SESSION_MESSAGE
) : MixinJob(Params(priority).addTags(bm.id).groupBy("send_session_message_group").requireWebSocketConnected().persist(), bm.id) {

    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        deliver(bm)
    }
}
