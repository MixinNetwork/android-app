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

    private val currentlyLoadingIds = mutableSetOf<String>()
    private val loadedIds = mutableSetOf<String>()
    private var canLoadAbove = true
    private var canLoadBelow = true

    suspend fun initMessages(
        conversationId: String,
        messageId: String? = null,
        forceBottom: Boolean = false,
    ): Triple<Int, List<MessageItem>, String?> =
        withContext(SINGLE_FETCHER_THREAD) {
            resetLoadState()
            val anchor =
                when {
                    messageId != null -> findAnchorByMessageId(messageId)
                    forceBottom -> null
                    else -> findFirstUnreadAnchor(conversationId)
                }
            if (anchor == null) {
                loadBottomMessages(conversationId)
            } else {
                loadAroundAnchor(conversationId, anchor)
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
            val ids = messageIds.joinToString(", ", "(", ")", transform = { "'$it'" })
            return@withContext MessageFetcherGenerated.findMessagesByIds(db, ids)
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
                MessageFetcherGenerated.loadNextPage(db, conversationId, anchor.createdAt, anchor.rowId, PAGE_SIZE).also {
                    if (it.size < PAGE_SIZE) {
                        canLoadBelow = false
                    }
                }
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
                MessageFetcherGenerated.loadPreviousPage(db, conversationId, anchor.createdAt, anchor.rowId, PAGE_SIZE).reversed().also {
                    if (it.size < PAGE_SIZE) {
                        canLoadAbove = false
                    }
                }
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
        val result = MessageFetcherGenerated.loadBottomMessages(db, conversationId, INIT_SIZE).reversed()
        canLoadBelow = false
        canLoadAbove = result.size >= INIT_SIZE
        return Triple(result.size - 1, result, null)
    }

    private fun loadAroundAnchor(
        conversationId: String,
        anchor: ChatMessageAnchor,
    ): Triple<Int, List<MessageItem>, String?> {
        val next = MessageFetcherGenerated.loadAroundAnchorNext(db, conversationId, anchor.createdAt, anchor.rowId, INIT_SIZE / 2)
        canLoadBelow = next.size >= INIT_SIZE / 2
        val thresholdSize = INIT_SIZE - next.size
        val previous = MessageFetcherGenerated.loadAroundAnchorPrevious(db, conversationId, anchor.createdAt, anchor.rowId, thresholdSize).reversed()
        canLoadAbove = previous.size >= thresholdSize
        val data = previous + next
        return Triple(data.indexOfFirst { it.messageId == anchor.messageId }, data, anchor.messageId)
    }

    private fun findFirstUnreadAnchor(conversationId: String): ChatMessageAnchor? =
        MessageFetcherGenerated.findFirstUnreadAnchor(db, conversationId)

    private fun findAnchorByMessageId(messageId: String): ChatMessageAnchor? =
        MessageFetcherGenerated.findAnchorByMessageId(db, messageId)

    private fun findAnchorByDate(
        conversationId: String,
        createdAt: String,
    ): ChatMessageAnchor? =
        MessageFetcherGenerated.findAnchorByDateAfter(db, conversationId, createdAt)
            ?: MessageFetcherGenerated.findAnchorByDateBefore(db, conversationId, createdAt)

    private fun findAnchorByPosition(
        conversationId: String,
        index: Int,
    ): ChatMessageAnchor? {
        val count = countMessages(conversationId)
        if (count <= 0) return null
        val offset = index.coerceIn(0, count - 1)
        return MessageFetcherGenerated.findAnchorByPosition(db, conversationId, offset)
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
        return findAnchorByPosition(conversationId, index)
    }

    private fun countMessages(conversationId: String): Int =
        MessageFetcherGenerated.countMessages(db, conversationId)
}
