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
import org.junit.Assert.assertNull
import one.mixin.android.db.runInTransaction
import one.mixin.android.db.deleteMessageByIds
import one.mixin.android.db.insertConversationMessages
import one.mixin.android.vo.RemoteMessageStatus
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
    fun initMessagesFillsWindowAroundUnreadAnchor() = runBlocking {
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

    @Test
    fun initialLoadDoesNotPopulateCountCache() = runBlocking {
        insertMessage(1, "unread", "2024-01-01T00:00:00.000Z")
        insertRemoteStatus("unread")
        val (position, data) = fetcher.initMessages(CONVERSATION_ID)
        assertEquals("unread", data[position].messageId)
        assertNull(db.conversationExtDao().getMessageCountByConversationId(CONVERSATION_ID))
    }

    @Test
    fun receiveReplayAndBatchDeleteKeepCountsExact() {
        insertConversationExt(0)
        insertMessage(1, "template", "2024-01-01T00:00:00.000Z")
        val message = requireNotNull(db.messageDao().findMessageById("template"))
        db.messageDao().deleteMessageById("template")
        val status = RemoteMessageStatus(message.messageId, CONVERSATION_ID, "DELIVERED")
        repeat(2) {
            db.insertConversationMessages(CONVERSATION_ID, listOf(message), listOf(status))
        }
        assertEquals(1, db.conversationExtDao().getMessageCountByConversationId(CONVERSATION_ID))
        assertEquals(1, MessageFetcherGenerated.findInitialPosition(db, CONVERSATION_ID))
        db.remoteMessageStatusDao().markRead(CONVERSATION_ID)
        db.insertConversationMessages(CONVERSATION_ID, listOf(message), listOf(status))
        assertEquals(0, MessageFetcherGenerated.findInitialPosition(db, CONVERSATION_ID))
        repeat(2) { db.deleteMessageByIds(listOf("template", "template", "missing")) }
        assertEquals(0, db.conversationExtDao().getMessageCountByConversationId(CONVERSATION_ID))
        assertEquals(0, MessageFetcherGenerated.countMessages(db, CONVERSATION_ID))
    }

    @Test
    fun deletingUnreadMessagesUpdatesBothCounts() {
        insertConversationExt(2)
        updateUnseenCount(2)
        insertMessage(1, "first", "2024-01-01T00:00:00.000Z")
        insertMessage(2, "last", "2024-01-02T00:00:00.000Z")
        insertRemoteStatus("first")
        insertRemoteStatus("last")
        db.deleteMessageByIds(listOf("first", "missing"))
        assertEquals(1, db.conversationExtDao().getMessageCountByConversationId(CONVERSATION_ID))
        assertEquals(1, MessageFetcherGenerated.findInitialPosition(db, CONVERSATION_ID))
        assertEquals("last", db.messageDao().findMessageById("last")?.messageId)
    }

    @Test
    fun trimmedPagesCanBeLoadedAgainInBothDirections() = runBlocking {
        repeat(180) { index ->
            insertMessage(index + 1, "message-$index", index.toString().padStart(12, '0'))
        }
        val (_, initial) = fetcher.initMessages(CONVERSATION_ID, forceBottom = true)
        val previous = fetcher.previousPage(CONVERSATION_ID, initial.first().messageId)
        assertTrue(previous.isNotEmpty())
        fetcher.onWindowTrimmed(fromStart = false)
        val next = fetcher.nextPage(CONVERSATION_ID, previous.last().messageId)
        assertEquals(initial.take(30).map { it.messageId }, next.map { it.messageId })
        fetcher.onWindowTrimmed(fromStart = true)
        val previousAgain = fetcher.previousPage(CONVERSATION_ID, initial.first().messageId)
        assertEquals(previous.map { it.messageId }, previousAgain.map { it.messageId })
    }

    @Test
    fun markReadDrainsMultipleBatchesAndPreservesReceipts() {
        db.runInTransaction {
            repeat(1100) { insertRemoteStatus("unread-$it") }
            updateUnseenCount(1100)
        }
        assertEquals(500, db.remoteMessageStatusDao().markReadBatch(CONVERSATION_ID))
        assertEquals(600, MessageFetcherGenerated.findInitialPosition(db, CONVERSATION_ID))
        db.remoteMessageStatusDao().markRead(CONVERSATION_ID)
        assertEquals(0, MessageFetcherGenerated.findInitialPosition(db, CONVERSATION_ID))
        assertEquals(0, db.remoteMessageStatusDao().countUnread(CONVERSATION_ID))
        RoomDatabaseCompat.query(db, "SELECT count(*) FROM remote_messages_status WHERE status = 'READ'").use {
            assertTrue(it.moveToFirst())
            assertEquals(1100, it.getInt(0))
        }
    }

    @Test
    fun clearHistoryBatchesPreserveNewArrivalsAndFindLaterTranscripts() = runBlocking {
        db.runInTransaction {
            repeat(1100) { index ->
                insertMessage(index + 1, "old-$index", (1100 - index).toString().padStart(12, '0'))
            }
            insertConversationExt(1101)
            RoomDatabaseCompat.execute(db, "UPDATE messages SET category = 'PLAIN_TRANSCRIPT' WHERE id = 'old-1099'")
        }
        val cutoff = requireNotNull(db.messageDao().getLastMessageRowId())
        insertMessage(2000, "arrival", "999999999999")
        val transcripts = mutableListOf<String>()
        var deleted = 0
        while (true) {
            val ids = db.messageDao().getMessageIdsByConversationId(CONVERSATION_ID, cutoff, 500)
            if (ids.isEmpty()) break
            transcripts += db.messageDao().getMessagesForDeletion(ids).map { it.messageId }
            db.deleteMessageByIds(ids)
            deleted += ids.size
        }
        assertEquals(1100, deleted)
        assertEquals(listOf("old-1099"), transcripts)
        assertEquals(1, db.conversationExtDao().getMessageCountByConversationId(CONVERSATION_ID))
        assertEquals("arrival", db.messageDao().findLastMessageId(CONVERSATION_ID))
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
