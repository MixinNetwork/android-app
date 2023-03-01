package one.mixin.android.fts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.os.CancellationSignal
import androidx.core.database.getStringOrNull
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
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

@Database(
    entities = [
        MessagesFts::class,
        MessagesMetas::class,
    ],
    version = 1,
)
abstract class FtsDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: FtsDatabase? = null

        private val lock = Any()

        private lateinit var supportSQLiteDatabase: SupportSQLiteDatabase
        fun getDatabase(context: Context): FtsDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder = Room.databaseBuilder(
                        context,
                        FtsDatabase::class.java,
                        "fts.db",
                    ).enableMultiInstanceInvalidation().addCallback(
                        object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                supportSQLiteDatabase = db
                            }
                        },
                    )
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as FtsDatabase
        }
    }

    abstract fun messageMetaDao(): MessageMetaDao

    abstract fun messageFtsDao(): MessageFtsDao

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
        openHelper.writableDatabase.beginTransaction()
        val values = ContentValues()
        values.put("content", content)
        val lastRowId =
            openHelper.writableDatabase.insert("messages_fts", CONFLICT_REPLACE, values).apply {
                Timber.e("insert return $this")
            }
        if (lastRowId <= 0) {
            openHelper.writableDatabase.endTransaction()
            return@withContext
        }
        openHelper.writableDatabase.execSQL("INSERT INTO messages_metas(doc_id, message_id, conversation_id, category, user_id, created_at) VALUES ($lastRowId, '$messageId', '$conversationId', '$category', '$userId', '$createdAt')")
        openHelper.writableDatabase.setTransactionSuccessful()
        openHelper.writableDatabase.endTransaction()
    }

    fun rawSearch(content: String, cancellationSignal: CancellationSignal): List<FtsSearchResult> {
        return try {
            query(
                SimpleSQLiteQuery(
                    """
                SELECT message_id, conversation_id, user_id, count(message_id) FROM messages_metas WHERE doc_id 
                IN (SELECT docid FROM messages_fts WHERE content MATCH '$content')
                GROUP BY conversation_id
                ORDER BY max(created_at) DESC
                LIMIT 999
            """,
                ),
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
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun rawSearch(
        content: String,
        conversationId: String,
        cancellationSignal: CancellationSignal,
    ): Collection<String> {
        try {
            query(
                SimpleSQLiteQuery(
                    "SELECT message_id FROM messages_metas WHERE conversation_id = '$conversationId' AND doc_id IN  (SELECT docid FROM messages_fts WHERE content MATCH '$content' LIMIT 999)",
                ),
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
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun deleteByMessageId(messageId: String) = runBlocking(FTS_THREAD) {
        runInTransaction {
            messageFtsDao().deleteMessageMetasByMessageId(messageId)
            messageMetaDao().deleteMessageMetasByMessageId(messageId)
        }
    }

    fun deleteByMessageIds(messageIds: List<String>) = runBlocking(FTS_THREAD) {
        runInTransaction {
            messageFtsDao().deleteMessageMetasByMessageIds(messageIds)
            messageMetaDao().deleteMessageMetasByMessageIds(messageIds)
        }
    }

    fun deleteByConversationId(conversationId: String) = runBlocking(FTS_THREAD) {
        runInTransaction {
            messageFtsDao().deleteMessageMetasByConversationId(conversationId)
            messageMetaDao().deleteMessageMetasByConversationId(conversationId)
        }
    }
}
