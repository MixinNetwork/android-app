package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.RemoteMessageStatus
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteMessageStatusDaoTest {
    private lateinit var database: MixinDatabase
    private lateinit var remoteMessageStatusDao: RemoteMessageStatusDao

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        remoteMessageStatusDao = database.remoteMessageStatusDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun pendingCleanupLoadsOnlyRequestedMessageMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pending = Room.inMemoryDatabaseBuilder(context, PendingDatabaseImp::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            RoomDatabaseCompat.execute(
                pending,
                "INSERT INTO pending_messages(id, conversation_id, user_id, category, content, status, created_at, media_url) VALUES ('pending', 'conversation', 'user', 'PLAIN_IMAGE', '', 'DELIVERED', '', 'media/image.jpg')",
            )
            val result = pending.pendingMessageDao().findMessageMediaByIds(listOf("pending", "missing"))
            assertEquals(1, result.size)
            assertEquals("pending", result.single().messageId)
            assertEquals("PLAIN_IMAGE", result.single().type)
            assertEquals("conversation", result.single().conversationId)
            assertEquals("media/image.jpg", result.single().mediaUrl)
        } finally {
            pending.close()
        }
    }

    @Test
    fun readStatusesJoinOnlyTheirExpiredMessages() = runBlocking {
        remoteMessageStatusDao.insert(
            RemoteMessageStatus("with-expiration", "conversation", MessageStatus.READ.name),
            RemoteMessageStatus("without-expiration", "conversation", MessageStatus.READ.name),
        )
        database.expiredMessageDao().insert(
            ExpiredMessage("with-expiration", 60, 100),
            ExpiredMessage("unrelated", 60, 200),
        )

        val statuses = remoteMessageStatusDao.findRemoteMessageStatus().associateBy { it.messageId }

        assertEquals(2, statuses.size)
        assertEquals(100, statuses.getValue("with-expiration").expireAt)
        assertEquals(null, statuses.getValue("without-expiration").expireAt)
    }

    @Test
    fun firstUnreadMessageIdSkipsReadStatuses() = runBlocking {
        remoteMessageStatusDao.insert(
            RemoteMessageStatus("read", "conversation", MessageStatus.READ.name),
            RemoteMessageStatus("first-unread", "conversation", MessageStatus.DELIVERED.name),
            RemoteMessageStatus("second-unread", "conversation", MessageStatus.DELIVERED.name),
        )

        assertEquals("first-unread", remoteMessageStatusDao.firstUnreadMessageId("conversation"))
    }
}
