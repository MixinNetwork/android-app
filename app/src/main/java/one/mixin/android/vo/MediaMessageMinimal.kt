package one.mixin.android.vo

import android.arch.persistence.room.Entity

@Entity
data class MediaMessageMinimal(
    val messageId: String,
    val mediaUrl: String?
)