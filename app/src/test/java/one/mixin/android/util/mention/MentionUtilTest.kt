package one.mixin.android.util.mention

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.MentionUser
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MentionUtilTest {
    private lateinit var database: MixinDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MixinApplication.appContext = context
        database =
            Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun parseAppCardMentionDataResolvesMissingUsers() {
        val mention = MentionUser("7000105334", "Pump")

        val (mentions, _) =
            parseMentionData(
                text = "➡️ @7000105334 ⬅️",
                messageId = "message-id",
                conversationId = "conversation-id",
                userDao = database.userDao(),
                messageMentionDao = database.mentionMessageDao(),
                userId = "sender-id",
                resolveMissingUsers = { numbers ->
                    assertEquals(setOf("7000105334"), numbers)
                    listOf(mention)
                },
            )

        assertEquals(listOf(mention), mentions)
        val persisted = database.mentionMessageDao().findMessageMentionById("message-id")
        assertNotNull(persisted)
        assertEquals(
            "➡️ @Pump ⬅️",
            rendMentionContent(
                "➡️ @7000105334 ⬅️",
                mentions?.associate { it.identityNumber to it.fullName },
            ),
        )
    }
}
