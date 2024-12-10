package one.mixin.android.job

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
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.event.ProgressEvent
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getTranscriptFile
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.isNullOrEmpty
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.okhttp.ProgressResponseBody
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class TranscriptAttachmentDownloadJob(
    val conversationId: String,
    private val transcriptMessage: TranscriptMessage,
) : MixinJob(
        Params(PRIORITY_RECEIVE_MESSAGE)
            .groupBy("transcript_download").requireNetwork().persist(),
        "${transcriptMessage.transcriptId}${transcriptMessage.messageId}",
    ) {
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
        transcriptMessageDao().updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
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
        transcriptMessageDao().updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.PENDING.name)
        val attachmentId =
            try {
                GsonHelper.customGson.fromJson(
                    transcriptMessage.content,
                    AttachmentExtra::class.java,
                ).attachmentId
            } catch (e: Exception) {
                requireNotNull(transcriptMessage.content)
            }
        attachmentCall = conversationApi.getAttachment(attachmentId)
        val body = attachmentCall!!.execute().body()
        if (body != null && (body.isSuccess || !isCancelled) && body.data != null) {
            val viewUrl = body.data?.view_url
            if (viewUrl != null) {
                if (decryptAttachment(viewUrl, transcriptMessage)) {
                    processTranscript()
                }
            } else {
                transcriptMessageDao().updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
            }
        } else {
            transcriptMessageDao().updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
            Timber.e(TAG, "get attachment url failed")
        }
        removeJob()
    }

    override fun onCancel(
        cancelReason: Int,
        throwable: Throwable?,
    ) {
        super.onCancel(cancelReason, throwable)
        transcriptMessageDao().updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
        removeJob()
    }

    private fun processTranscript() {
        if (transcriptMessageDao().hasUploadedAttachment(transcriptMessage.transcriptId) == 0) {
            messageDao().findMessageById(transcriptMessage.transcriptId)?.let {
                messageDao().updateMediaStatus(MediaStatus.DONE.name, transcriptMessage.transcriptId)
                MessageFlow.update(it.conversationId, it.messageId)
            }
        }
    }

    private fun decryptAttachment(
        url: String,
        transcriptMessage: TranscriptMessage,
    ): Boolean {
        val destination = createTempFile()
        val client =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor { chain: Interceptor.Chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder().body(
                        ProgressResponseBody(
                            originalResponse.body,
                        ) { bytesRead, contentLength, done ->
                            if (!done) {
                                val progress =
                                    try {
                                        bytesRead.toFloat() / contentLength.toFloat()
                                    } catch (e: Exception) {
                                        0f
                                    }
                                RxBus.publish(ProgressEvent.loadingEvent("${transcriptMessage.transcriptId}${transcriptMessage.messageId}", progress))
                            }
                        },
                    ).build()
                }
                .build()

        val request =
            Request.Builder()
                .addHeader("Content-Type", "application/octet-stream")
                .url(url)
                .build()
        call = client.newCall(request)
        val response = requireNotNull(call).execute()
        if (response.code == 404) {
            destination.delete()
            return true
        } else if (response.isSuccessful && !isCancelled && response.body != null) {
            val sink = destination.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()
            when {
                transcriptMessage.type.endsWith("_IMAGE") -> {
                    val attachmentCipherInputStream =
                        if (!transcriptMessage.mediaKey.isNullOrEmpty() && !transcriptMessage.mediaDigest.isNullOrEmpty()) {
                            AttachmentCipherInputStream.createForAttachment(destination, 0, transcriptMessage.mediaKey, transcriptMessage.mediaDigest)
                        } else {
                            FileInputStream(destination)
                        }
                    val imageFile =
                        when {
                            transcriptMessage.mediaMimeType?.isImageSupport() == false -> {
                                MixinApplication.get()
                                    .getTranscriptFile(
                                        transcriptMessage.messageId,
                                        "",
                                    )
                            }
                            transcriptMessage.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                                MixinApplication.get()
                                    .getTranscriptFile(
                                        transcriptMessage.messageId,
                                        ".png",
                                    )
                            }
                            transcriptMessage.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                                MixinApplication.get()
                                    .getTranscriptFile(
                                        transcriptMessage.messageId,
                                        ".gif",
                                    )
                            }
                            transcriptMessage.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                                MixinApplication.get()
                                    .getTranscriptFile(
                                        transcriptMessage.messageId,
                                        ".webp",
                                    )
                            }
                            else -> {
                                MixinApplication.get()
                                    .getTranscriptFile(
                                        transcriptMessage.messageId,
                                        ".jpg",
                                    )
                            }
                        }
                    imageFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptMessageDao().updateMedia(
                        imageFile.name,
                        imageFile.length(),
                        MediaStatus.DONE.name,
                        transcriptMessage.transcriptId,
                        transcriptMessage.messageId,
                    )
                }
                transcriptMessage.type.endsWith("_DATA") -> {
                    val attachmentCipherInputStream =
                        if (!transcriptMessage.mediaKey.isNullOrEmpty() && !transcriptMessage.mediaDigest.isNullOrEmpty()) {
                            AttachmentCipherInputStream.createForAttachment(destination, 0, transcriptMessage.mediaKey, transcriptMessage.mediaDigest)
                        } else {
                            FileInputStream(destination)
                        }
                    val extensionName = transcriptMessage.mediaName?.getExtensionName()
                    val dataFile =
                        MixinApplication.get()
                            .getTranscriptFile(
                                transcriptMessage.messageId,
                                extensionName.notNullWithElse({ ".$it" }, ""),
                            )
                    dataFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptMessageDao().updateMedia(
                        dataFile.name,
                        dataFile.length(),
                        MediaStatus.DONE.name,
                        transcriptMessage.transcriptId,
                        transcriptMessage.messageId,
                    )
                }
                transcriptMessage.type.endsWith("_VIDEO") -> {
                    val attachmentCipherInputStream =
                        if (!transcriptMessage.mediaKey.isNullOrEmpty() && !transcriptMessage.mediaDigest.isNullOrEmpty()) {
                            AttachmentCipherInputStream.createForAttachment(destination, 0, transcriptMessage.mediaKey, transcriptMessage.mediaDigest)
                        } else {
                            FileInputStream(destination)
                        }
                    val extensionName =
                        transcriptMessage.mediaName?.getExtensionName().let {
                            it ?: "mp4"
                        }
                    val videoFile =
                        MixinApplication.get()
                            .getTranscriptFile(
                                transcriptMessage.messageId,
                                ".$extensionName",
                            )
                    videoFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptMessageDao().updateMedia(
                        videoFile.name,
                        videoFile.length(),
                        MediaStatus.DONE.name,
                        transcriptMessage.transcriptId,
                        transcriptMessage.messageId,
                    )
                }
                transcriptMessage.type.endsWith("_AUDIO") -> {
                    val attachmentCipherInputStream =
                        if (!transcriptMessage.mediaKey.isNullOrEmpty() && !transcriptMessage.mediaDigest.isNullOrEmpty()) {
                            AttachmentCipherInputStream.createForAttachment(destination, 0, transcriptMessage.mediaKey, transcriptMessage.mediaDigest)
                        } else {
                            FileInputStream(destination)
                        }
                    val audioFile =
                        MixinApplication.get()
                            .getTranscriptFile(transcriptMessage.messageId, ".ogg")
                    audioFile.copyFromInputStream(attachmentCipherInputStream)
                    transcriptMessageDao().updateMedia(
                        audioFile.name,
                        audioFile.length(),
                        MediaStatus.DONE.name,
                        transcriptMessage.transcriptId,
                        transcriptMessage.messageId,
                    )
                }
            }
            destination.delete()
            return true
        } else {
            destination.delete()
            return false
        }
    }

    private fun createTempFile(): File {
        val file = File.createTempFile("attachment", "tmp", applicationContext.cacheDir)
        file.deleteOnExit()
        return file
    }
}
