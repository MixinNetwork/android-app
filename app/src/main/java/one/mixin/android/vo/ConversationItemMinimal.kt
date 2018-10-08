package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class ConversationItemMinimal(
    val conversationId: String,
    val groupIconUrl: String?,
    val groupName: String?,
    val ownerIdentityNumber: String
)