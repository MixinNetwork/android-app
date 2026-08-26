package one.mixin.android.db.fetcher

import android.database.Cursor
import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.util.SINGLE_FETCHER_THREAD
import one.mixin.android.vo.MessageItem
import kotlin.math.roundToInt
import javax.inject.Inject

data class ChatMessageAnchor(
    val rowId: Long,
    val createdAt: String,
    val messageId: String,
)

internal fun convertToChatMessageAnchor(cursor: Cursor?): ChatMessageAnchor? =
    if (cursor != null && cursor.moveToFirst()) {
        ChatMessageAnchor(
            rowId = cursor.getLong(0),
            createdAt = cursor.getString(1),
            messageId = cursor.getString(2),
        )
    } else {
        null
    }

internal fun convertToMessageCount(cursor: Cursor?): Int =
    if (cursor != null && cursor.moveToFirst()) cursor.getInt(0) else 0

class MessageFetcher
    @Inject
    constructor(
        val db: MixinDatabase,
    ) {
        companion object {
            const val SCROLL_THRESHOLD = 15
            const val PAGE_SIZE = 30
            private const val INIT_SIZE = 90 // PAGE_SIZE * 3
        }

    private val messageDataSource = MessageDataSource(db)
    private val currentlyLoadingIds = mutableSetOf<String>()
    private val loadedIds = mutableSetOf<String>()
    private var canLoadAbove = true
    private var canLoadBelow = true

    suspend fun initMessages(
        conversationId: String,
        messageId: String? = null,
        forceBottom: Boolean = false,
        initialUnreadMessageId: String? = null,
        initialUnreadCount: Int? = null,
    ): Triple<Int, List<MessageItem>, String?> =
        withContext(SINGLE_FETCHER_THREAD) {
            resetLoadState()
            when {
                messageId != null -> {
                    val anchor = findAnchorByMessageId(messageId)
                        ?: return@withContext Triple(-1, emptyList(), null)
                    loadAroundAnchor(conversationId, anchor)
                }
                forceBottom -> loadBottomMessages(conversationId)
                else -> {
                    val page =
                        messageDataSource.loadInitial(
                            conversationId = conversationId,
                            initialUnreadMessageId = initialUnreadMessageId,
                            initialUnreadCount = initialUnreadCount,
                            loadSize = INIT_SIZE,
                        )
                    updateLoadBoundaries(page)
                    Triple(page.position, page.messages, page.unreadMessageId)
                }
            }
        }

    suspend fun initMessagesAtDate(
        conversationId: String,
        createdAt: String,
    ): Triple<Int, List<MessageItem>, String?> =
        withContext(SINGLE_FETCHER_THREAD) {
            resetLoadState()
            val anchor = findAnchorByDate(conversationId, createdAt)
                ?: return@withContext Triple(-1, emptyList(), null)
            loadAroundAnchor(conversationId, anchor)
        }

    suspend fun initMessagesAtPosition(
        conversationId: String,
        index: Int,
    ): Triple<Int, List<MessageItem>, String?> =
        withContext(SINGLE_FETCHER_THREAD) {
            resetLoadState()
            val anchor = findAnchorByPosition(conversationId, index)
                ?: return@withContext Triple(-1, emptyList(), null)
            loadAroundAnchor(conversationId, anchor)
        }

    suspend fun initMessagesAtPercent(
        conversationId: String,
        percent: Float,
    ): Triple<Int, List<MessageItem>, String?> =
        withContext(SINGLE_FETCHER_THREAD) {
            resetLoadState()
            val anchor = findAnchorByPercent(conversationId, percent)
                ?: return@withContext Triple(-1, emptyList(), null)
            loadAroundAnchor(conversationId, anchor)
        }

    suspend fun findMessageById(messageIds: List<String>) =
        withContext(SINGLE_FETCHER_THREAD) {
            messageDataSource.loadMessages(messageIds)
        }

    fun isBottom() = !canLoadBelow

    fun isTop() = !canLoadAbove

    suspend fun nextPage(
        conversationId: String,
        messageId: String,
    ) =
        withContext(SINGLE_FETCHER_THREAD) {
            val loadKey = "next:$messageId"
            if (!canLoadBelow || currentlyLoadingIds.contains(loadKey) || loadedIds.contains(loadKey)) {
                return@withContext emptyList()
            }

            currentlyLoadingIds.add(loadKey)
            try {
                val anchor = findAnchorByMessageId(messageId) ?: return@withContext emptyList()
                val page = messageDataSource.loadNextPage(conversationId, anchor, PAGE_SIZE)
                canLoadBelow = page.hasMore
                page.messages
            } finally {
                currentlyLoadingIds.remove(loadKey)
                loadedIds.add(loadKey)
            }
        }

    suspend fun previousPage(
        conversationId: String,
        messageId: String,
    ) =
        withContext(SINGLE_FETCHER_THREAD) {
            val loadKey = "previous:$messageId"
            if (!canLoadAbove || currentlyLoadingIds.contains(loadKey) || loadedIds.contains(loadKey)) {
                return@withContext emptyList()
            }

            currentlyLoadingIds.add(loadKey)
            try {
                val anchor = findAnchorByMessageId(messageId) ?: return@withContext emptyList()
                val page = messageDataSource.loadPreviousPage(conversationId, anchor, PAGE_SIZE)
                canLoadAbove = page.hasMore
                page.messages
            } finally {
                currentlyLoadingIds.remove(loadKey)
                loadedIds.add(loadKey)
            }
        }

    private fun resetLoadState() {
        currentlyLoadingIds.clear()
        loadedIds.clear()
        canLoadAbove = true
        canLoadBelow = true
    }

    private fun loadBottomMessages(conversationId: String): Triple<Int, List<MessageItem>, String?> {
        val page = messageDataSource.loadBottom(conversationId, INIT_SIZE)
        updateLoadBoundaries(page)
        return Triple(page.position, page.messages, page.unreadMessageId)
    }

    private fun loadAroundAnchor(
        conversationId: String,
        anchor: ChatMessageAnchor,
    ): Triple<Int, List<MessageItem>, String?> {
        val page = messageDataSource.loadAroundAnchor(conversationId, anchor, INIT_SIZE)
        updateLoadBoundaries(page)
        return Triple(page.position, page.messages, page.unreadMessageId)
    }

    private fun updateLoadBoundaries(page: InitialMessagePage) {
        canLoadAbove = page.canLoadAbove
        canLoadBelow = page.canLoadBelow
    }

    private fun findAnchorByMessageId(messageId: String): ChatMessageAnchor? =
        messageDataSource.findAnchorByMessageId(messageId)

    private fun findAnchorByDate(
        conversationId: String,
        createdAt: String,
    ): ChatMessageAnchor? =
        messageDataSource.findAnchorByDate(conversationId, createdAt)

    private fun findAnchorByPosition(
        conversationId: String,
        index: Int,
    ): ChatMessageAnchor? {
        val count = countMessages(conversationId)
        if (count <= 0) return null
        val offset = index.coerceIn(0, count - 1)
        return messageDataSource.findAnchorByPosition(conversationId, offset)
    }

    private fun findAnchorByPercent(
        conversationId: String,
        percent: Float,
    ): ChatMessageAnchor? {
        val count = countMessages(conversationId)
        if (count <= 0) return null
        val normalizedPercent =
            when {
                percent.isNaN() -> 0f
                percent > 1f -> (percent / 100f).coerceIn(0f, 1f)
                else -> percent.coerceIn(0f, 1f)
            }
        val index = ((count - 1) * normalizedPercent).roundToInt()
        return messageDataSource.findAnchorByPosition(conversationId, index)
    }

    private fun countMessages(conversationId: String): Int =
        messageDataSource.countMessages(conversationId)
}
