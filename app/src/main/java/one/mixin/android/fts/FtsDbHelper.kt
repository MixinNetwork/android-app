package one.mixin.android.fts

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.CancellationSignal
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.util.FTS_THREAD
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
        "CREATE TABLE IF NOT EXISTS `messages_metas` (`doc_id` INTEGER NOT NULL, `message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `category` TEXT NOT NULL, `user_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`message_id`));",
    ),
) {
    fun insertOrReplaceMessageFts4(message: Message, extraContent: String? = null) {
        if (!message.isFtsMessage()) {
            if (message.isContact() && !extraContent.isNullOrBlank()) {
                runBlocking {
                    inertContent(
                        extraContent.joinWhiteSpace(),
                        conversationId = message.conversationId,
                        messageId = message.messageId,
                        category = message.category,
                        userId = message.userId,
                        createdAt = message.createdAt.createAtToLong(),
                    )
                }
            }
            return
        }

        val name = message.name.joinWhiteSpace()
        val content = message.content.joinWhiteSpace()
        runBlocking {
            inertContent(
                name + content,
                conversationId = message.conversationId,
                messageId = message.messageId,
                category = message.category,
                userId = message.userId,
                createdAt = message.createdAt.createAtToLong(),
            )
        }
    }

    fun insertFts4(
        content: String,
        conversationId: String,
        messageId: String,
        category: String,
        userId: String,
        createdAt: String,
    ) = runBlocking {
        inertContent(
            content,
            conversationId = conversationId,
            messageId = messageId,
            category = category,
            userId = userId,
            createdAt = createdAt.createAtToLong(),
        )
    }

    private suspend fun inertContent(
        content: String,
        conversationId: String,
        messageId: String,
        category: String,
        userId: String,
        createdAt: Long,
    ) = withContext(FTS_THREAD) {
        writableDatabase.beginTransaction()
        val values = ContentValues()
        values.put("content", content)
        val lastRowId = writableDatabase.insert("messages_fts", null, values).apply {
            Timber.e("insert return $this")
        }
        if (lastRowId <= 0) {
            writableDatabase.endTransaction()
            return@withContext
        }
        writableDatabase.execSQL("INSERT INTO messages_metas(doc_id, message_id, conversation_id, category, user_id, created_at) VALUES ($lastRowId, '$messageId', '$conversationId', '$category', '$userId', '$createdAt')")
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun rawSearch(content: String, cancellationSignal: CancellationSignal): Cursor = readableDatabase.rawQuery("SELECT message_id FROM messages_metas WHERE doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content') LIMIT 999", null, cancellationSignal)

    fun rawSearch(content: String, conversationId: String): List<String> {
        readableDatabase.rawQuery(
            "SELECT message_id FROM messages_metas WHERE conversation_id = '$conversationId' AND doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content' LIMIT 100)",
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

    fun deleteByMessageId(messageId: String): Int = runBlocking(FTS_THREAD) {
        var count: Int
        writableDatabase.beginTransaction()
        writableDatabase.delete(
            "messages_fts",
            "docid = (SELECT doc_id FROM messages_metas WHERE message_id = '$messageId')",
            null,
        ).apply {
            count = this
        }
        writableDatabase.delete("messages_metas", "message_id = '$messageId'", null).apply {
            count = max(this, count)
        }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return@runBlocking count
    }

    fun deleteByMessageIds(messageIds: List<String>): Int = runBlocking(FTS_THREAD) {
        if (messageIds.isEmpty()) return@runBlocking 0
        var count: Int
        writableDatabase.beginTransaction()
        val ids = messageIds.joinToString(prefix = "'", postfix = "'", separator = "', '")
        writableDatabase.delete(
            "messages_fts",
            "docid IN (SELECT doc_id FROM messages_metas WHERE message_id IN ($ids))",
            null,
        ).apply {
            count = this
        }
        writableDatabase.delete("messages_metas", "message_id IN ($ids)", null).apply {
            count = max(this, count)
        }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return@runBlocking count
    }

    fun deleteByConversationId(conversationId: String): Int = runBlocking(FTS_THREAD) {
        var count: Int
        writableDatabase.beginTransaction()
        writableDatabase.delete(
            "messages_fts",
            "docid IN (SELECT doc_id FROM messages_metas WHERE conversation_id = '$conversationId')",
            null,
        ).apply {
            count = this
        }
        writableDatabase.delete("messages_metas", "conversation_id IN '$conversationId'", null)
            .apply {
                count = max(this, count)
            }
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
        return@runBlocking count
    }
}
