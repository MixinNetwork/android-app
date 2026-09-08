package one.mixin.android.fts

import android.content.Context
import android.os.CancellationSignal
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.User
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FtsDataSourceTest {
    private lateinit var database: MixinDatabase
    private lateinit var ftsDatabase: FtsDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        ftsDatabase = Room.inMemoryDatabaseBuilder(context, FtsDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        database.conversationDao().insert(
            Conversation(
                conversationId = "conversation", ownerId = null, category = ConversationCategory.CONTACT.name,
                name = null, iconUrl = null, announcement = null, codeUrl = null, payType = null,
                createdAt = "2026-09-08T00:00:00Z", pinTime = null, lastMessageId = null,
                lastReadMessageId = null, unseenMessageCount = 0, status = ConversationStatus.SUCCESS.ordinal,
            ),
        )
        database.userDao().insert(
            User(
                userId = "user", identityNumber = "1", relationship = "", biography = "",
                fullName = "User", avatarUrl = null, phone = null, isVerified = false,
                createdAt = null, muteUntil = null,
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        ftsDatabase.close()
        database.close()
    }

    @Test
    fun differentLoadSizesKeepRefreshPrependAndAppendContiguous() = runBlocking {
        seed(105)
        val source = source()
        val refresh = source.page(LoadParams.Refresh(70, 30, false))
        assertIds(70 until 100, refresh)
        assertEquals(70, refresh.prevKey)
        assertEquals(100, refresh.nextKey)
        val prepend = source.page(LoadParams.Prepend(refresh.prevKey!!, 10, false))
        assertIds(60 until 70, prepend)
        assertEquals(60, prepend.prevKey)
        assertEquals(70, prepend.nextKey)
        val append = source.page(LoadParams.Append(refresh.nextKey!!, 10, false))
        assertIds(100 until 105, append)
        assertNull(append.nextKey)
        assertEquals(100, append.prevKey)
    }

    @Test
    fun prependClipsAtZeroAndRefreshRetainsItsPageBoundary() = runBlocking {
        seed(20)
        val source = source()
        val refresh = source.page(LoadParams.Refresh(7, 9, false))
        val prepend = source.page(LoadParams.Prepend(refresh.prevKey!!, 10, false))
        assertIds(0 until 7, prepend)
        assertNull(prepend.prevKey)
        assertEquals(7, prepend.nextKey)
        assertEquals(
            7,
            source.getRefreshKey(PagingState(listOf(refresh), 4, PagingConfig(3), 0)),
        )
        assertNull(source.getRefreshKey(PagingState(emptyList(), null, PagingConfig(3), 0)))
    }

    @Test
    fun emptyAndExactPageBoundaryTerminateWithoutRepeatingKeys() = runBlocking {
        val source = source()
        val empty = source.page(LoadParams.Refresh(null, 10, false))
        assertEquals(emptyList(), empty.data)
        assertNull(empty.prevKey)
        assertNull(empty.nextKey)
        seed(10)
        val first = source.page(LoadParams.Refresh(null, 10, false))
        assertIds(0 until 10, first)
        val end = source.page(LoadParams.Append(first.nextKey!!, 10, false))
        assertEquals(emptyList(), end.data)
        assertNull(end.nextKey)
    }

    @Test
    fun staleIdsAndMissingUsersDoNotEndSearchEarly() = runBlocking {
        seed(12, absent = setOf(2), missingUsers = setOf(5))
        val source = source()
        val first = source.page(LoadParams.Refresh(null, 10, false))
        assertEquals((0 until 10).filter { it != 2 && it != 5 }.map(::id), first.data.map { it.messageId })
        assertEquals(10, first.nextKey)
        assertIds(10 until 12, source.page(LoadParams.Append(first.nextKey!!, 10, false)))
    }

    @Test
    fun entirelyStalePageStillAdvancesToValidRows() = runBlocking {
        seed(23, absent = (0 until 20).toSet())
        val source = source()
        val first = source.page(LoadParams.Refresh(null, 10, false))
        assertEquals(emptyList(), first.data)
        assertEquals(10, first.nextKey)
        val second = source.page(LoadParams.Append(first.nextKey!!, 10, false))
        assertEquals(emptyList(), second.data)
        assertEquals(20, second.nextKey)
        val last = source.page(LoadParams.Append(second.nextKey!!, 10, false))
        assertIds(20 until 23, last)
        assertNull(last.nextKey)
    }

    @Test
    fun prependAcrossStalePageRetainsBothBoundaries() = runBlocking {
        seed(30, absent = (10 until 20).toSet())
        val source = source()
        val middle = source.page(LoadParams.Prepend(20, 10, false))
        assertEquals(emptyList(), middle.data)
        assertEquals(10, middle.prevKey)
        assertEquals(20, middle.nextKey)
        assertIds(0 until 10, source.page(LoadParams.Prepend(middle.prevKey!!, 10, false)))
    }

    @Test
    fun equalMessageTimestampsPreserveFtsOrdering() = runBlocking {
        seed(8, sameTimestamp = true)
        assertIds(0 until 8, source().page(LoadParams.Refresh(null, 10, false)))
    }

    @Test
    fun coroutineCancellationIsRethrown() = runBlocking<Unit> {
        val source = source()
        val completion = CompletableDeferred<Result<LoadResult<Int, SearchMessageDetailItem>>>()
        val load: suspend () -> LoadResult<Int, SearchMessageDetailItem> = {
            source.load(LoadParams.Refresh(null, 10, false))
        }
        load.startCoroutine(
            object : Continuation<LoadResult<Int, SearchMessageDetailItem>> {
                override val context = Job().apply { cancel() }
                override fun resumeWith(result: Result<LoadResult<Int, SearchMessageDetailItem>>) {
                    completion.complete(result)
                }
            },
        )
        assertIs<CancellationException>(completion.await().exceptionOrNull())
    }

    private fun source() = FtsDataSource(ftsDatabase, database, "needle", "conversation", CancellationSignal())

    private suspend fun FtsDataSource.page(params: LoadParams<Int>): LoadResult.Page<Int, SearchMessageDetailItem> =
        assertIs<LoadResult.Page<Int, SearchMessageDetailItem>>(load(params))

    private fun assertIds(indices: IntRange, page: LoadResult.Page<Int, SearchMessageDetailItem>) {
        assertEquals(indices.map(::id), page.data.map { it.messageId })
    }

    private fun id(index: Int) = "message-$index"

    private fun seed(
        count: Int,
        absent: Set<Int> = emptySet(),
        missingUsers: Set<Int> = emptySet(),
        sameTimestamp: Boolean = false,
    ) {
        val indices = if (sameTimestamp) (0 until count).reversed() else 0 until count
        indices.forEach { index ->
            val docId = ftsDatabase.messageFtsDao().insertReturn(MessageFts("needle"))
            ftsDatabase.messageMetaDao().insert(
                MessagesMeta(docId, id(index), "conversation", MessageCategory.PLAIN_TEXT.name, "user", (count - index).toLong()),
            )
            if (index !in absent) {
                database.messageDao().insert(
                    MessageBuilder(
                        id(index), "conversation", if (index in missingUsers) "missing-user" else "user",
                        MessageCategory.PLAIN_TEXT.name, MessageStatus.SENT.name,
                        if (sameTimestamp) "2026-09-08T00:00:00Z" else "2026-09-08T00:${((count - index) / 60).toString().padStart(2, '0')}:${((count - index) % 60).toString().padStart(2, '0')}Z",
                    ).setContent("needle").build(),
                )
            }
        }
    }
}
