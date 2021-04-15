package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.within24Hours
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.websocket.toAttachmentMessagePayload

class ConvertDataJob(
    private val message: Message
) : MixinJob(Params(PRIORITY_BACKGROUND).groupBy(GROUP_ID), message.id) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP_ID = "convert_data_group"
    }

    override fun onAdded() {
        messageDao.insert(message)
    }

    override fun cancel() {
        isCancelled = true
        messageDao.updateMediaStatus(MediaStatus.CANCELED.name, message.id)
        removeJob()
    }

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)

        val content = message.content
        if (content != null) {
            val attachment = content.toAttachmentMessagePayload()
            if (attachment?.createdAt != null) {
                val within24Hours = attachment.createdAt?.within24Hours() == true
                if (within24Hours) {
                    messageDao.updateMediaSize(attachment.size, message.id)
                    messageDao.updateMessageContent(content, message.id)
                    messageDao.updateMediaStatus(MediaStatus.DONE.name, message.id)
                    val newMessage = createAttachmentMessage(
                        message.id, message.conversationId, message.userId, message.category, content, message.name,
                        message.mediaUrl, attachment.mimeType, attachment.size, message.createdAt, message.mediaKey,
                        message.mediaDigest, MediaStatus.DONE, MessageStatus.SENDING.name, message.quoteMessageId, message.quoteContent
                    )
                    jobManager.addJobInBackground(SendMessageJob(newMessage, null, true))
                    removeJob()
                    return
                }
            }
        }

        message.mediaUrl?.getFilePath()?.let { _ ->
            val inputStream = MixinApplication.appContext.contentResolver.openInputStream(Uri.parse(message.mediaUrl)) ?: return@let
            val extensionName = message.name?.getExtensionName()
            val file = MixinApplication.appContext.getDocumentPath().createDocumentTemp(message.conversationId, message.id, extensionName)
            file.copyFromInputStream(inputStream)
            messageDao.updateMediaMessageUrl(Uri.fromFile(file).toString(), message.id)

            jobManager.addJobInBackground(
                SendAttachmentMessageJob(
                    createAttachmentMessage(
                        message.id, message.conversationId, message.userId, message.category,
                        null, message.name, Uri.fromFile(file).toString(),
                        message.mediaMimeType!!, message.mediaSize!!, message.createdAt, null,
                        null, MediaStatus.PENDING, MessageStatus.SENDING.name, message.quoteMessageId, message.quoteContent
                    )
                )
            )
        }
    }
}
