package one.mixin.android.vo

import androidx.room3.Entity

@Entity
data class MediaMessageMinimal(
    override val type: String,
    val messageId: String,
    val mediaUrl: String?,
) : ICategory
