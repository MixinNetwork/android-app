package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class MarkReadJob(val conversationId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).setGroupId(conversationId).addTags(GROUP).persist()
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "MarkReadJob"
    }

    override fun onRun() {
    }
}
