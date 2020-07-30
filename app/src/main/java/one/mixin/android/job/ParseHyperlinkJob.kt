package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class ParseHyperlinkJob(private val hyperlink: String, private val messageId: String) :
    BaseJob(Params(PRIORITY_BACKGROUND).groupBy("parse_hyperlink_group").persist().requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
    }

    override fun getRetryLimit(): Int {
        return 1
    }
}
