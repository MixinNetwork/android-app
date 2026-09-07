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
            const val MAX_LOADED_MESSAGES = 600
            private const val INIT_SIZE = 90 // PAGE_SIZE * 3
        }

    private val messageDataSource = MessageDataSource(db)
    @Volatile
    private var loadGeneration = 0
    private var lastNextKey: String? = null
    private var lastPreviousKey: String? = null
    @Volatile
    private var canLoadAbove = true
    @Volatile
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

    suspend fun nextPage(conversationId: String, messageId: String): List<MessageItem> =
        loadPage(conversationId, messageId, next = true)

    suspend fun previousPage(conversationId: String, messageId: String): List<MessageItem> =
        loadPage(conversationId, messageId, next = false)

    private suspend fun loadPage(conversationId: String, messageId: String, next: Boolean): List<MessageItem> {
        val generation = loadGeneration
        return withContext(SINGLE_FETCHER_THREAD) {
            val canLoad = synchronized(this@MessageFetcher) {
                generation == loadGeneration &&
                    if (next) canLoadBelow && lastNextKey != messageId else canLoadAbove && lastPreviousKey != messageId
            }
            if (!canLoad) return@withContext emptyList()
            val anchor = findAnchorByMessageId(messageId) ?: return@withContext emptyList()
            val page = if (next) {
                messageDataSource.loadNextPage(conversationId, anchor, PAGE_SIZE)
            } else {
                messageDataSource.loadPreviousPage(conversationId, anchor, PAGE_SIZE)
            }
            synchronized(this@MessageFetcher) {
                if (generation != loadGeneration) return@withContext emptyList()
                if (next) {
                    canLoadBelow = page.hasMore
                    lastNextKey = messageId
                } else {
                    canLoadAbove = page.hasMore
                    lastPreviousKey = messageId
                }
            }
            page.messages
        }
    }

    @Synchronized
    fun onWindowTrimmed(fromStart: Boolean) {
        loadGeneration++
        if (fromStart) {
            canLoadAbove = true
            lastPreviousKey = null
        } else {
            canLoadBelow = true
            lastNextKey = null
        }
    }

    @Synchronized
    private fun resetLoadState() {
        loadGeneration++
        lastNextKey = null
        lastPreviousKey = null
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
        return messageDataSource.findAnchorByPosition(conversationId, offset, count)
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
        return messageDataSource.findAnchorByPosition(conversationId, index, count)
    }

    private fun countMessages(conversationId: String): Int =
        messageDataSource.countMessages(conversationId)
}
