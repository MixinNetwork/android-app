package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class ConversationStorageUsage(
    val conversationId: String,
    val avatarUrl: String?,
    val groupIconUrl: String?,
    val category: String?,
    val groupName: String?,
    val name: String?,
    val ownerId: String,
    val ownerIdentityNumber: String,
    val ownerIsVerified: Boolean,
    val mediaSize: Long
)
