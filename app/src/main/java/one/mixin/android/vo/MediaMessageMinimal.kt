package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class MediaMessageMinimal(
    override val type: String,
    val messageId: String,
    val mediaUrl: String?
) : ICategory
