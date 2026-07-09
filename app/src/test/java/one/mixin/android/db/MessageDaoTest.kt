package one.mixin.android.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
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

    private fun conversation(id: String) =
        Conversation(
            conversationId = id,
            ownerId = null,
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
            unseenMessageCount = 0,
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
