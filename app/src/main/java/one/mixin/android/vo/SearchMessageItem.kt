package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class SearchMessageItem(
    val conversationId: String,
    val conversationCategory: String?,
    val conversationName: String?,
    var messageCount: Int,
    val userId: String,
    val appId: String?,
    val userFullName: String?,
    val userAvatarUrl: String?,
    val conversationAvatarUrl: String?,
    val isVerified: Boolean?,
    val membership: Membership?
) : Parcelable {
    fun isMembership(): Boolean {
        return conversationCategory == ConversationCategory.CONTACT.name && membership?.isMembership() == true
    }

    fun isBot(): Boolean {
        return conversationCategory == ConversationCategory.CONTACT.name && !appId.isNullOrEmpty()
    }

    fun isVerified(): Boolean {
        return conversationCategory == ConversationCategory.CONTACT.name && isVerified == true
    }
}
