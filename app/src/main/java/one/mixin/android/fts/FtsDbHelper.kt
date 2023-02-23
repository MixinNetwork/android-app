package one.mixin.android.fts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.os.CancellationSignal
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.util.FTS_THREAD
import one.mixin.android.vo.FtsSearchResult
import one.mixin.android.vo.Message
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isFtsMessage
import timber.log.Timber
import kotlin.math.max

@Suppress("NON_TAIL_RECURSIVE_CALL")
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
            Timber.e("insert new fts return rowid: $this")
        }
        if (lastRowId <= 0) {
            writableDatabase.endTransaction()
            return@withContext
        }
        writableDatabase.execSQL("INSERT INTO messages_metas(doc_id, message_id, conversation_id, category, user_id, created_at) VALUES ($lastRowId, '$messageId', '$conversationId', '$category', '$userId', '$createdAt')")
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun rawSearch(content: String, cancellationSignal: CancellationSignal): List<FtsSearchResult> =
        readableDatabase.rawQuery(
            """
                SELECT message_id, conversation_id, user_id, count(message_id) FROM messages_metas WHERE doc_id 
                IN (SELECT docid FROM messages_fts WHERE content MATCH '$content')
                GROUP BY conversation_id
                ORDER BY max(created_at) DESC
                LIMIT 999
            """,
            null,
            cancellationSignal,
        ).use {
            val results = mutableListOf<FtsSearchResult>()
            while (it.moveToNext()) {
                val messageId = it.getStringOrNull(0) ?: continue
                val conversationId = it.getStringOrNull(1) ?: continue
                val userId = it.getStringOrNull(2) ?: continue
                val count = it.getInt(3)
                if (count > 0) {
                    results.add(FtsSearchResult(messageId, conversationId, userId, count))
                }
            }
            return@use results
        }

    fun rawSearch(content: String, conversationId: String, cancellationSignal: CancellationSignal): Collection<String> {
        readableDatabase.rawQuery(
            "SELECT message_id FROM messages_metas WHERE conversation_id = '$conversationId' AND doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content' LIMIT 999)",
            null,
            cancellationSignal,
        ).use { cursor ->
            val ids = mutableSetOf<String>()
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

    tailrec fun sync(db: SupportSQLiteDatabase) {
        db.query("SELECT message_id, rowid, content FROM messages_fts4 LIMIT 10000").use { cursor ->
            while (cursor.moveToNext()) {
                val messageId = cursor.getStringOrNull(0) ?: continue
                val rowid = cursor.getLongOrNull(1) ?: continue
                val content = cursor.getStringOrNull(2) ?: continue
                syncFtsMessage(db, messageId, content)
                db.delete("messages_fts4", "rowid = '$rowid'", null)
                Timber.e("delete fts4 rowid: $rowid")
            }
            if (cursor.count >= 10000) {
                sync(db)
            }
        }
    }

    private fun syncFtsMessage(db: SupportSQLiteDatabase, messageId: String, content: String) {
        db.query("SELECT conversation_id, user_id, category, created_at FROM messages WHERE id = '$messageId'")
            .use { cursor ->
                if (cursor.moveToNext()) {
                    val conversationId = cursor.getStringOrNull(0) ?: return@use
                    val userId = cursor.getStringOrNull(1) ?: return@use
                    val category = cursor.getStringOrNull(2) ?: return@use
                    val createdAt = cursor.getStringOrNull(3)?.createAtToLong() ?: return@use
                    try {
                        writableDatabase.beginTransaction()
                        val values = ContentValues()
                        values.put("content", content)
                        val lastRowId = writableDatabase.insert("messages_fts", null, values).apply {
                            Timber.e("insert new fts return rowid: $this")
                        }
                        if (lastRowId <= 0) {
                            writableDatabase.endTransaction()
                            return@use
                        }
                        writableDatabase.execSQL("INSERT INTO messages_metas(doc_id, message_id, conversation_id, category, user_id, created_at) VALUES ($lastRowId, '$messageId', '$conversationId', '$category', '$userId', $createdAt)")
                        writableDatabase.setTransactionSuccessful()
                        writableDatabase.endTransaction()
                    } catch (e: SQLiteConstraintException) {
                        readableDatabase.rawQuery("SELECT message_id FROM messages_metas WHERE message_id = '$messageId'", null).use {
                            if (it.moveToNext()) {
                                Timber.e("insert fts4 exits: $messageId")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("insert fts4 error: $messageId\t$e")
                    }
                } else {
                    Timber.e("messageId: $messageId not found")
                }
            }
    }
}
