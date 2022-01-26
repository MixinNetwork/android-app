package one.mixin.android.vo

import android.content.Context
import androidx.core.net.toFile
import androidx.room.ColumnInfo
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isFileUri
import one.mixin.android.extension.toUri
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
) : ICategory {
    override val type: String
        get() = category
}

fun AttachmentMigration.getFile(context: Context): File? {
    return when {
        mediaUrl == null -> null
        mediaUrl.isFileUri() -> mediaUrl.toUri().toFile()
        category.endsWith("_IMAGE") -> File(
            context.getImagePath(true).generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_VIDEO") -> File(
            context.getVideoPath(true).generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_AUDIO") -> File(
            context.getAudioPath(true).generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_DATA") -> File(
            context.getDocumentPath(true).generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_TRANSCRIPT") -> File(context.getTranscriptDirPath(true), mediaUrl)
        else -> null
    }
}
