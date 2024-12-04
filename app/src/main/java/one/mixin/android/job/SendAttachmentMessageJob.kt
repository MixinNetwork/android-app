package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.AttachmentResponse
import one.mixin.android.crypto.Util
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStream
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStreamFactory
import one.mixin.android.crypto.attachment.PushAttachmentData
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.event.ProgressEvent.Companion.loadingEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager.Companion.attachmentProcess
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.AttachmentExtra
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

    @Transient
    private var disposable: Disposable? = null

    override fun cancel() {
        isCancelled = true
        messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        MessageFlow.update(message.conversationId, message.messageId)
        attachmentProcess.remove(message.messageId)
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        removeJob()
    }

    override fun onAdded() {
        super.onAdded()
        if (message.isVideo()) {
            val mId = messageDao().findMessageIdById(message.messageId)
            if (mId != null) {
                messageDao().updateMediaSize(message.mediaSize ?: 0, mId)
                MessageFlow.update(message.conversationId, message.messageId)
            } else {
                database().insertMessage(message)
                MessageFlow.insert(message.conversationId, message.messageId)
            }
        } else {
            val mId = messageDao().findMessageIdById(message.messageId)
            if (mId == null) {
                database().insertMessage(message)
                MessageFlow.insert(message.conversationId, message.messageId)
            }
        }
        val conversation = conversationDao().findConversationById(message.conversationId)
        conversation?.expireIn?.let { e ->
            if (e > 0) {
                expiredMessageDao().insert(
                    ExpiredMessage(
                        message.messageId,
                        e,
                        null,
                    ),
                )
            }
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

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        if (message.mediaUrl == null) {
            removeJob()
            return
        }
        jobManager.saveJob(this)

        disposable =
            conversationApi.requestAttachment().map {
                if (it.isSuccess && !isCancelled) {
                    val result = it.data!!
                    processAttachment(result)
                } else {
                    false
                }
            }.subscribe(
                {
                    if (it) {
                        messageDao().updateMediaStatus(MediaStatus.DONE.name, message.messageId)
                        MessageFlow.update(message.conversationId, message.messageId)
                        attachmentProcess.remove(message.messageId)
                        removeJob()
                    } else {
                        messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
                        MessageFlow.update(message.conversationId, message.messageId)
                        attachmentProcess.remove(message.messageId)
                        removeJob()
                    }
                },
                {
                    Timber.e("upload attachment error, ${it.getStackTraceString()}")
                    reportException(it)
                    messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
                    MessageFlow.update(message.conversationId, message.messageId)
                    attachmentProcess.remove(message.messageId)
                    removeJob()
                },
            )
    }

    private fun processAttachment(attachResponse: AttachmentResponse): Boolean {
        val isPlain = message.isPlain()
        val key =
            if (isPlain) {
                null
            } else {
                Util.getSecretBytes(64)
            }
        val inputStream =
            try {
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
                if (isPlain) {
                    null
                } else {
                    AttachmentCipherOutputStreamFactory(key, null)
                },
            ) { total, progress ->
                val pg =
                    try {
                        progress.toFloat() / total.toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                attachmentProcess[message.messageId] = (pg * 100).toInt()
                RxBus.publish(loadingEvent(message.messageId, pg))
            }
        val digest =
            try {
                val transmittedDigest =
                    Util.uploadAttachment(
                        attachResponse.upload_url!!,
                        attachmentData.data,
                        if (isPlain) {
                            message.mediaSize
                        } else {
                            AttachmentCipherOutputStream.getCiphertextLength(attachmentData.dataSize)
                        },
                        attachmentData.outputStreamFactory,
                        attachmentData.listener,
                    ) { isCancelled }
                if (key != null && transmittedDigest == null) {
                    throw IllegalStateException("Digest is null")
                }
                transmittedDigest
            } catch (e: Exception) {
                Timber.e(e)
                if (e is SocketTimeoutException) {
                    applicationScope.launch(Dispatchers.Main) {
                        toast(R.string.Upload_timeout)
                    }
                }
                messageDao().updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
                MessageFlow.update(message.conversationId, message.messageId)
                attachmentProcess.remove(message.messageId)
                removeJob()
                reportException(e)
                return false
            }
        if (isCancelled) {
            removeJob()
            return true
        }

        if (!isPlain && (key == null || key.isEmpty() || digest.isEmpty())) {
            removeJob()
            reportException(IllegalStateException("Encryption error"))
            return false
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
        val transferMediaData =
            AttachmentMessagePayload(
                key,
                digest,
                attachmentId,
                mimeType,
                size,
                name,
                width,
                height,
                thumbnail,
                duration,
                waveform,
                createdAt = attachResponse.created_at,
                caption = message.caption
            )
        val plainText = GsonHelper.customGson.toJson(transferMediaData)
        val encoded = plainText.base64Encode()
        message.content = encoded
        messageDao().updateAttachmentMessage(
            messageId = message.messageId,
            content = GsonHelper.customGson.toJson(AttachmentExtra(attachmentId = attachmentId, messageId = message.messageId, createdAt = attachResponse.created_at)),
            mediaMimeType = mimeType,
            mediaSize = size,
            mediaWidth = width,
            mediaHeight = height,
            mediaDigest = digest,
            mediaKey = key,
            mediaDuration = message.mediaDuration,
            mediaWaveform = waveform,
            thumbImage = thumbnail,
            name = name,
            mediaStatus = message.mediaStatus ?: MediaStatus.PENDING.name,
            status = message.status,
        )
        jobManager.addJobInBackground(SendMessageJob(message, null, true))
        return true
    }
}
