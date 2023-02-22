package one.mixin.android.util

import androidx.annotation.WorkerThread
import androidx.room.ColumnInfo
import one.mixin.android.fts.FtsDbHelper
import one.mixin.android.job.NotificationGenerator.ftsDbHelper
import one.mixin.android.vo.Message

data class QueryMessage(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val content: String?,
    val name: String?,
)

object MessageFts4Helper {

    @WorkerThread
    fun insertOrReplaceMessageFts4(ftsDbHelper: FtsDbHelper, message: Message, extraContent: String? = null) {
        ftsDbHelper.insertOrReplaceMessageFts4(message, extraContent)
    }

    @WorkerThread
    fun insertMessageFts4(content: String, conversationId: String, messageId: String, category: String, userId: String, createdAt: String) {
        ftsDbHelper.insertFts4(content, conversationId, messageId, category, userId, createdAt)
    }
}
