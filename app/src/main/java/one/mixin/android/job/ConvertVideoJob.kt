package one.mixin.android.job

import android.net.Uri
import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getFileNameNoEx
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getVideoModel
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.video.MediaController
import one.mixin.android.util.video.VideoEditedInfo
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createVideoMessage
import java.io.File

class ConvertVideoJob(
    private val conversationId: String,
    private val senderId: String,
    private val uri: Uri,
    isPlain: Boolean,
    private val messageId: String,
    createdAt: String? = null
) : MixinJob(Params(PRIORITY_BACKGROUND).addTags(TAG).groupBy(GROUP_ID), messageId) {

    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "ConvertVideoJob"
        const val GROUP_ID = "convert_video_group"
    }

    private val video: VideoEditedInfo = MixinApplication.appContext.getVideoModel(uri)!!
    private val videoFile: File = MixinApplication.get().getVideoPath().createVideoTemp("mp4")
    private val category = if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
    private val createdAt: String = createdAt ?: nowInUtc()

    override fun onAdded() {
        val mimeType = getMimeType(uri)
        if (mimeType != "video/mp4") {
            video.needChange = true
        }
        if (!video.fileName.endsWith(".mp4")) {
            video.fileName = "${video.fileName.getFileNameNoEx()}.mp4"
        }

        val message = createVideoMessage(messageId, conversationId, senderId, category, null,
            video.fileName, uri.toString(), video.duration, video.resultWidth,
            video.resultHeight, video.thumbnail, "video/mp4",
            0L, createdAt, null, null, MediaStatus.PENDING, MessageStatus.SENDING)
        // insert message with mediaSize 0L
        // for show video place holder in chat list before convert video
        messageDao.insert(message)
    }

    override fun onRun() {
        jobManager.saveJob(this)
        val result = MediaController.getInstance().convertVideo(video.originalPath, video.bitrate, video.resultWidth, video.resultHeight,
            video.originalWidth, video.originalHeight, videoFile, video.needChange)
        if (isCancel) {
            removeJob()
            return
        }
        if (result) {
            messageDao.updateMediaMessageUrl(videoFile.toUri().toString(), messageId)
        }
        val message = createVideoMessage(messageId, conversationId, senderId, category, null,
            video.fileName, videoFile.toUri().toString(), video.duration, video.resultWidth,
            video.resultHeight, video.thumbnail, "video/mp4",
            videoFile.length(), createdAt, null, null,
            if (result) MediaStatus.PENDING else MediaStatus.CANCELED,
            if (result) MessageStatus.SENDING else MessageStatus.FAILED)

        removeJob()
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }

    override fun cancel() {
        isCancel = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, messageId)
    }
}