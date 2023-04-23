package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copy
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.moveTo
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import java.io.File

class TransferSyncAttachmentJob(private val filePath: String) :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L

        // Group by TransferSyncJob, transfer attachment is final operation
        private const val GROUP_ID = "transfer_sync"
    }

    override fun onRun(): Unit = runBlocking {
        val context = MixinApplication.appContext
        val folder = File(filePath)
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                val messageId = f.name
                if (messageId.isUUID()) {
                    val transferMessage = transcriptMessageDao.findAttachmentMessage(messageId)
                    if (transferMessage?.mediaUrl != null) {
                        f.copy(File(context.getTranscriptDirPath(), transferMessage.mediaUrl))
                    }
                    val message = messageDao.findAttachmentMessage(messageId)
                    if (message?.mediaUrl != null) {
                        val extensionName = message.mediaUrl?.getExtensionName() ?: ""
                        val outFile =
                            if (message.isImage()) {
                                context.getImagePath().createImageTemp(
                                    message.conversationId,
                                    message.messageId,
                                    ".$extensionName",
                                )
                            } else if (message.isAudio()) {
                                context.getAudioPath().createAudioTemp(
                                    message.conversationId,
                                    message.messageId,
                                    "ogg",
                                )
                            } else if (message.isVideo()) {
                                context.getVideoPath().createVideoTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "mp4",
                                )
                            } else {
                                context.getDocumentPath().createDocumentTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName,
                                )
                            }
                        f.moveTo(outFile)
                    }
                }
            }
        }
    }
}
