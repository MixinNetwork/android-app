package one.mixin.android.fts

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getStringOrNull
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.vo.Message
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isFtsMessage
import timber.log.Timber
import kotlin.math.max

class FtsDbHelper(val context: Context) : SqlHelper(
    context,
    "fts.db",
    1,
    listOf(
        "CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(content, tokenize=unicode61);",
        "CREATE TABLE IF NOT EXISTS `metas` (`doc_id` INTEGER NOT NULL, `message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, PRIMARY KEY(`message_id`), FOREIGN KEY(`doc_id`) REFERENCES `messages_fts`(DOCID)) WITHOUT ROWID;",
    ),
) {
    fun insertOrReplaceMessageFts4(message: Message, extraContent: String? = null) {
        if (!message.isFtsMessage()) {
            if (message.isContact() && !extraContent.isNullOrBlank()) {
                inertContent(
                    extraContent.joinWhiteSpace(),
                    messageId = message.messageId,
                    conversationId = message.conversationId,
                    userId = message.userId,
                )
            }
            return
        }

        val name = message.name.joinWhiteSpace()
        val content = message.content.joinWhiteSpace()
        inertContent(
            name + content,
            messageId = message.messageId,
            conversationId = message.conversationId,
            userId = message.userId,
        )
    }

    fun insertFts4(content: String, conversationId: String, messageId: String, userId: String) {
        inertContent(
            content,
            messageId = messageId,
            conversationId = conversationId,
            userId = userId,
        )
    }

    private fun inertContent(
        content: String,
        messageId: String,
        conversationId: String,
        userId: String,
    ) {
        writableDatabase.beginTransaction()
        val values = ContentValues()
        values.put("content", content)
        val lastRowId = writableDatabase.insert("messages_fts", null, values).apply {
            Timber.e("insert return $this")
        }
        if (lastRowId <= 0) {
            writableDatabase.endTransaction()
            return
        }
        writableDatabase.execSQL("INSERT INTO metas(doc_id, message_id, conversation_id, user_id) VALUES ($lastRowId, '$messageId', '$conversationId', '$userId')")
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun search(content: String): List<String> {
        readableDatabase.rawQuery(
            "SELECT message_id FROM metas WHERE doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content')",
            null,
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(0)?.let { messageId ->
                    ids.add(messageId)
                }
            }
            return ids
        }
    }

    fun search(content: String, conversationId: String): List<String> {
        readableDatabase.rawQuery(
            "SELECT message_id FROM metas WHERE conversation_id = '$conversationId' AND doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content')",
            null,
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(0)?.let { messageId ->
                    ids.add(messageId)
                }
            }
            return ids
        }
    }

    fun deleteByMessageId(messageId: String): Int {
        var count: Int
        writableDatabase.beginTransaction()
        writableDatabase.delete("messages_fts", "docid = (SELECT doc_id FROM metas WHERE message_id = '$messageId')", null).apply {
            count = this
        }
        writableDatabase.delete("metas","message_id = '$messageId'", null).apply {
            count = max(this, count)
        }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return count
    }

    fun deleteByMessageIds(messageIds: List<String>): Int {
        if (messageIds.isEmpty()) return 0
        var count: Int
        writableDatabase.beginTransaction()
        val ids = messageIds.joinToString(prefix = "'", postfix = "'", separator = "', '")
        writableDatabase.delete("messages_fts","docid IN (SELECT doc_id FROM metas WHERE message_id IN ($ids))", null).apply {
            count = this
        }
        writableDatabase.delete("metas", "message_id IN ($ids)", null).apply {
            count = max(this, count)
        }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return count
    }

    fun deleteByConversationId(conversationId: String): Int {
        var count: Int
        writableDatabase.beginTransaction()
        writableDatabase.delete("messages_fts","docid IN (SELECT doc_id FROM metas WHERE conversation_id = '$conversationId')", null).apply {
            count = this
        }
        writableDatabase.delete("metas","conversation_id IN '$conversationId'", null).apply {
            count = max(this, count)
        }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return count
    }
}
