package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.absolutePath
import timber.log.Timber
import java.io.File

class MediaMessageDeleteJob(private val messages: List<MediaMessageMinimal>) :
    BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("media_message_delete").persist()) {
    private val TAG = MediaMessageDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "AttachmentDownloadJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        messages.forEach { message ->
            val path = message.absolutePath() ?: return
            val file = File(path)
            file.deleteOnExit()
            Timber.e("$TAG delete $path")
        }
    }
}
