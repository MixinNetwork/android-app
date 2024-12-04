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
import one.mixin.android.event.ProgressEvent.Companion.loadingEvent
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.createEmptyTemp
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.createWebpTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.isNullOrEmpty
import one.mixin.android.job.MixinJobManager.Companion.attachmentProcess
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.okhttp.ProgressResponseBody
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.widget.gallery.MimeType
import org.whispersystems.libsignal.logging.Log
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class AttachmentDownloadJob(
    private val message: Message,
    private val attachmentId: String? = null,
) : MixinJob(
        Params(PRIORITY_RECEIVE_MESSAGE)
            .groupBy("attachment_download").requireNetwork().persist(),
        message.messageId,
    ) {
    private val TAG = AttachmentDownloadJob::class.java.simpleName

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
        messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        MessageFlow.update(message.conversationId, message.messageId)
        attachmentProcess.remove(message.messageId)
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
        var shareable: Boolean? = null
        attachmentCall =
            conversationApi.getAttachment(
                attachmentId ?: try {
                    GsonHelper.customGson.fromJson(
                        message.content,
                        AttachmentExtra::class.java,
                    ).apply {
                        shareable = this.shareable
                    }.attachmentId
                } catch (e: Exception) {
                    message.content!!
                },
            )
        val body = attachmentCall!!.execute().body()
        if (body != null && (body.isSuccess || !isCancelled) && body.data != null) {
            val attachmentResponse = body.data!!
            attachmentResponse.view_url?.let {
                val result = decryptAttachment(it)
                if (result) {
                    val attachmentExtra = GsonHelper.customGson.toJson(AttachmentExtra(attachmentResponse.attachment_id, message.messageId, attachmentResponse.created_at, shareable))
                    messageDao().updateMessageContent(attachmentExtra, message.messageId)
                }
            }
            removeJob()
        } else {
            removeJob()
            Log.e(TAG, "get attachment url failed")
            messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
            MessageFlow.update(message.conversationId, message.messageId)
            attachmentProcess.remove(message.messageId)
        }
    }

    override fun onCancel(
        cancelReason: Int,
        throwable: Throwable?,
    ) {
        super.onCancel(cancelReason, throwable)
        messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        MessageFlow.update(message.conversationId, message.messageId)
        attachmentProcess.remove(message.messageId)
        removeJob()
    }

    override fun onAdded() {
        super.onAdded()
        messageDao().updateMediaStatus(MediaStatus.PENDING.name, message.messageId)
        MessageFlow.update(message.conversationId, message.messageId)
        RxBus.publish(loadingEvent(message.messageId, 0f))
    }

    private fun decryptAttachment(url: String): Boolean {
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
                                attachmentProcess[message.messageId] = (progress * 100).toInt()
                                RxBus.publish(loadingEvent(message.messageId, progress))
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
        val response =
            try {
                call!!.execute()
            } catch (e: Exception) {
                messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
                destination.delete()
                return false
            }
        if (response.code == 404) {
            messageDao().updateMediaStatus(MediaStatus.EXPIRED.name, message.messageId)
            MessageFlow.update(message.conversationId, message.messageId)
            attachmentProcess.remove(message.messageId)
            destination.delete()
            return true
        } else if (response.isSuccessful && !isCancelled && response.body != null) {
            val sink = destination.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()
            if (message.category.endsWith("_IMAGE")) {
                val attachmentCipherInputStream =
                    if (!message.mediaKey.isNullOrEmpty() && !message.mediaDigest.isNullOrEmpty()) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, message.mediaKey, message.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                val imageFile =
                    when {
                        message.mediaMimeType?.isImageSupport() == false -> {
                            MixinApplication.get().getImagePath().createEmptyTemp(message.conversationId, message.messageId)
                        }
                        message.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                            MixinApplication.get().getImagePath().createImageTemp(message.conversationId, message.messageId, ".png")
                        }
                        message.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                            MixinApplication.get().getImagePath().createGifTemp(message.conversationId, message.messageId)
                        }
                        message.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                            MixinApplication.get().getImagePath().createWebpTemp(message.conversationId, message.messageId)
                        }
                        else -> {
                            MixinApplication.get().getImagePath().createImageTemp(message.conversationId, message.messageId, ".jpg")
                        }
                    }
                imageFile.copyFromInputStream(attachmentCipherInputStream)
                messageDao().updateMedia(message.messageId, imageFile.name, imageFile.length(), MediaStatus.DONE.name)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
            } else if (message.category.endsWith("_DATA")) {
                val attachmentCipherInputStream =
                    if (!message.mediaKey.isNullOrEmpty() && !message.mediaDigest.isNullOrEmpty()) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, message.mediaKey, message.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                val extensionName = message.name?.getExtensionName()
                val dataFile =
                    MixinApplication.get().getDocumentPath()
                        .createDocumentTemp(message.conversationId, message.messageId, extensionName)
                dataFile.copyFromInputStream(attachmentCipherInputStream)
                messageDao().updateMedia(message.messageId, dataFile.name, dataFile.length(), MediaStatus.DONE.name)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
            } else if (message.category.endsWith("_VIDEO")) {
                val attachmentCipherInputStream =
                    if (!message.mediaKey.isNullOrEmpty() && !message.mediaDigest.isNullOrEmpty()) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, message.mediaKey, message.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                val extensionName =
                    message.name?.getExtensionName().let {
                        it ?: "mp4"
                    }
                val videoFile =
                    MixinApplication.get().getVideoPath()
                        .createVideoTemp(message.conversationId, message.messageId, extensionName)
                videoFile.copyFromInputStream(attachmentCipherInputStream)
                messageDao().updateMedia(message.messageId, videoFile.name, videoFile.length(), MediaStatus.DONE.name)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
            } else if (message.category.endsWith("_AUDIO")) {
                val attachmentCipherInputStream =
                    if (!message.mediaKey.isNullOrEmpty() && !message.mediaDigest.isNullOrEmpty()) {
                        AttachmentCipherInputStream.createForAttachment(destination, 0, message.mediaKey, message.mediaDigest)
                    } else {
                        FileInputStream(destination)
                    }
                val audioFile =
                    MixinApplication.get().getAudioPath()
                        .createAudioTemp(message.conversationId, message.messageId, "ogg")
                audioFile.copyFromInputStream(attachmentCipherInputStream)
                messageDao().updateMedia(message.messageId, audioFile.name, audioFile.length(), MediaStatus.DONE.name)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
            }
            destination.delete()
            return true
        } else {
            messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
            attachmentProcess.remove(message.messageId)
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
