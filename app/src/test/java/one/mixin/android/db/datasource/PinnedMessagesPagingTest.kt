package one.mixin.android.db.datasource

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.User
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesPagingTest {
    private lateinit var database: MixinDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        database.userDao().insert(User("user", "1", "", "", "User", null, null, false, null, null))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun emptyConversationReturnsEmptyPage() = runBlocking {
        val source = DataProvider.getPinMessages(database, "conversation", 0)
        val page = source.page(PagingSource.LoadParams.Refresh(null, 20, false))
        assertEquals(emptyList(), page.data)
        assertNull(page.prevKey)
        assertNull(page.nextKey)
        assertEquals(0, page.itemsBefore)
        assertEquals(0, page.itemsAfter)
    }

    @Test
    fun pagesKeepConversationFilterAndDoNotRepeatRows() = runBlocking {
        seed("conversation", 7)
        seed("other", 3)
        val source = DataProvider.getPinMessages(database, "conversation", 7)
        val first = source.page(PagingSource.LoadParams.Refresh(null, 3, true))
        assertEquals(ids(0..2), first.data.map { it.messageId })
        assertNull(first.prevKey)
        assertEquals(3, first.nextKey)
        assertEquals(4, first.itemsAfter)
        val second = source.page(PagingSource.LoadParams.Append(first.nextKey!!, 3, true))
        assertEquals(ids(3..5), second.data.map { it.messageId })
        assertEquals(3, second.prevKey)
        val last = source.page(PagingSource.LoadParams.Append(second.nextKey!!, 3, true))
        assertEquals(ids(6..6), last.data.map { it.messageId })
        assertNull(last.nextKey)
        assertEquals(0, last.itemsAfter)
    }

    @Test
    fun prependAfterLargerRefreshHasNoGaps() = runBlocking {
        seed("conversation", 20)
        val source = DataProvider.getPinMessages(database, "conversation", 20)
        val refresh = source.page(PagingSource.LoadParams.Refresh(7, 6, false))
        assertEquals(ids(7..12), refresh.data.map { it.messageId })
        val before = source.page(PagingSource.LoadParams.Prepend(refresh.prevKey!!, 2, false))
        assertEquals(ids(5..6), before.data.map { it.messageId })
        val after = source.page(PagingSource.LoadParams.Append(refresh.nextKey!!, 2, false))
        assertEquals(ids(13..14), after.data.map { it.messageId })
    }

    @Test
    fun prependClipsAtStartAndRefreshClipsAtEnd() = runBlocking {
        seed("conversation", 7)
        val source = DataProvider.getPinMessages(database, "conversation", 7)
        val end = source.page(PagingSource.LoadParams.Refresh(99, 3, false))
        assertEquals(ids(4..6), end.data.map { it.messageId })
        assertNull(end.nextKey)
        val start = source.page(PagingSource.LoadParams.Prepend(2, 3, false))
        assertEquals(ids(0..1), start.data.map { it.messageId })
        assertNull(start.prevKey)
    }

    @Test
    fun exactPageBoundaryDoesNotOfferEmptyNextPage() = runBlocking {
        seed("conversation", 3)
        val source = DataProvider.getPinMessages(database, "conversation", 3)
        val page = source.page(PagingSource.LoadParams.Refresh(null, 3, true))
        assertEquals(ids(0..2), page.data.map { it.messageId })
        assertNull(page.nextKey)
    }

    @Test
    fun pinRemovalInvalidatesOldPageAndReloadsRemainingMessages() = runBlocking {
        seed("conversation", 3)
        val source = DataProvider.getPinMessages(database, "conversation", 3)
        source.page(PagingSource.LoadParams.Refresh(null, 3, false))
        val invalidated = CompletableDeferred<Unit>()
        source.registerInvalidatedCallback { invalidated.complete(Unit) }
        database.pinMessageDao().deleteByMessageId("conversation-1")
        withTimeout(5_000) { invalidated.await() }
        val reloaded = DataProvider.getPinMessages(database, "conversation", 2)
            .page(PagingSource.LoadParams.Refresh(null, 3, false))
        assertEquals(listOf("conversation-0", "conversation-2"), reloaded.data.map { it.messageId })
    }

    private fun seed(conversation: String, count: Int) {
        database.conversationDao().insert(Conversation(
            conversationId = conversation, ownerId = null, category = ConversationCategory.CONTACT.name,
            name = null, iconUrl = null, announcement = null, codeUrl = null, payType = null,
            createdAt = "2026-09-08T00:00:00Z", pinTime = null, lastMessageId = null,
            lastReadMessageId = null, unseenMessageCount = 0, status = ConversationStatus.SUCCESS.ordinal,
        ))
        repeat(count) { index ->
            val id = "$conversation-$index"
            val date = "2026-09-08T00:00:${index.toString().padStart(2, '0')}Z"
            database.messageDao().insert(MessageBuilder(id, conversation, "user", MessageCategory.PLAIN_TEXT.name, MessageStatus.SENT.name, date).setContent(id).build())
            database.pinMessageDao().insert(PinMessage(id, conversation, date))
        }
    }

    private fun ids(range: IntRange) = range.map { "conversation-$it" }

    private suspend fun PagingSource<Int, ChatHistoryMessageItem>.page(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult.Page<Int, ChatHistoryMessageItem> =
        assertIs<PagingSource.LoadResult.Page<Int, ChatHistoryMessageItem>>(load(params))
}
