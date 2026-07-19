package one.mixin.android.db

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.User
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private lateinit var database: MixinDatabase
    private lateinit var messageDao: MessageDao

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun mediaQueriesIncludeAppCardsWithCoverUrl() = runBlocking {
        val conversationId = "conversation-id"
        val userId = "user-id"
        database.conversationDao().insert(conversation(conversationId))
        database.userDao().insert(user(userId))
        messageDao.insertList(
            listOf(
                imageMessage("image-before", conversationId, userId, "2026-06-29T00:00:00Z"),
                appCardMessage(
                    id = "cover-url-app-card",
                    conversationId = conversationId,
                    userId = userId,
                    content = """{"app_id":"app","cover_url":"https://example.com/cover-url.jpg","title":"cover url"}""",
                    createdAt = "2026-06-29T00:01:00Z",
                ),
                imageMessage("target-image", conversationId, userId, "2026-06-29T00:02:00Z"),
                appCardMessage(
                    id = "nested-cover-app-card",
                    conversationId = conversationId,
                    userId = userId,
                    content = """{"app_id":"app","cover":{"url":"https://example.com/cover.jpg","height":320,"width":640,"mime_type":"image/jpeg"},"title":"cover"}""",
                    createdAt = "2026-06-29T00:03:00Z",
                ),
                appCardMessage(
                    id = "empty-cover-app-card",
                    conversationId = conversationId,
                    userId = userId,
                    content = """{"app_id":"app","cover":{},"title":"empty"}""",
                    createdAt = "2026-06-29T00:04:00Z",
                ),
            ),
        )

        assertNotNull(messageDao.getMediaMessage(conversationId, "cover-url-app-card"))
        assertNull(messageDao.getMediaMessage(conversationId, "empty-cover-app-card"))
        assertNull(messageDao.getMediaMessage(conversationId, "nested-cover-app-card"))
        assertEquals(
            listOf("image-before", "cover-url-app-card", "target-image"),
            messageDao.getMediaMessagesList(conversationId).map { it.messageId },
        )
        assertEquals(2, messageDao.indexMediaMessages(conversationId, "target-image"))
        assertEquals(3, messageDao.countIndexMediaMessages(conversationId))
    }

    @Test
    fun rawWritesNotifyInvalidationTracker() = runBlocking {
        val conversationId = "invalidation-conversation"
        database.conversationDao().insert(conversation(conversationId))
        val invalidation = CompletableDeferred<Unit>()
        val observer =
            RoomDatabaseCompat.observeInvalidation(this, database, "conversations") {
                invalidation.complete(Unit)
            }
        database.invalidationTracker.createFlow("conversations").first()
        yield()
        assertFalse(invalidation.isCompleted)

        RoomDatabaseCompat.execute(
            database,
            "UPDATE conversations SET name = ? WHERE conversation_id = ?",
            arrayOf("Updated", conversationId),
        )

        withTimeout(5_000) { invalidation.await() }
        observer.cancel()
    }

    @Test
    fun rawWritesInvalidatePagingSource() = runBlocking {
        val conversationId = "paging-invalidation-conversation"
        database.conversationDao().insert(conversation(conversationId))
        val pagingSource =
            object : PagingSource<Int, Int>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> =
                    LoadResult.Page(emptyList(), null, null)

                override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
            }

        val invalidation = RoomDatabaseCompat.observeInvalidation(database, pagingSource, "conversations")
        invalidation.awaitStart()
        assertFalse(pagingSource.invalid)

        RoomDatabaseCompat.execute(
            database,
            "UPDATE conversations SET name = ? WHERE conversation_id = ?",
            arrayOf("Updated", conversationId),
        )

        withTimeout(5_000) {
            while (!pagingSource.invalid) yield()
        }
        assertTrue(pagingSource.invalid)
    }

    @Test
    fun markReadInvalidatesConversationPagingSource() = runBlocking {
        val conversationId = "mark-read-conversation"
        val userId = "mark-read-user"
        database.userDao().insert(user(userId))
        database.conversationDao().insert(conversation(conversationId, userId, 1))
        val pagingSource = DataProvider.observeConversations(database)
        val result =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = true,
                ),
            )
        assertTrue(result is PagingSource.LoadResult.Page)
        assertFalse(pagingSource.invalid)

        database.remoteMessageStatusDao().markRead(conversationId)

        withTimeout(5_000) {
            while (!pagingSource.invalid) yield()
        }
        assertTrue(pagingSource.invalid)

        val refreshedPagingSource = DataProvider.observeConversations(database)
        val refreshed =
            refreshedPagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = true,
                ),
            ) as PagingSource.LoadResult.Page
        assertEquals(0, refreshed.data.single().unseenMessageCount)
    }

    @Test
    fun incomingMessageTransactionInvalidatesConversationPagingSource() = runBlocking {
        val conversationId = "incoming-message-conversation"
        val userId = "incoming-message-user"
        val messageId = "incoming-message"
        database.userDao().insert(user(userId))
        database.conversationDao().insert(conversation(conversationId, userId))
        val pagingSource = DataProvider.observeConversations(database)
        val result =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = true,
                ),
            )
        assertTrue(result is PagingSource.LoadResult.Page)
        assertFalse(pagingSource.invalid)

        database.runInTransaction {
            messageDao.insert(
                MessageBuilder(
                    messageId,
                    conversationId,
                    userId,
                    MessageCategory.PLAIN_TEXT.name,
                    MessageStatus.DELIVERED.name,
                    "2026-06-29T00:01:00Z",
                ).setContent("New message").build(),
            )
            database.remoteMessageStatusDao().insert(
                RemoteMessageStatus(messageId, conversationId, MessageStatus.DELIVERED.name),
            )
            database.conversationDao().updateLastMessageId(
                messageId,
                "2026-06-29T00:01:00Z",
                conversationId,
            )
            database.remoteMessageStatusDao().updateConversationUnseen(conversationId)
        }

        withTimeout(5_000) {
            while (!pagingSource.invalid) yield()
        }
        assertTrue(pagingSource.invalid)
        assertEquals(1, database.conversationDao().indexUnread(conversationId))

        val refreshedPagingSource = DataProvider.observeConversations(database)
        val refreshed =
            refreshedPagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = true,
                ),
            ) as PagingSource.LoadResult.Page
        assertEquals("New message", refreshed.data.single().content)
        assertEquals(1, refreshed.data.single().unseenMessageCount)
        assertFalse(refreshedPagingSource.invalid)

        val secondMessageId = "second-incoming-message"
        database.runInTransaction {
            messageDao.insert(
                MessageBuilder(
                    secondMessageId,
                    conversationId,
                    userId,
                    MessageCategory.PLAIN_TEXT.name,
                    MessageStatus.DELIVERED.name,
                    "2026-06-29T00:02:00Z",
                ).setContent("Second message").build(),
            )
            database.remoteMessageStatusDao().insert(
                RemoteMessageStatus(secondMessageId, conversationId, MessageStatus.DELIVERED.name),
            )
            database.conversationDao().updateLastMessageId(
                secondMessageId,
                "2026-06-29T00:02:00Z",
                conversationId,
            )
            database.remoteMessageStatusDao().updateConversationUnseen(conversationId)
        }

        withTimeout(5_000) {
            while (!refreshedPagingSource.invalid) yield()
        }

        val secondRefresh =
            DataProvider.observeConversations(database).load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = true,
                ),
            ) as PagingSource.LoadResult.Page
        assertEquals("Second message", secondRefresh.data.single().content)
        assertEquals(2, secondRefresh.data.single().unseenMessageCount)
    }

    private fun conversation(
        id: String,
        ownerId: String? = null,
        unseenMessageCount: Int = 0,
    ) =
        Conversation(
            conversationId = id,
            ownerId = ownerId,
            category = ConversationCategory.CONTACT.name,
            name = null,
            iconUrl = null,
            announcement = null,
            codeUrl = null,
            payType = null,
            createdAt = "2026-06-29T00:00:00Z",
            pinTime = null,
            lastMessageId = null,
            lastReadMessageId = null,
            unseenMessageCount = unseenMessageCount,
            status = ConversationStatus.SUCCESS.ordinal,
        )

    private fun user(id: String) =
        User(
            userId = id,
            identityNumber = "1",
            relationship = "",
            biography = "",
            fullName = "User",
            avatarUrl = null,
            phone = null,
            isVerified = false,
            createdAt = null,
            muteUntil = null,
        )

    private fun imageMessage(
        id: String,
        conversationId: String,
        userId: String,
        createdAt: String,
    ) =
        MessageBuilder(id, conversationId, userId, MessageCategory.PLAIN_IMAGE.name, MessageStatus.SENT.name, createdAt)
            .setMediaStatus(MediaStatus.DONE.name)
            .setMediaWidth(100)
            .setMediaHeight(100)
            .build()

    private fun appCardMessage(
        id: String,
        conversationId: String,
        userId: String,
        content: String,
        createdAt: String,
    ) =
        MessageBuilder(id, conversationId, userId, MessageCategory.APP_CARD.name, MessageStatus.SENT.name, createdAt)
            .setContent(content)
            .build()
}
