package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.AttachmentResponse
import one.mixin.android.crypto.Util
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStream
import one.mixin.android.crypto.attachment.AttachmentCipherOutputStreamFactory
import one.mixin.android.crypto.attachment.PushAttachmentData
import one.mixin.android.event.ProgressEvent
import one.mixin.android.extension.toast
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.Transcript
import one.mixin.android.vo.isAttachment
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.getStackTraceString
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.net.SocketTimeoutException

class SendTranscriptAttachmentMessageJob(
    val message: Message
) : MixinJob(Params(PRIORITY_SEND_ATTACHMENT_MESSAGE).groupBy("send_media_job").requireNetwork().persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    @Transient
    private var disposable: Disposable? = null

    override fun cancel() {
        isCancelled = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
        MixinJobManager.attachmentProcess.remove(message.id)
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        removeJob()
    }

    private fun isPlain(): Boolean {
        return message.category.startsWith("PLAIN_")
    }

    override fun onAdded() {
        super.onAdded()
        messageDao.insert(message)
    }

    private lateinit var transcripts: MutableList<Transcript>
    override fun onRun() {
        transcripts = GsonHelper.customGson.fromJson(requireNotNull(message.content), Array<Transcript>::class.java).toMutableList()
        transcripts.forEachWithIndex { index, transcript ->
            if (!transcript.isAttachment()) return@forEachWithIndex
            disposable = conversationApi.requestAttachment().map {
                val file = File(requireNotNull(Uri.parse(transcript.mediaUrl).path))
                if (it.isSuccess && !isCancelled) {
                    val result = it.data!!
                    processAttachment(index, transcript, file, result)
                } else {
                    false
                }
            }.subscribe(
                {
                    if (it) {
                        messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
                        MixinJobManager.attachmentProcess.remove(message.id)
                        removeJob()
                    } else {
                        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
                        MixinJobManager.attachmentProcess.remove(message.id)
                        removeJob()
                    }
                },
                {
                    Timber.e("upload attachment error, ${it.getStackTraceString()}")
                    reportException(it)
                    messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
                    MixinJobManager.attachmentProcess.remove(message.id)
                    removeJob()
                }
            )
        }
        messageDao.findMessageById(message.id)?.let { it ->
            jobManager.addJob(SendMessageJob(it))
        }
    }

    private fun processAttachment(index: Int, transcript: Transcript, file: File, attachResponse: AttachmentResponse): Boolean {
        val key = if (isPlain()) {
            null
        } else {
            Util.getSecretBytes(64)
        }
        val inputStream = try {
            MixinApplication.appContext.contentResolver.openInputStream(Uri.fromFile(file))
        } catch (e: FileNotFoundException) {
            GlobalScope.launch(Dispatchers.Main) {
                MixinApplication.get().toast(R.string.error_file_exists)
            }
            return false
        }
        val attachmentData =
            PushAttachmentData(
                message.mediaMimeType,
                inputStream,
                file.length(),
                if (isPlain()) {
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
                MixinJobManager.attachmentProcess[message.id] = (pg * 100).toInt()
                RxBus.publish(ProgressEvent.loadingEvent(message.id, pg))
            }
        val digest = try {
            if (isPlain()) {
                uploadPlainAttachment(attachResponse.upload_url!!, file.length(), attachmentData)
                null
            } else {
                uploadAttachment(attachResponse.upload_url!!, attachmentData) // SHA256
            }
        } catch (e: Exception) {
            Timber.e(e)
            if (e is SocketTimeoutException) {
                GlobalScope.launch(Dispatchers.Main) {
                    MixinApplication.get().toast(R.string.upload_timeout)
                }
            }
            MixinJobManager.attachmentProcess.remove(message.id)
            removeJob()
            reportException(e)
            return false
        }
        if (isCancelled) {
            removeJob()
            return true
        }
        transcript.content = attachResponse.attachment_id
        transcript.attachmentCreatedAt = attachResponse.created_at
        transcript.mediaKey = key
        transcript.mediaDigest = digest
        transcripts.removeAt(index)
        transcripts.add(index, transcript)
        messageDao.updateMessageContent(GsonHelper.customGson.toJson(transcripts), message.id)
        return true
    }

    private fun uploadPlainAttachment(url: String, size: Long, attachment: PushAttachmentData) {
        Util.uploadAttachment(url, attachment.data, size, attachment.outputStreamFactory, attachment.listener, { isCancelled })
    }

    private fun uploadAttachment(url: String, attachment: PushAttachmentData): ByteArray {
        val dataSize = AttachmentCipherOutputStream.getCiphertextLength(attachment.dataSize)
        return Util.uploadAttachment(url, attachment.data, dataSize, attachment.outputStreamFactory, attachment.listener, { isCancelled })
    }
}
