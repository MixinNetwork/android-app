package one.mixin.android.db.fetcher

import android.database.Cursor
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.MessageItem

internal data class MessagePage(
    val messages: List<MessageItem>,
    val hasMore: Boolean,
)

internal data class InitialMessagePage(
    val position: Int,
    val messages: List<MessageItem>,
    val unreadMessageId: String?,
    val canLoadAbove: Boolean,
    val canLoadBelow: Boolean,
)

internal fun convertToMessageIds(cursor: Cursor?): List<String> {
    if (cursor == null) return emptyList()
    return buildList {
        while (cursor.moveToNext()) {
            add(cursor.getString(0))
        }
    }
}

internal fun convertToMessageId(cursor: Cursor?): String? =
    if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null

internal fun convertToNullableMessageCount(cursor: Cursor?): Int? =
    if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else null

internal class MessageDataSource(
    private val db: MixinDatabase,
) {
    fun loadInitial(
        conversationId: String,
        initialUnreadMessageId: String?,
        initialUnreadCount: Int?,
        loadSize: Int,
    ): InitialMessagePage {
        val totalCount = countMessages(conversationId)
        val unreadMessageId = MessageFetcherGenerated.findFirstUnreadMessageId(db, conversationId)
        if (unreadMessageId == null) {
            return loadBottom(conversationId, loadSize)
        }

        val storedInitialPosition = MessageFetcherGenerated.findInitialPosition(db, conversationId)
        val initialPosition =
            when {
                storedInitialPosition > 0 -> storedInitialPosition
                initialUnreadMessageId == unreadMessageId && initialUnreadCount != null -> initialUnreadCount
                else -> MessageFetcherGenerated.countUnreadMessages(db, conversationId)
            }
        if (initialPosition <= 0) {
            return loadBottom(conversationId, loadSize)
        }

        val effectiveCount = maxOf(totalCount, initialPosition)
        val targetOffset = (initialPosition - 1).coerceAtLeast(0)
        val maxStartOffset = (effectiveCount - loadSize).coerceAtLeast(0)
        val startOffset =
            (targetOffset - loadSize / 2)
                .coerceAtLeast(0)
                .coerceAtMost(maxStartOffset)
        val page = loadOffsetWindow(conversationId, startOffset, loadSize)
        val position = page.messages.indexOfFirst { it.messageId == unreadMessageId }
        if (position >= 0) {
            return InitialMessagePage(
                position = position,
                messages = page.messages,
                unreadMessageId = unreadMessageId,
                canLoadAbove = page.canLoadAbove,
                canLoadBelow = page.canLoadBelow,
            )
        }

        val anchor = findAnchorByMessageId(unreadMessageId)
            ?: return loadBottom(conversationId, loadSize)
        return loadAroundAnchor(conversationId, anchor, loadSize)
    }

    fun loadBottom(
        conversationId: String,
        loadSize: Int,
    ): InitialMessagePage {
        val page = loadOffsetWindow(conversationId, 0, loadSize)
        return InitialMessagePage(
            position = page.messages.size - 1,
            messages = page.messages,
            unreadMessageId = null,
            canLoadAbove = page.canLoadAbove,
            canLoadBelow = false,
        )
    }

    fun loadAroundAnchor(
        conversationId: String,
        anchor: ChatMessageAnchor,
        loadSize: Int,
    ): InitialMessagePage {
        val nextLimit = loadSize / 2
        val nextIdsWithExtra =
            MessageFetcherGenerated.loadAroundAnchorNextMessageIds(
                db,
                conversationId,
                anchor.createdAt,
                anchor.rowId,
                nextLimit + 1,
            )
        val nextIds = nextIdsWithExtra.take(nextLimit)
        val previousLimit = loadSize - nextIds.size
        val previousIdsWithExtra =
            MessageFetcherGenerated.loadAroundAnchorPreviousMessageIds(
                db,
                conversationId,
                anchor.createdAt,
                anchor.rowId,
                previousLimit + 1,
            )
        val previousIds = previousIdsWithExtra.take(previousLimit)
        val messages = loadMessages(previousIds.asReversed() + nextIds)
        return InitialMessagePage(
            position = messages.indexOfFirst { it.messageId == anchor.messageId },
            messages = messages,
            unreadMessageId = anchor.messageId,
            canLoadAbove = previousIdsWithExtra.size > previousLimit,
            canLoadBelow = nextIdsWithExtra.size > nextLimit,
        )
    }

    fun loadNextPage(
        conversationId: String,
        anchor: ChatMessageAnchor,
        loadSize: Int,
    ): MessagePage {
        val ids =
            MessageFetcherGenerated.loadNextPageMessageIds(
                db,
                conversationId,
                anchor.createdAt,
                anchor.rowId,
                loadSize + 1,
            )
        return MessagePage(
            messages = loadMessages(ids.take(loadSize)),
            hasMore = ids.size > loadSize,
        )
    }

    fun loadPreviousPage(
        conversationId: String,
        anchor: ChatMessageAnchor,
        loadSize: Int,
    ): MessagePage {
        val ids =
            MessageFetcherGenerated.loadPreviousPageMessageIds(
                db,
                conversationId,
                anchor.createdAt,
                anchor.rowId,
                loadSize + 1,
            )
        return MessagePage(
            messages = loadMessages(ids.take(loadSize).asReversed()),
            hasMore = ids.size > loadSize,
        )
    }

    fun loadMessages(messageIds: List<String>): List<MessageItem> {
        if (messageIds.isEmpty()) return emptyList()
        val messages = MessageFetcherGenerated.findMessagesByIds(db, messageIds.toSqlIds())
        return orderMessages(messageIds, messages)
    }

    fun loadChatMessagesByOffset(
        conversationId: String,
        offset: Int,
        limit: Int,
    ): List<MessageItem> {
        val messageIds =
            MessageFetcherGenerated.loadChatMessageIdsByOffset(
                db,
                conversationId,
                limit,
                offset,
            )
        return loadMessages(messageIds)
    }

    fun findAnchorByMessageId(messageId: String): ChatMessageAnchor? =
        MessageFetcherGenerated.findAnchorByMessageId(db, messageId)

    fun findAnchorByDate(
        conversationId: String,
        createdAt: String,
    ): ChatMessageAnchor? =
        MessageFetcherGenerated.findAnchorByDateAfter(db, conversationId, createdAt)
            ?: MessageFetcherGenerated.findAnchorByDateBefore(db, conversationId, createdAt)

    fun findAnchorByPosition(
        conversationId: String,
        offset: Int,
    ): ChatMessageAnchor? =
        MessageFetcherGenerated.findAnchorByPosition(db, conversationId, offset)

    fun countMessages(conversationId: String): Int {
        MessageFetcherGenerated.findCachedMessageCount(db, conversationId)?.let { return it }
        db.conversationExtDao().refreshCountByConversationId(conversationId)
        return MessageFetcherGenerated.findCachedMessageCount(db, conversationId)
            ?: MessageFetcherGenerated.countMessages(db, conversationId)
    }

    private fun loadOffsetWindow(
        conversationId: String,
        offset: Int,
        loadSize: Int,
    ): OffsetMessagePage {
        val ids =
            MessageFetcherGenerated.loadMessageIdsByOffset(
                db,
                conversationId,
                loadSize + 1,
                offset,
            )
        val selectedIds = ids.take(loadSize)
        return OffsetMessagePage(
            messages = loadMessages(selectedIds.asReversed()),
            canLoadAbove = ids.size > loadSize,
            canLoadBelow = offset > 0,
        )
    }

    private data class OffsetMessagePage(
        val messages: List<MessageItem>,
        val canLoadAbove: Boolean,
        val canLoadBelow: Boolean,
    )

    private fun List<String>.toSqlIds(): String =
        joinToString(", ", "(", ")") {
            "'${it.replace("'", "''")}'"
        }

    private fun orderMessages(
        messageIds: List<String>,
        messages: List<MessageItem>,
    ): List<MessageItem> {
        val messagesById = messages.associateBy(MessageItem::messageId)
        return messageIds.mapNotNull(messagesById::get)
    }
}
