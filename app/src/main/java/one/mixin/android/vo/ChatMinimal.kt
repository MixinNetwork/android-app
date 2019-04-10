package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class ChatMinimal(
    val category: String,
    val conversationId: String,
    val groupIconUrl: String?,
    val groupName: String?,
    val ownerIdentityNumber: String,
    val userId: String,
    val fullName: String?,
    val avatarUrl: String?
)