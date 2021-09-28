package one.mixin.android.job

import android.net.Uri
import android.provider.MediaStore
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
import one.mixin.android.job.MixinJobManager.Companion.attachmentProcess
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.copyInputStreamToUri
import one.mixin.android.util.getAttachmentAudioUri
import one.mixin.android.util.getAttachmentFilesUri
import one.mixin.android.util.getAttachmentImagesUri
import one.mixin.android.util.getAttachmentVideoUri
import one.mixin.android.util.okhttp.ProgressListener
import one.mixin.android.util.okhttp.ProgressResponseBody
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import org.whispersystems.libsignal.logging.Log
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class AttachmentDownloadJob(
    private val message: Message,
    private val attachmentId: String? = null
) : MixinJob(
    Params(PRIORITY_RECEIVE_MESSAGE)
        .groupBy("attachment_download").requireNetwork().persist(),
    message.id
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
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
        attachmentProcess.remove(message.id)
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
        attachmentCall = conversationApi.getAttachment(attachmentId ?: message.content!!)
        val body = attachmentCall!!.execute().body()
        if (body != null && (body.isSuccess || !isCancelled) && body.data != null) {
            val attachmentResponse = body.data!!
            attachmentResponse.view_url?.let {
                val result = decryptAttachment(it)
                if (result) {
                    val attachmentExtra = GsonHelper.customGson.toJson(AttachmentExtra(attachmentResponse.attachment_id, message.id, attachmentResponse.created_at))
                    messageDao.updateMessageContent(attachmentExtra, message.id)
                }
            }
            removeJob()
        } else {
            removeJob()
            Log.e(TAG, "get attachment url failed")
            messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            attachmentProcess.remove(message.id)
        }
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

    private fun decryptAttachment(url: String): Boolean {
        val destination = createTempFile()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain: Interceptor.Chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder().body(
                    ProgressResponseBody(
                        originalResponse.body,
                        ProgressListener { bytesRead, contentLength, done ->
                            if (!done) {
                                val progress = try {
                                    bytesRead.toFloat() / contentLength.toFloat()
                                } catch (e: Exception) {
                                    0f
                                }
                                attachmentProcess[message.id] = (progress * 100).toInt()
                                RxBus.publish(loadingEvent(message.id, progress))
                            }
                        }
                    )
                ).build()
            }
            .build()

        val request = Request.Builder()
            .addHeader("Content-Type", "application/octet-stream")
            .url(url)
            .build()
        call = client.newCall(request)
        val response = try {
            call!!.execute()
        } catch (e: Exception) {
            messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            attachmentProcess.remove(message.id)
            return false
        }
        if (response.code == 404) {
            messageDao.updateMediaStatus(MediaStatus.EXPIRED.name, message.id)
            attachmentProcess.remove(message.id)
            return true
        } else if (response.isSuccessful && !isCancelled && response.body != null) {
            val sink = destination.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()

            val attachmentCipherInputStream = if (message.category.startsWith("SIGNAL_")) {
                AttachmentCipherInputStream.createForAttachment(destination, 0, message.mediaKey, message.mediaDigest)
            } else {
                FileInputStream(destination)
            }
            val mediaUri: Uri? = when {
                message.category.endsWith("_IMAGE") -> {
                    getAttachmentImagesUri(message.mediaMimeType, message.conversationId, message.id, message.name)
                }
                message.category.endsWith("_DATA") -> {
                    getAttachmentFilesUri(message.conversationId, message.id, message.name)
                }
                message.category.endsWith("_VIDEO") -> {
                    getAttachmentVideoUri(message.conversationId, message.id, message.name)
                }

                message.category.endsWith("_AUDIO") -> {
                    getAttachmentAudioUri(message.conversationId, message.id, message.name)
                }
                else -> null
            }
            if (mediaUri == null) {
                messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
                attachmentProcess.remove(message.id)
                return false
            }

            copyInputStreamToUri(mediaUri, attachmentCipherInputStream)

            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            val cursor = MixinApplication.appContext.contentResolver.query(mediaUri, projection, null, null, null)
            if (cursor != null) {
                val mediaSizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                cursor.moveToNext()
                val imageSize = cursor.getLong(mediaSizeIndex)
                messageDao.updateMediaSize(imageSize, message.id)
                cursor.close()
            }

            messageDao.updateMediaMessageUrl(mediaUri.toString(), message.id)
            messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            attachmentProcess.remove(message.id)

            // if (message.category.endsWith("_IMAGE")) {
            //     val imageUri = getAttachmentImagesUri(message.mediaMimeType, message.conversationId, message.id, message.name)
            //     imageFile.copyFromInputStream(attachmentCipherInputStream)
            //     messageDao.updateMediaMessageUrl(Uri.fromFile(imageFile).toString(), message.id)
            //     messageDao.updateMediaSize(imageFile.length(), message.id)
            //     messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            //     attachmentProcess.remove(message.id)
            // } else if (message.category.endsWith("_DATA")) {
            //     val extensionName = message.name?.getExtensionName()
            //     val dataFile = MixinApplication.get().getLegacyDocumentPath()
            //         .createDocumentTemp(message.conversationId, message.id, extensionName)
            //     dataFile.copyFromInputStream(attachmentCipherInputStream)
            //     messageDao.updateMediaMessageUrl(MixinApplication.appContext.getUriForFile(dataFile).toString(), message.id)
            //     messageDao.updateMediaSize(dataFile.length(), message.id)
            //     messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            //     attachmentProcess.remove(message.id)
            // } else if (message.category.endsWith("_VIDEO")) {
            //     val extensionName = message.name?.getExtensionName().let {
            //         it ?: "mp4"
            //     }
            //     val videoFile = MixinApplication.get().getLegacyVideoPath()
            //         .createVideoTemp(message.conversationId, message.id, extensionName)
            //     videoFile.copyFromInputStream(attachmentCipherInputStream)
            //     messageDao.updateMediaMessageUrl(Uri.fromFile(videoFile).toString(), message.id)
            //     messageDao.updateMediaSize(videoFile.length(), message.id)
            //     messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            //     attachmentProcess.remove(message.id)
            // } else if (message.category.endsWith("_AUDIO")) {
            //     val audioFile = MixinApplication.get().getLegacyAudioPath()
            //         .createAudioTemp(message.conversationId, message.id, "ogg")
            //     audioFile.copyFromInputStream(attachmentCipherInputStream)
            //     messageDao.updateMediaMessageUrl(Uri.fromFile(audioFile).toString(), message.id)
            //     messageDao.updateMediaSize(audioFile.length(), message.id)
            //     messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
            //     attachmentProcess.remove(message.id)
            // }
            return true
        } else {
            messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
            attachmentProcess.remove(message.id)
            return false
        }
    }

    private fun createTempFile(): File {
        val file = File.createTempFile("attachment", "tmp", applicationContext.cacheDir)
        file.deleteOnExit()
        return file
    }
}
