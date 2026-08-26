package one.mixin.android.db.fetcher

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageFetcherAnchorTest {
    private lateinit var db: MixinDatabase
    private lateinit var fetcher: MessageFetcher

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        fetcher = MessageFetcher(db)
        insertUser()
        insertConversation()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun initMessagesKeepsChronologicalNeighborsWhenRowIdAndCreatedAtDiffer() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "anchor", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")

        val (position, data, unreadMessageId) = fetcher.initMessages(CONVERSATION_ID, "anchor")

        assertEquals("anchor", unreadMessageId)
        assertEquals(1, position)
        assertEquals(listOf("old", "anchor", "new"), data.map { it.messageId })
    }

    @Test
    fun initMessagesUsesRowIdToOrderMessagesWithSameTimestamp() = runBlocking {
        val createdAt = "2024-01-01T00:00:00.000Z"
        insertMessage(rowId = 3, messageId = "new", createdAt = createdAt)
        insertMessage(rowId = 1, messageId = "old", createdAt = createdAt)
        insertMessage(rowId = 2, messageId = "anchor", createdAt = createdAt)

        val (position, data, anchorMessageId) = fetcher.initMessages(CONVERSATION_ID, "anchor")

        assertEquals("anchor", anchorMessageId)
        assertEquals(1, position)
        assertEquals(listOf("old", "anchor", "new"), data.map { it.messageId })
    }

    @Test
    fun initMessagesUsesFirstUnreadMessageAsAnchor() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "unread", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")
        insertRemoteStatus("unread")

        val (position, data, unreadMessageId) = fetcher.initMessages(CONVERSATION_ID)

        assertEquals("unread", unreadMessageId)
        assertEquals("unread", data[position].messageId)
        assertEquals(listOf("old", "unread", "new"), data.map { it.messageId })
    }

    @Test
    fun initMessagesRevalidatesPrefetchedUnreadAnchor() = runBlocking {
        insertMessage(rowId = 4, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 3, messageId = "stale", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "unread", createdAt = "2024-01-03T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-04T00:00:00.000Z")
        insertRemoteStatus("stale", "READ")
        insertRemoteStatus("unread")

        val (position, data, unreadMessageId) =
            fetcher.initMessages(CONVERSATION_ID, initialUnreadMessageId = "stale")

        assertEquals("unread", unreadMessageId)
        assertEquals("unread", data[position].messageId)
    }

    @Test
    fun initMessagesUsesUnreadCountToBuildFastInitialWindow() = runBlocking {
        repeat(120) { offset ->
            val index = offset + 1
            val suffix = index.toString().padStart(3, '0')
            insertMessage(
                rowId = index,
                messageId = "message-$suffix",
                createdAt = "2024-01-${suffix}T00:00:00.000Z",
            )
        }
        insertConversationExt(120)
        updateUnseenCount(10)
        (111..120).forEach { index ->
            insertRemoteStatus("message-${index.toString().padStart(3, '0')}")
        }

        val (position, data, unreadMessageId) =
            fetcher.initMessages(
                conversationId = CONVERSATION_ID,
                initialUnreadMessageId = "message-111",
                initialUnreadCount = 10,
            )

        assertEquals(90, data.size)
        assertEquals("message-031", data.first().messageId)
        assertEquals("message-120", data.last().messageId)
        assertEquals("message-111", unreadMessageId)
        assertEquals(unreadMessageId, data[position].messageId)
    }

    @Test
    fun pagingKeepsChronologicalOrderInBothDirections() = runBlocking {
        repeat(149) { offset ->
            val index = offset + 1
            val suffix = index.toString().padStart(3, '0')
            insertMessage(
                rowId = index,
                messageId = "message-$suffix",
                createdAt = "2024-01-${suffix}T00:00:00.000Z",
            )
        }

        val (_, initial) = fetcher.initMessages(CONVERSATION_ID, "message-075")
        assertEquals("message-030", initial.first().messageId)
        assertEquals("message-119", initial.last().messageId)

        val newer = fetcher.nextPage(CONVERSATION_ID, initial.last().messageId)
        assertEquals("message-120", newer.first().messageId)
        assertEquals("message-149", newer.last().messageId)
        assertTrue(fetcher.isBottom())

        val older = fetcher.previousPage(CONVERSATION_ID, initial.first().messageId)
        assertEquals("message-001", older.first().messageId)
        assertEquals("message-029", older.last().messageId)
        assertTrue(fetcher.isTop())
    }

    @Test
    fun chatExportLoadsChronologicalIdPages() {
        repeat(130) { offset ->
            val index = offset + 1
            val suffix = index.toString().padStart(3, '0')
            insertMessage(
                rowId = index,
                messageId = "message-$suffix",
                createdAt = "2024-01-${suffix}T00:00:00.000Z",
            )
        }

        val messages = MessageDataSource(db).loadChatMessagesByOffset(CONVERSATION_ID, 100, 30)

        assertEquals("message-101", messages.first().messageId)
        assertEquals("message-130", messages.last().messageId)
    }

    @Test
    fun initMessagesAtDateUsesFirstMessageAtOrAfterDate() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "middle", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")

        val (position, data, anchorMessageId) = fetcher.initMessagesAtDate(CONVERSATION_ID, "2024-01-02T12:00:00.000Z")

        assertEquals("new", anchorMessageId)
        assertEquals("new", data[position].messageId)
    }

    @Test
    fun initMessagesAtDateFallsBackToLastMessageBeforeDate() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "middle", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")

        val (position, data, anchorMessageId) = fetcher.initMessagesAtDate(CONVERSATION_ID, "2024-01-04T00:00:00.000Z")

        assertEquals("new", anchorMessageId)
        assertEquals("new", data[position].messageId)
    }

    @Test
    fun initMessagesAtPositionClampsIndexAndFindsChronologicalAnchor() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "middle", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")

        val (position, data, anchorMessageId) = fetcher.initMessagesAtPosition(CONVERSATION_ID, 99)

        assertEquals("new", anchorMessageId)
        assertEquals("new", data[position].messageId)
    }

    @Test
    fun initMessagesAtPercentSupportsNormalizedAndWholeNumberPercent() = runBlocking {
        insertMessage(rowId = 3, messageId = "old", createdAt = "2024-01-01T00:00:00.000Z")
        insertMessage(rowId = 2, messageId = "middle", createdAt = "2024-01-02T00:00:00.000Z")
        insertMessage(rowId = 1, messageId = "new", createdAt = "2024-01-03T00:00:00.000Z")

        val normalized = fetcher.initMessagesAtPercent(CONVERSATION_ID, 0.5f)
        val wholeNumber = fetcher.initMessagesAtPercent(CONVERSATION_ID, 50f)

        assertTrue(normalized.second.isNotEmpty())
        assertEquals("middle", normalized.third)
        assertEquals("middle", normalized.second[normalized.first].messageId)
        assertEquals(normalized.third, wholeNumber.third)
    }

    private fun insertUser() {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO users(user_id, identity_number, relationship, biography, full_name)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(USER_ID, "1000", "FRIEND", "", "Test User"),
        )
    }

    private fun insertConversation() {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO conversations(conversation_id, owner_id, category, name, created_at, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(CONVERSATION_ID, USER_ID, "CONTACT", "Test Conversation", "2024-01-01T00:00:00.000Z", 0),
        )
    }

    private fun insertMessage(
        rowId: Int,
        messageId: String,
        createdAt: String,
    ) {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO messages(rowid, id, conversation_id, user_id, category, content, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(rowId, messageId, CONVERSATION_ID, USER_ID, "PLAIN_TEXT", messageId, "SENT", createdAt),
        )
    }

    private fun insertRemoteStatus(
        messageId: String,
        status: String = "DELIVERED",
    ) {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO remote_messages_status(message_id, conversation_id, status)
            VALUES (?, ?, ?)
            """.trimIndent(),
            arrayOf(messageId, CONVERSATION_ID, status),
        )
    }

    private fun insertConversationExt(count: Int) {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO conversation_ext(conversation_id, count, created_at)
            VALUES (?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(CONVERSATION_ID, count, "2024-01-01T00:00:00.000Z"),
        )
    }

    private fun updateUnseenCount(count: Int) {
        RoomDatabaseCompat.execute(
            db,
            "UPDATE conversations SET unseen_message_count = ? WHERE conversation_id = ?",
            arrayOf<Any?>(count, CONVERSATION_ID),
        )
    }

    private companion object {
        const val CONVERSATION_ID = "conversation-id"
        const val USER_ID = "user-id"
    }
}
