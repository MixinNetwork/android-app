package one.mixin.android.vo

import android.content.Context
import androidx.room.ColumnInfo
import one.mixin.android.extension.ancientMediaPath
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.oldMediaPath
import java.io.File

class AttachmentMigration(
    @ColumnInfo(name = "id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "category")
    var category: String,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "media_mine_type")
    val mediaMimeType: String?
)

fun AttachmentMigration.getFile(context: Context): File? {
    return when {
        mediaUrl == null -> null
        oldMediaPath != null && mediaUrl.startsWith(oldMediaPath!!) -> File(mediaUrl)
        ancientMediaPath != null && mediaUrl.startsWith(ancientMediaPath!!) -> File(mediaUrl)
        category.endsWith("_IMAGE") -> File(
            context.getImagePath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_VIDEO") -> File(
            context.getVideoPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_AUDIO") -> File(
            context.getAudioPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_DATA") -> File(
            context.getDocumentPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_TRANSCRIPT") -> File(context.getTranscriptDirPath(), mediaUrl)
        else -> null
    }
}
