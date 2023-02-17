package one.mixin.android.fts5

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.vo.Message
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isFtsMessage
import timber.log.Timber
import java.util.UUID

class FtsDbHelper(val context: Context) : SqlHelper(
        context, "fts.db", 1,
    listOf(
        "CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(content)",
        "CREATE TABLE IF NOT EXISTS `metas` (`doc_id` INTEGER NOT NULL, `message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, PRIMARY KEY(`message_id`), FOREIGN KEY(`doc_id`) REFERENCES `messages_fts`(DOCID)) WITHOUT ROWID;"
    )
){
    private val writableDb: SQLiteDatabase by lazy  {
        writableDatabase
    }
    fun insertOrReplaceMessageFts4(message: Message, extraContent: String? = null) {
        if (!message.isFtsMessage()) {
            if (message.isContact() && !extraContent.isNullOrBlank()) {
                inertContent(extraContent.joinWhiteSpace(), messageId = message.messageId, conversationId = message.conversationId, userId = message.userId)
            }
            return
        }

        val name = message.name.joinWhiteSpace()
        val content = message.content.joinWhiteSpace()
        inertContent(name+content, messageId = message.messageId, conversationId = message.conversationId, userId = message.userId)
    }

    private fun inertContent(content: String, messageId: String, conversationId: String, userId: String) {
        writableDb.beginTransaction()
        val values = ContentValues()
        values.put("content", UUID.randomUUID().toString())
        val lastRowId = writableDb.insert("messages_fts", null, values).apply {
            Timber.e("insert return $this")
        }
        if (lastRowId <= 0) {
            writableDb.endTransaction()
        }
        writableDb.execSQL("INSERT INTO metas(doc_id, message_id, conversation_id, user_id) VALUES ($lastRowId, random(), random(), random())")
        writableDb.setTransactionSuccessful()
        writableDb.endTransaction()
    }
}