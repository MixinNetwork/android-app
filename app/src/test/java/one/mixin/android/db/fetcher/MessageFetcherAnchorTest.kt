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

    private fun insertRemoteStatus(messageId: String) {
        RoomDatabaseCompat.execute(
            db,
            """
            INSERT INTO remote_messages_status(message_id, conversation_id, status)
            VALUES (?, ?, ?)
            """.trimIndent(),
            arrayOf(messageId, CONVERSATION_ID, "DELIVERED"),
        )
    }

    private companion object {
        const val CONVERSATION_ID = "conversation-id"
        const val USER_ID = "user-id"
    }
}
