package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.AttachmentResponse
import one.mixin.android.api.worthRetrying
import one.mixin.android.crypto.Util
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStream
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStreamFactory
import one.mixin.android.crypto.attachment.PushAttachmentData
import one.mixin.android.db.insertMessage
import one.mixin.android.event.ProgressEvent.Companion.loadingEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager.Companion.attachmentProcess
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.util.reportException
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isVideo
import one.mixin.android.websocket.AttachmentMessagePayload
import timber.log.Timber
import java.io.FileNotFoundException
import java.net.SocketTimeoutException

class SendAttachmentMessageJob(
    val message: Message,
) : MixinJob(Params(PRIORITY_SEND_ATTACHMENT_MESSAGE).groupBy("send_media_job").requireNetwork().persist(), message.messageId) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun cancel() {
        isCancelled = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        InvalidateFlow.emit(message.conversationId)
        attachmentProcess.remove(message.messageId)
        removeJob()
    }

    override fun onAdded() {
        super.onAdded()
        if (message.isVideo()) {
            val mId = messageDao.findMessageIdById(message.messageId)
            if (mId != null) {
                messageDao.updateMediaSize(message.mediaSize ?: 0, mId)
                InvalidateFlow.emit(message.conversationId)
            } else {
                mixinDatabase.insertMessage(message)
                InvalidateFlow.emit(message.conversationId)
            }
        } else {
            val mId = messageDao.findMessageIdById(message.messageId)
            if (mId == null) {
                messageDao.insert(message)
                conversationDao.updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
                InvalidateFlow.emit(message.conversationId)
            }
        }
        val conversation = conversationDao.findConversationById(message.conversationId)
        conversation?.expireIn?.let { e ->
            if (e > 0) {
                expiredMessageDao.insert(
                    ExpiredMessage(
                        message.messageId,
                        e,
                        null
                    )
                )
            }
        }
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        super.onCancel(cancelReason, throwable)
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        InvalidateFlow.emit(message.conversationId)
        attachmentProcess.remove(message.messageId)
        removeJob()
    }

    override fun onRun() = runBlocking(CoroutineExceptionHandler { _, error ->
        if (error.worthRetrying()) {
            throw error
        } else {
            updateLocalMessage(MediaStatus.CANCELED)
        }
    }) {
        if (isCancelled) {
            removeJob()
            return@runBlocking
        }
        if (message.mediaUrl == null) {
            removeJob()
            return@runBlocking
        }
        jobManager.saveJob(this)
        jobManager.saveJob(this@SendAttachmentMessageJob)
        val response = conversationApi.requestAttachment()
        if (response.isSuccess) {
            val attachmentResponse = response.data
            if (attachmentResponse != null) {
                val result = processAttachment(attachmentResponse)
                if (result) {
                    updateLocalMessage(MediaStatus.DONE)
                    return@runBlocking
                }
            }
        }
        updateLocalMessage(MediaStatus.CANCELED)
    }

    private fun updateLocalMessage(status: MediaStatus) {
        messageDao.updateMediaStatus(status.name, message.messageId)
        InvalidateFlow.emit(message.conversationId)
        attachmentProcess.remove(message.messageId)
        removeJob()
    }

    private fun processAttachment(attachResponse: AttachmentResponse): Boolean {
        val key = if (message.isPlain()) {
            null
        } else {
            Util.getSecretBytes(64)
        }
        val inputStream = try {
            MixinApplication.appContext.contentResolver.openInputStream(Uri.parse(message.absolutePath()))
        } catch (e: FileNotFoundException) {
            applicationScope.launch(Dispatchers.Main) {
                toast(R.string.File_does_not_exist)
            }
            null
        }
        val attachmentData =
            PushAttachmentData(
                message.mediaMimeType,
                inputStream,
                message.mediaSize!!,
                if (message.isPlain()) {
                    null
                } else {
                    AttachmentCipherOutputStreamFactory(key, null)
                }
            ) { total, progress ->
                val pg = try {
                    progress.toFloat() / total.toFloat()
                } catch (e: Exception) {
                    0f
                }
                attachmentProcess[message.messageId] = (pg * 100).toInt()
                RxBus.publish(loadingEvent(message.messageId, pg))
            }
        val digest = try {
            if (message.isPlain()) {
                uploadPlainAttachment(
                    attachResponse.upload_url!!,
                    message.mediaSize,
                    attachmentData
                )
                null
            } else {
                uploadAttachment(attachResponse.upload_url!!, attachmentData) // SHA256
            }
        } catch (e: Exception) {
            Timber.e(e)
            if (e is SocketTimeoutException) {
                applicationScope.launch(Dispatchers.Main) {
                    toast(R.string.Upload_timeout)
                }
            }
            messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
            InvalidateFlow.emit(message.conversationId)
            attachmentProcess.remove(message.messageId)
            removeJob()
            reportException(e)
            return false
        }
        if (isCancelled) {
            removeJob()
            return true
        }
        val attachmentId = attachResponse.attachment_id
        val width = message.mediaWidth
        val height = message.mediaHeight
        val size = message.mediaSize
        val thumbnail = message.thumbImage
        val name = message.name
        val mimeType = message.mediaMimeType!!
        val duration = if (message.mediaDuration == null) null else message.mediaDuration.toLong()
        val waveform = message.mediaWaveform
        val transferMediaData = AttachmentMessagePayload(
            key, digest, attachmentId, mimeType, size, name, width, height,
            thumbnail, duration, waveform, createdAt = attachResponse.created_at
        )
        val plainText = GsonHelper.customGson.toJson(transferMediaData)
        val encoded = plainText.base64Encode()
        message.content = encoded
        messageDao.updateMessageContent(encoded, message.messageId)
        jobManager.addJobInBackground(SendMessageJob(message, null, true))
        return true
    }

    private fun uploadPlainAttachment(url: String, size: Long, attachment: PushAttachmentData) {
        Util.uploadAttachment(
            url,
            attachment.data,
            size,
            attachment.outputStreamFactory,
            attachment.listener
        ) { isCancelled }
    }

    private fun uploadAttachment(url: String, attachment: PushAttachmentData): ByteArray {
        val dataSize = AttachmentCipherOutputStream.getCiphertextLength(attachment.dataSize)
        return Util.uploadAttachment(
            url,
            attachment.data,
            dataSize,
            attachment.outputStreamFactory,
            attachment.listener
        ) { isCancelled }
    }
}
