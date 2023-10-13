package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import java.io.File
import one.mixin.android.extension.getFilePath

class AttachmentDeleteJob(private vararg val mediaUrls: String) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("attachment_delete").persist()) {

    private val TAG = AttachmentDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "AttachmentDownloadJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        mediaUrls.forEach { mediaUrl ->
            mediaUrl.getFilePath()?.let { path ->
                val file = File(path)
                if (file.exists() && file.isFile) {
                    file.delete()
                }
            }
        }
    }
}
