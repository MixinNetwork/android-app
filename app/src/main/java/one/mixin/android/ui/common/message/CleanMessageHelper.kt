package one.mixin.android.ui.common.message

import one.mixin.android.job.MixinJobManager
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.MessageItem
import javax.inject.Inject

class CleanMessageHelper @Inject internal constructor(private val jobManager: MixinJobManager) {

    suspend fun deleteMessageByConversationId(conversationId: String) {
    }

    suspend fun deleteMessageByMessageId(messageId: String) {
    }

    suspend fun deleteMessageItems(messageItems: List<MessageItem>) {
    }

    suspend fun deleteMessageMinimals(messageItems: List<MediaMessageMinimal>) {
    }
}
