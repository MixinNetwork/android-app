package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.AttachmentResponse
import one.mixin.android.crypto.attachment.AttachmentCipherInputStream
import one.mixin.android.event.ProgressEvent.Companion.loadingEvent
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getTranscriptPath
import one.mixin.android.extension.isImageSupport
import one.mixin.android.job.MixinJobManager.Companion.attachmentProcess
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.okhttp.ProgressResponseBody
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.Transcript
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class TranscriptAttachmentDownloadJob(
    val message: Message
) : MixinJob(
    Params(PRIORITY_RECEIVE_MESSAGE)
        .groupBy("attachment_download").requireNetwork().persist(),
    message.id
) {

    private val TAG = TranscriptAttachmentDownloadJob::class.java.simpleName

    companion object {
        private const val serialVersionUID = 1L
    }

    @Transient
    private var call: Call? = null

    @Transient
    private var attachmentCall: retrofit2.Call<MixinResponse<AttachmentResponse>>? = null

    override fun cancel() {
        isCancelled = true
        call?.let {
            if (!it.isCanceled()) {
                it.cancel()
            }
        }
        attachmentCall?.let {
            if (!it.isCanceled) {
                it.cancel()
            }
        }
        removeJob()
    }

    override fun getRetryLimit(): Int {
        return 1
    }

    private lateinit var transcripts: MutableList<Transcript>
    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)
        transcripts = GsonHelper.customGson.fromJson(requireNotNull(message.content), Array<Transcript>::class.java).toMutableList()
        transcripts.forEachIndexed { index, transcript ->
            val attachmentId = requireNotNull(transcript.content)
            attachmentCall = conversationApi.getAttachment(attachmentId)
            val body = attachmentCall!!.execute().body()
            if (body != null && (body.isSuccess || !isCancelled) && body.data != null) {
                body.data?.view_url?.let {
                    decryptAttachment(it, index, transcript)
                }
            } else {
                Timber.e(TAG, "get attachment url failed")
            }
        }
        removeJob()
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        super.onCancel(cancelReason, throwable)
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
        attachmentProcess.remove(message.id)
        removeJob()
    }

    override fun onAdded() {
        super.onAdded()
        messageDao.updateMediaStatus(MediaStatus.PENDING.name, message.id)
        RxBus.publish(loadingEvent(message.id, 0f))
    }

    private fun decryptAttachment(url: String, index: Int, transcript: Transcript): Boolean {
        val destination = createTempFile()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain: Interceptor.Chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder().body(
                    ProgressResponseBody(
                        originalResponse.body
                    ) { bytesRead, contentLength, done ->
                        if (!done) {
                            val progress = try {
                                bytesRead.toFloat() / contentLength.toFloat()
                            } catch (e: Exception) {
                                0f
                            }
                        }
                    }
                ).build()
            }
            .build()

        val request = Request.Builder()
            .addHeader("Content-Type", "application/octet-stream")
            .url(url)
            .build()
        call = client.newCall(request)
        val response = requireNotNull(call).execute()
        if (response.code == 404) {
            return true
        } else if (response.isSuccessful && !isCancelled && response.body != null) {
            val sink = destination.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()
            when {
                transcript.type.endsWith("_IMAGE") -> {
                    val attachmentCipherInputStream = if (transcript.type == MessageCategory.SIGNAL_IMAGE.name) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, transcript.mediaKey, transcript.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                    val imageFile = when {
                        transcript.mediaMimeType?.isImageSupport() == false -> {
                            MixinApplication.get()
                                .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, "")
                        }
                        transcript.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, ".png")
                        }
                        transcript.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, ".gif")
                        }
                        transcript.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, ".webp")
                        }
                        else -> {
                            MixinApplication.get()
                                .getTranscriptPath(message.conversationId, message.id, transcript.messageId, ".jpg")
                        }
                    }
                    imageFile.copyFromInputStream(attachmentCipherInputStream)
                    transcript.mediaUrl = Uri.fromFile(imageFile).toString()
                }
                transcript.type.endsWith("_DATA") -> {
                    val attachmentCipherInputStream = if (transcript.type == MessageCategory.SIGNAL_DATA.name) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, transcript.mediaKey, transcript.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                    val extensionName = transcript.mediaName?.getExtensionName()
                    val dataFile = MixinApplication.get()
                        .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, extensionName ?: "")
                    dataFile.copyFromInputStream(attachmentCipherInputStream)
                    transcript.mediaUrl = Uri.fromFile(dataFile).toString()
                }
                transcript.type.endsWith("_VIDEO") -> {
                    val attachmentCipherInputStream = if (transcript.type == MessageCategory.SIGNAL_VIDEO.name) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, transcript.mediaKey, transcript.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                    val extensionName = transcript.mediaName?.getExtensionName().let {
                        it ?: "mp4"
                    }
                    val videoFile = MixinApplication.get()
                        .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, extensionName)
                    videoFile.copyFromInputStream(attachmentCipherInputStream)
                    transcript.mediaUrl = Uri.fromFile(videoFile).toString()
                }
                transcript.type.endsWith("_AUDIO") -> {
                    val attachmentCipherInputStream = if (transcript.type == MessageCategory.SIGNAL_AUDIO.name) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, transcript.mediaKey, transcript.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                    val audioFile = MixinApplication.get()
                        .getTranscriptPath(message.conversationId, transcript.messageId, transcript.messageId, ".ogg")
                    audioFile.copyFromInputStream(attachmentCipherInputStream)
                    transcript.mediaUrl = Uri.fromFile(audioFile).toString()
                }
            }
            transcripts.removeAt(index)
            transcripts.add(index, transcript)
            messageDao.updateMessageContent(GsonHelper.customGson.toJson(transcripts), message.id)
            return true
        } else {
            return false
        }
    }

    private fun createTempFile(): File {
        val file = File.createTempFile("attachment", "tmp", applicationContext.cacheDir)
        file.deleteOnExit()
        return file
    }
}
