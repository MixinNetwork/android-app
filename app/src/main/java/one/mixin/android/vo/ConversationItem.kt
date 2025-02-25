package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import one.mixin.android.websocket.SystemConversationAction
import org.threeten.bp.Instant

@Entity
data class ConversationItem(
    val conversationId: String,
    val avatarUrl: String?,
    val groupIconUrl: String?,
    val category: String?,
    val groupName: String?,
    val name: String,
    val ownerId: String,
    val status: Int,
    val lastReadMessageId: String?,
    val unseenMessageCount: Int?,
    val content: String?,
    val contentType: String?,
    val createdAt: String?,
    val pinTime: String?,
    val senderId: String?,
    val senderFullName: String?,
    val messageStatus: String?,
    val actionName: String?,
    val participantFullName: String?,
    val participantUserId: String?,
    val ownerMuteUntil: String?,
    val ownerVerified: Boolean?,
    val ownerIdentityNumber: String,
    val muteUntil: String?,
    val appId: String?,
    val mentions: String?,
    val mentionCount: Int?,
    val membership: Membership?
) : ICategory, IConversationCategory {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<ConversationItem>() {
                override fun areItemsTheSame(
                    oldItem: ConversationItem,
                    newItem: ConversationItem,
                ) =
                    oldItem.conversationId == newItem.conversationId

                override fun areContentsTheSame(
                    oldItem: ConversationItem,
                    newItem: ConversationItem,
                ) =
                    oldItem == newItem
            }
    }

    override val type: String?
        get() = contentType

    override val conversationCategory: String?
        get() = category

    fun getConversationName(): String {
        return when {
            isContactConversation() -> name
            isGroupConversation() -> groupName!!
            else -> ""
        }
    }

    fun iconUrl(): String? {
        return when {
            isContactConversation() -> avatarUrl
            isGroupConversation() -> groupIconUrl
            else -> null
        }
    }

    fun isMute(): Boolean {
        if (isContactConversation() && ownerMuteUntil != null) {
            return Instant.now().isBefore(Instant.parse(ownerMuteUntil))
        }
        if (isGroupConversation() && muteUntil != null) {
            return Instant.now().isBefore(Instant.parse(muteUntil))
        }
        return false
    }

    fun isExpire() = actionName == SystemConversationAction.EXPIRE.name

    fun isBot(): Boolean {
        return isContactConversation() && ownerIdentityNumber.let {
            val n = it.toLongOrNull() ?: return@let false
            return (n in 7000000001..7999999999) || n == 7000L
        }
    }

    fun isMembership(): Boolean {
        return isContactConversation() && membership?.isMembership() == true
    }

    fun isProsperity(): Boolean {
        return isContactConversation() && membership?.isProsperity() == true
    }

    fun isVerified(): Boolean {
        return isContactConversation() && ownerVerified == true
    }
}
