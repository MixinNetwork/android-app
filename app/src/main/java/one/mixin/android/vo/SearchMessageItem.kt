package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class SearchMessageItem(
    val messageId: String,
    val conversationCategory: String?,
    val conversationId: String,
    val userFullName: String?,
    val botFullName: String?,
    val conversationName: String?,
    val messageCount: Int,
    val type: String,
    val userId: String,
    val botUserId: String?,
    val userAvatarUrl: String?,
    val botAvatarUrl: String?,
    val conversationAvatarUrl: String?

)
