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
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.AttachmentResponse
import one.mixin.android.crypto.attachment.AttachmentCipherInputStream
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getTranscriptFile
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.util.okhttp.ProgressResponseBody
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.Transcript
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class TranscriptAttachmentDownloadJob(
    val conversationId: String,
    val messageId: String,
    val transcript: Transcript
) : MixinJob(
    Params(PRIORITY_RECEIVE_MESSAGE)
        .groupBy("transcript_download").requireNetwork().persist(),
    "$messageId${transcript.messageId}"
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

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)

        val attachmentId = requireNotNull(transcript.content)
        attachmentCall = conversationApi.getAttachment(attachmentId)
        val body = attachmentCall!!.execute().body()
        if (body != null && (body.isSuccess || !isCancelled) && body.data != null) {
            body.data?.view_url.notNullWithElse({
                decryptAttachment(it, transcript)
            }, {
                transcriptDao.updateMediaStatus(transcript.transcriptId, transcript.messageId, MediaStatus.CANCELED.name)
            })
        } else {
            transcriptDao.updateMediaStatus(transcript.transcriptId, transcript.messageId, MediaStatus.CANCELED.name)
            Timber.e(TAG, "get attachment url failed")
        }
        removeJob()
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        super.onCancel(cancelReason, throwable)
        removeJob()
    }

    private fun decryptAttachment(url: String, transcript: Transcript): Boolean {
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
                            // todo
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
                    val attachmentCipherInputStream =
                        if (transcript.type == MessageCategory.SIGNAL_IMAGE.name) {
                            AttachmentCipherInputStream.createForAttachment(
                                destination,
                                0,
                                transcript.mediaKey,
                                transcript.mediaDigest
                            )
                        } else {
                            FileInputStream(destination)
                        }
                    val imageFile = when {
                        transcript.mediaMimeType?.isImageSupport() == false -> {
                            MixinApplication.get()
                                .getTranscriptFile(
                                    conversationId,
                                    messageId,
                                    transcript.messageId,
                                    ""
                                )
                        }
                        transcript.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptFile(
                                    conversationId,
                                    messageId,
                                    transcript.messageId,
                                    ".png"
                                )
                        }
                        transcript.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptFile(
                                    conversationId,
                                    messageId,
                                    transcript.messageId,
                                    ".gif"
                                )
                        }
                        transcript.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                            MixinApplication.get()
                                .getTranscriptFile(
                                    conversationId,
                                    messageId,
                                    transcript.messageId,
                                    ".webp"
                                )
                        }
                        else -> {
                            MixinApplication.get()
                                .getTranscriptFile(
                                    conversationId,
                                    messageId,
                                    transcript.messageId,
                                    ".jpg"
                                )
                        }
                    }
                    imageFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptDao.updateMedia(
                        Uri.fromFile(imageFile).toString(),
                        imageFile.length(),
                        MediaStatus.DONE.name,
                        transcript.transcriptId,
                        transcript.messageId
                    )
                }
                transcript.type.endsWith("_DATA") -> {
                    val attachmentCipherInputStream =
                        if (transcript.type == MessageCategory.SIGNAL_DATA.name) {
                            AttachmentCipherInputStream.createForAttachment(
                                destination,
                                0,
                                transcript.mediaKey,
                                transcript.mediaDigest
                            )
                        } else {
                            FileInputStream(destination)
                        }
                    val extensionName = transcript.mediaName?.getExtensionName()
                    val dataFile = MixinApplication.get()
                        .getTranscriptFile(
                            conversationId,
                            messageId,
                            transcript.messageId,
                            extensionName ?: ""
                        )
                    dataFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptDao.updateMedia(
                        Uri.fromFile(dataFile).toString(),
                        dataFile.length(),
                        MediaStatus.DONE.name,
                        transcript.transcriptId,
                        transcript.messageId
                    )
                }
                transcript.type.endsWith("_VIDEO") -> {
                    val attachmentCipherInputStream =
                        if (transcript.type == MessageCategory.SIGNAL_VIDEO.name) {
                            AttachmentCipherInputStream.createForAttachment(
                                destination,
                                0,
                                transcript.mediaKey,
                                transcript.mediaDigest
                            )
                        } else {
                            FileInputStream(destination)
                        }
                    val extensionName = transcript.mediaName?.getExtensionName().let {
                        it ?: "mp4"
                    }
                    val videoFile = MixinApplication.get()
                        .getTranscriptFile(
                            conversationId,
                            messageId,
                            transcript.messageId,
                            extensionName
                        )
                    videoFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptDao.updateMedia(
                        Uri.fromFile(videoFile).toString(),
                        videoFile.length(),
                        MediaStatus.DONE.name,
                        transcript.transcriptId,
                        transcript.messageId
                    )
                }
                transcript.type.endsWith("_AUDIO") -> {
                    val attachmentCipherInputStream =
                        if (transcript.type == MessageCategory.SIGNAL_AUDIO.name) {
                            AttachmentCipherInputStream.createForAttachment(
                                destination,
                                0,
                                transcript.mediaKey,
                                transcript.mediaDigest
                            )
                        } else {
                            FileInputStream(destination)
                        }
                    val audioFile = MixinApplication.get()
                        .getTranscriptFile(conversationId, messageId, transcript.messageId, ".ogg")
                    audioFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptDao.updateMedia(
                        Uri.fromFile(audioFile).toString(),
                        audioFile.length(),
                        MediaStatus.DONE.name,
                        transcript.transcriptId,
                        transcript.messageId
                    )
                }
            }
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
