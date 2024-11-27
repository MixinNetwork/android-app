package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getFilePath
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAttachmentMessage

class ConvertDataJob(
    private val message: Message,
) : MixinJob(Params(PRIORITY_BACKGROUND).groupBy(GROUP_ID), message.messageId) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP_ID = "convert_data_group"
    }

    override fun onAdded() {
        database.insertMessage(message)
        MessageFlow.insert(message.conversationId, message.messageId)
    }

    override fun cancel() {
        isCancelled = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.messageId)
        MessageFlow.update(message.conversationId, message.messageId)
        removeJob()
    }

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)

        message.mediaUrl?.getFilePath()?.let { _ ->
            // Here mediaUrl is the full url.
            val inputStream = MixinApplication.appContext.contentResolver.openInputStream(Uri.parse(message.mediaUrl)) ?: return@let
            val extensionName = message.name?.getExtensionName()
            val file = MixinApplication.appContext.getDocumentPath().createDocumentTemp(message.conversationId, message.messageId, extensionName)
            file.copyFromInputStream(inputStream)
            messageDao.updateMediaMessageUrl(file.name, message.messageId)
            MessageFlow.update(message.conversationId, message.messageId)

            jobManager.addJobInBackground(
                SendAttachmentMessageJob(
                    createAttachmentMessage(
                        message.messageId,
                        message.conversationId,
                        message.userId,
                        message.category,
                        null,
                        message.name,
                        file.name,
                        message.mediaMimeType!!,
                        message.mediaSize!!,
                        message.createdAt,
                        null,
                        null,
                        MediaStatus.PENDING,
                        MessageStatus.SENDING.name,
                        message.quoteMessageId,
                        message.quoteContent,
                    ),
                ),
            )
        }
    }
}
