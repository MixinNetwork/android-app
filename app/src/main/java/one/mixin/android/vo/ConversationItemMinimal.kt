package one.mixin.android.vo

import android.arch.persistence.room.Entity

@Entity
data class ConversationItemMinimal(
    val conversationId: String,
    val groupIconUrl: String?,
    val groupName: String?,
    val ownerIdentityNumber: String
)