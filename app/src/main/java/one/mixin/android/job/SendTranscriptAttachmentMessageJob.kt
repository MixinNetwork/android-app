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
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.Util
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStream
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStreamFactory
import one.mixin.android.crypto.attachment.PushAttachmentData
import one.mixin.android.event.ProgressEvent
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.toast
import one.mixin.android.extension.within24Hours
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.util.reportException
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isSecret
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isValidAttachment
import one.mixin.android.websocket.AttachmentMessagePayload
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.net.SocketTimeoutException

class SendTranscriptAttachmentMessageJob(
    val transcriptMessage: TranscriptMessage,
    val encryptCategory: EncryptCategory,
    val parentId: String? = null
) : MixinJob(
    Params(PRIORITY_SEND_ATTACHMENT_MESSAGE).groupBy("send_transcript_job").requireNetwork().persist(),
    "${transcriptMessage.transcriptId}${transcriptMessage.messageId}"
) {

    companion object {
        private const val serialVersionUID = 1L
    }

    @Transient
    private var disposable: Disposable? = null

    override fun cancel() {
        isCancelled = true
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        removeJob()
        transcriptMessageDao.updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
    }

    override fun onRun() {
        if (transcriptMessage.isPlain() == encryptCategory.isPlain()) {
            if (transcriptMessage.mediaCreatedAt?.within24Hours() == true && transcriptMessage.isValidAttachment()) {
                transcriptMessageDao.updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.DONE.name)
                sendMessage()
                return
            }
            val attachmentExtra = try {
                GsonHelper.customGson.fromJson(transcriptMessage.content, AttachmentExtra::class.java)
            } catch (e: Exception) {
                null
            } ?: try {
                val payload = GsonHelper.customGson.fromJson(String(Base64.decode(transcriptMessage.content)), AttachmentMessagePayload::class.java)
                AttachmentExtra(payload.attachmentId, transcriptMessage.messageId, payload.createdAt)
            } catch (e: Exception) {
                null
            }
            if (attachmentExtra != null && attachmentExtra.createdAt?.within24Hours() == true) {
                val m = messageDao.findMessageById(transcriptMessage.messageId)
                if (m != null && transcriptMessage.type == m.category && m.isValidAttachment()) {
                    transcriptMessageDao.updateTranscript(
                        transcriptMessage.transcriptId,
                        transcriptMessage.messageId,
                        attachmentExtra.attachmentId,
                        m.mediaKey,
                        m.mediaDigest,
                        MediaStatus.DONE.name,
                        attachmentExtra.createdAt!!
                    )
                    sendMessage()
                    return
                }
            }
        }
        transcriptMessageDao.updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.PENDING.name)
        disposable = conversationApi.requestAttachment().map {
            val file = File(requireNotNull(Uri.parse(transcriptMessage.absolutePath()).path))
            if (it.isSuccess && !isCancelled) {
                val result = it.data!!
                processAttachment(transcriptMessage, file, result)
            } else {
                false
            }
        }.subscribe(
            {
                if (it) {
                    sendMessage()
                    removeJob()
                } else {
                    removeJob()
                    transcriptMessageDao.updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
                }
            },
            {
                Timber.e("upload attachment error, ${it.getStackTraceString()}")
                reportException(it)
                removeJob()
                transcriptMessageDao.updateMediaStatus(transcriptMessage.transcriptId, transcriptMessage.messageId, MediaStatus.CANCELED.name)
            }
        )
    }

    private fun processAttachment(transcriptMessage: TranscriptMessage, file: File, attachResponse: AttachmentResponse): Boolean {
        val key = if (transcriptMessage.isPlain()) {
            null
        } else {
            Util.getSecretBytes(64)
        }
        val inputStream = try {
            MixinApplication.appContext.contentResolver.openInputStream(Uri.fromFile(file))
        } catch (e: FileNotFoundException) {
            MixinApplication.appScope.launch(Dispatchers.Main) {
                toast(R.string.File_does_not_exit)
            }
            return false
        }
        val attachmentData =
            PushAttachmentData(
                transcriptMessage.mediaMimeType,
                inputStream,
                file.length(),
                if (encryptCategory.isSecret()) {
                    AttachmentCipherOutputStreamFactory(key, null)
                } else {
                    null
                }
            ) { total, progress ->
                val pg = try {
                    progress.toFloat() / total.toFloat()
                } catch (e: Exception) {
                    0f
                }
                RxBus.publish(ProgressEvent.loadingEvent("${transcriptMessage.transcriptId}${transcriptMessage.messageId}", pg))
            }
        val digest = try {
            if (encryptCategory.isSecret()) {
                uploadAttachment(attachResponse.upload_url!!, attachmentData) // SHA256
            } else {
                uploadPlainAttachment(attachResponse.upload_url!!, file.length(), attachmentData)
                null
            }
        } catch (e: Exception) {
            Timber.e(e)
            if (e is SocketTimeoutException) {
                MixinApplication.appScope.launch(Dispatchers.Main) {
                    toast(R.string.Upload_timeout)
                }
            }
            removeJob()
            reportException(e)
            return false
        }
        if (isCancelled) {
            removeJob()
            return true
        }
        transcriptMessageDao.updateTranscript(
            transcriptMessage.transcriptId,
            transcriptMessage.messageId,
            attachResponse.attachment_id,
            key,
            digest,
            MediaStatus.DONE.name,
            attachResponse.created_at
        )
        return true
    }

    private fun sendMessage() {
        if (transcriptMessageDao.hasUploadedAttachment(parentId ?: transcriptMessage.transcriptId) == 0) {
            messageDao.findMessageById(parentId ?: transcriptMessage.transcriptId)?.let { msg ->
                val transcripts = mutableSetOf<TranscriptMessage>()
                getTranscripts(parentId ?: transcriptMessage.transcriptId, transcripts)
                msg.content = GsonHelper.customGson.toJson(transcripts)
                messageDao.updateMediaStatus(MediaStatus.DONE.name, parentId ?: transcriptMessage.transcriptId)
                InvalidateFlow.emit(msg.conversationId)
                jobManager.addJob(SendMessageJob(msg))
            }
        }
    }

    private fun getTranscripts(transcriptId: String, list: MutableSet<TranscriptMessage>) {
        val transcripts = transcriptMessageDao.getTranscript(transcriptId)
        list.addAll(transcripts)
        transcripts.asSequence().apply {
            forEach { t ->
                if (t.isAttachment()) {
                    t.mediaUrl = null
                    t.mediaStatus = null
                }
            }
        }.filter { t -> t.isTranscript() }.forEach { transcriptMessage ->
            getTranscripts(transcriptMessage.messageId, list)
        }
    }

    private fun uploadPlainAttachment(url: String, size: Long, attachment: PushAttachmentData) {
        Util.uploadAttachment(url, attachment.data, size, attachment.outputStreamFactory, attachment.listener, { isCancelled })
    }

    private fun uploadAttachment(url: String, attachment: PushAttachmentData): ByteArray {
        val dataSize = AttachmentCipherOutputStream.getCiphertextLength(attachment.dataSize)
        return Util.uploadAttachment(url, attachment.data, dataSize, attachment.outputStreamFactory, attachment.listener, { isCancelled })
    }
}
