package one.mixin.android.job

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.PROGRESS_STATE_NOT_STARTED
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getFileNameNoEx
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getVideoModel
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.NotificationGenerator.database
import one.mixin.android.util.tickerFlow
import one.mixin.android.util.video.VideoEditedInfo
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.vo.toCategory
import one.mixin.android.vo.toQuoteMessageItem
import one.mixin.android.widget.ConvertEvent
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
class ConvertVideoJob(
    private val conversationId: String,
    private val senderId: String,
    private val uri: Uri,
    encryptCategory: EncryptCategory,
    private val messageId: String,
    createdAt: String? = null,
    private val replyMessage: MessageItem? = null,
) : MixinJob(Params(PRIORITY_BACKGROUND).groupBy(GROUP_ID), messageId) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP_ID = "convert_video_group"
    }

    private val video: VideoEditedInfo? = getVideoModel(uri)
    private val category = encryptCategory.toCategory(
        MessageCategory.PLAIN_VIDEO,
        MessageCategory.SIGNAL_VIDEO,
        MessageCategory.ENCRYPTED_VIDEO,
    )
    private val createdAt: String = createdAt ?: nowInUtc()

    override fun onAdded() {
        val mimeType = getMimeType(uri)
        if (video == null) {
            return
        }
        if (mimeType != "video/mp4") {
            video.needChange = true
        }
        if (!video.fileName.endsWith(".mp4")) {
            video.fileName = "${video.fileName.getFileNameNoEx()}.mp4"
        }
        val message = createVideoMessage(
            messageId, conversationId, senderId, category, null,
            video.fileName, uri.toString(), video.duration, video.resultWidth,
            video.resultHeight, video.thumbnail, "video/mp4",
            0L, createdAt, null, null, MediaStatus.PENDING, MessageStatus.SENDING.name,
            replyMessage?.messageId, replyMessage?.toQuoteMessageItem(),
        )
        // insert message with mediaSize 0L
        // for show video place holder in chat list before convert video
        val mId = messageDao.findMessageIdById(message.messageId)
        if (mId == null) {
            database.insertMessage(message)
            MessageFlow.insert(message.conversationId, message.messageId)
        }
    }

    override fun onRun() = runBlocking {
        if (isCancelled) {
            removeJob()
            return@runBlocking
        }
        if (video == null) {
            return@runBlocking
        }
        jobManager.saveJob(this@ConvertVideoJob)

        val mediaItem = MediaItem.fromUri(uri)
        val transformationRequest = TransformationRequest.Builder()
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .build()
        var error: ExportException?
        val videoFile: File = MixinApplication.get().getVideoPath().createVideoTemp(conversationId, messageId, "mp4")
        val transformer = Transformer.Builder(MixinApplication.get())
            .setTransformationRequest(transformationRequest)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                error = exportResult.exportException
                launch {
                    doAfterConvert(transformer, videoFile, video, error)
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                error = exportException
                launch {
                    doAfterConvert(transformer, videoFile, video, error)
                }
            }
        }
        withContext(Dispatchers.Main) {
            transformer.addListener(listener)
            transformer.start(mediaItem, videoFile.absolutePath)
        }

        val progressHolder = ProgressHolder()
        tickerFlow(100.milliseconds)
            .onEach {
                withContext(Dispatchers.Main) {
                    if (transformer.getProgress(progressHolder) != PROGRESS_STATE_NOT_STARTED) {
                        RxBus.publish(ConvertEvent(messageId, progressHolder.progress.toFloat()))
                    }
                }
            }.launchIn(this)
    }

    private suspend fun doAfterConvert(transformer: Transformer, videoFile: File, video: VideoEditedInfo, error: ExportException?) = withContext(Dispatchers.IO) {
        if (error != null) {
            Timber.e("ConvertVideo error: $error")
        }
        if (isCancelled) {
            withContext(Dispatchers.Main) {
                transformer.cancel()
            }
            removeJob()
            return@withContext
        }
        val message = createVideoMessage(
            messageId, conversationId, senderId, category, null,
            video.fileName, videoFile.name, video.duration, video.resultWidth,
            video.resultHeight, video.thumbnail, "video/mp4",
            videoFile.length(), createdAt, null, null,
            if (error != null) MediaStatus.CANCELED else MediaStatus.PENDING,
            if (error != null) MessageStatus.FAILED.name else MessageStatus.SENDING.name,
        )
        if (error == null) {
            messageDao.updateMediaMessageUrl(videoFile.name, messageId)
            MessageFlow.update(message.conversationId, message.messageId)
            jobManager.addJobInBackground(SendAttachmentMessageJob(message))
        }

        removeJob()
    }

    override fun cancel() {
        isCancelled = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, messageId)
        MessageFlow.update(conversationId, messageId)
        removeJob()
    }
}
