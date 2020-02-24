package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class MediaMessageMinimal(
    val messageId: String,
    val mediaUrl: String
)
