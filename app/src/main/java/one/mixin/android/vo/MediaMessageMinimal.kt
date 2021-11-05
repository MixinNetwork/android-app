package one.mixin.android.vo

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import one.mixin.android.MixinApplication

@Entity
data class MediaMessageMinimal(
    @ColumnInfo(name = "category")
    override val type: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "id")
    val messageId: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?
) : ICategory

fun MediaMessageMinimal.absolutePath(context: Context = MixinApplication.appContext): String? {
    return absolutePath(context, conversationId, mediaUrl)
}
