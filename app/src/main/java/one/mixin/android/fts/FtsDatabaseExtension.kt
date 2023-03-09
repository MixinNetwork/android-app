package one.mixin.android.fts

import android.os.CancellationSignal
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.util.FTS_THREAD
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.FtsSearchResult
import one.mixin.android.vo.Message
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isFtsMessage

fun FtsDatabase.deleteByMessageId(messageId: String) = runBlocking(FTS_THREAD) {
    messageFtsDao().deleteMessageMetasByMessageId(messageId)
    messageMetaDao().deleteMessageMetasByMessageId(messageId)
}

fun FtsDatabase.deleteByMessageIds(messageIds: List<String>) = runBlocking(FTS_THREAD) {
    messageFtsDao().deleteMessageMetasByMessageIds(messageIds)
    messageMetaDao().deleteMessageMetasByMessageIds(messageIds)
}

fun FtsDatabase.deleteByConversationId(conversationId: String) = runBlocking(FTS_THREAD) {
    messageFtsDao().deleteMessageMetasByConversationId(conversationId)
    messageMetaDao().deleteMessageMetasByConversationId(conversationId)
}

private suspend fun FtsDatabase.insertMessageFts4(
    content: String,
    conversationId: String,
    messageId: String,
    category: String,
    userId: String,
    createdAt: Long,
) = withContext(FTS_THREAD) {
    if (messageMetaDao().checkMessageMetaExists(messageId)) return@withContext
    val docId = messageFtsDao().insertMessageFts(MessageFts(content))
    messageMetaDao().insertMessageMate(
        MessagesMeta(docId, messageId, conversationId, category, userId, createdAt),
    )
}

fun FtsDatabase.insertOrReplaceMessageFts4(message: Message) {
    if (!message.isFtsMessage()) return
    val content = if (message.isContact() || message.isData()) {
        message.name
    } else if (message.isAppCard()) {
        try {
            val actionCard =
                GsonHelper.customGson.fromJson(message.content, AppCardData::class.java)
            "${actionCard.title} ${actionCard.description}"
        } catch (e: Exception) {
            null
        }
    } else {
        message.content
    }
    val ftsContent = content?.joinWhiteSpace() ?: return
    runBlocking {
        insertMessageFts4(
            content = ftsContent,
            conversationId = message.conversationId,
            messageId = message.messageId,
            category = message.category,
            userId = message.userId,
            createdAt = message.createdAt.createAtToLong(),
        )
    }
}

// Only support transcript
fun FtsDatabase.insertFts4(
    content: String,
    conversationId: String,
    messageId: String,
    category: String,
    userId: String,
    createdAt: String,
) = runBlocking {
    insertMessageFts4(
        content,
        conversationId = conversationId,
        messageId = messageId,
        category = category,
        userId = userId,
        createdAt = createdAt.createAtToLong(),
    )
}

fun FtsDatabase.rawSearch(
    content: String,
    cancellationSignal: CancellationSignal,
): List<FtsSearchResult> {
    return try {
        // A maximum of 999 search results are returned
        query(
            SimpleSQLiteQuery(
                """
                SELECT message_id, conversation_id, user_id, count(message_id) FROM messages_metas WHERE doc_id IN (SELECT docid FROM messages_fts WHERE content MATCH '$content')
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
