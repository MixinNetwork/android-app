package one.mixin.android.vo

import android.content.Context
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import one.mixin.android.MixinApplication
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getLegacyAudioPath
import one.mixin.android.extension.getLegacyDocumentPath
import one.mixin.android.extension.getLegacyImagePath
import one.mixin.android.extension.getLegacyMediaPath
import one.mixin.android.extension.getLegacyTranscriptDirPath
import one.mixin.android.extension.getLegacyVideoPath
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

private val mediaPath by lazy {
    MixinApplication.appContext.getLegacyMediaPath()?.toUri()?.toString()
}

fun AttachmentMigration.getFile(context: Context): File? {
    return when {
        mediaUrl == null -> null
        category.endsWith("_IMAGE") -> File(
            context.getLegacyImagePath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_VIDEO") -> File(
            context.getLegacyVideoPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_AUDIO") -> File(
            context.getLegacyAudioPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_DATA") -> File(
            context.getLegacyDocumentPath().generateConversationPath(conversationId), mediaUrl
        )
        category.endsWith("_TRANSCRIPT") -> File(context.getLegacyTranscriptDirPath(), mediaUrl)
        else -> null
    }
}
