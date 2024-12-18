package one.mixin.android.job

import com.birbit.android.jobqueue.Params
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

class AsyncProcessTransferAttachmentFileJob(private val folder: File) : BaseJob(
    Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("async_process_attachment").persist(),
) {
    companion object {
        const val GROUP = "AsyncProcessTransferAttachmentFileJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        val context = MixinApplication.appContext
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                val messageId = f.name
                if (messageId.isUUID()) {
                    val transferMessage = transcriptMessageDao().findAttachmentMessage(messageId)
                    transferMessage?.mediaUrl?.let { mediaUrl ->
                        val dir = context.getTranscriptDirPath()
                        if (!dir.exists()) {
                            dir.mkdirs()
                        }
                        f.copy(File(dir, mediaUrl))
                    }
                    val message = messageDao().findAttachmentMessage(messageId)
                    if (message?.mediaUrl != null) {
                        val extensionName = message.mediaUrl.getExtensionName()
                        val outFile =
                            if (message.isImage()) {
                                context.getImagePath().createImageTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName?.run { ".$extensionName" } ?: ".jpg",
                                )
                            } else if (message.isAudio()) {
                                context.getAudioPath().createAudioTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "ogg",
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
                                    extensionName ?: "",
                                )
                            }
                        f.moveTo(outFile)
                    } else {
                        // Unable to Mapping to data, delete file
                        f.delete()
                    }
                }
            }
        }
        folder.deleteRecursively()
    }
}
