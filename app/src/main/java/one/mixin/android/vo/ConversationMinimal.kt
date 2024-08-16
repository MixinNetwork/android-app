package one.mixin.android.vo

import android.os.Parcelable
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConversationMinimal(
    val conversationId: String,
    val avatarUrl: String?,
    val groupIconUrl: String?,
    val category: String?,
    val groupName: String?,
    val name: String,
    val ownerId: String,
    val ownerIdentityNumber: String,
    val ownerVerified: Boolean?,
    val appId: String?,
    val content: String?,
    val contentType: String?,
    val messageStatus: String?,
    val membership: Membership?
) : IConversationCategory, ICategory, Parcelable {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<ConversationMinimal>() {
                override fun areItemsTheSame(
                    oldItem: ConversationMinimal,
                    newItem: ConversationMinimal,
                ) =
                    oldItem.conversationId == newItem.conversationId

                override fun areContentsTheSame(
                    oldItem: ConversationMinimal,
                    newItem: ConversationMinimal,
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

    fun isBot(): Boolean {
        return category == ConversationCategory.CONTACT.name && appId != null
    }

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }
}

fun ConversationMinimal.showVerifiedOrBot(
    verifiedView: View,
    botView: View,
) {
    when {
        ownerVerified == true -> {
            verifiedView.isVisible = true
            botView.isVisible = false
        }
        isBot() -> {
            verifiedView.isVisible = false
            botView.isVisible = true
        }
        else -> {
            verifiedView.isVisible = false
            botView.isVisible = false
        }
    }
}
