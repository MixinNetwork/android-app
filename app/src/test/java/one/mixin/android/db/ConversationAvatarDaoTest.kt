package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAvatarDaoTest {
    private lateinit var database: MixinDatabase
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        conversationDao = database.conversationDao()
        conversationDao.upsert(
            ConversationBuilder("group", "2026-01-01T00:00:00Z", ConversationStatus.SUCCESS.ordinal)
                .setOwnerId("owner")
                .setCategory(ConversationCategory.GROUP.name)
                .build(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun customAvatarsReplaceLocalCacheAndRejectLateGeneratedAvatars() {
        conversationDao.updateGeneratedGroupIconUrl("group", "/cache/default.png")
        for (iconUrl in listOf("https://example.com/first.png", "http://example.com/second.png", "HTTPS://example.com/third.png")) {
            syncIcon(iconUrl)
            assertEquals(iconUrl, conversationDao.getGroupIconUrl("group"))

            conversationDao.updateGeneratedGroupIconUrl("group", "/cache/stale.png")
            assertEquals(iconUrl, conversationDao.getGroupIconUrl("group"))
        }
    }

    @Test
    fun defaultAvatarRefreshPreservesGeneratedCache() {
        conversationDao.updateGeneratedGroupIconUrl("group", "/cache/default.png")
        for (iconUrl in listOf("", null)) {
            syncIcon(iconUrl)
            assertEquals("/cache/default.png", conversationDao.getGroupIconUrl("group"))
        }
    }

    @Test
    fun removingCustomAvatarAllowsDefaultAvatarGeneration() {
        for (iconUrl in listOf("", null)) {
            syncIcon("https://example.com/custom.png")
            syncIcon(iconUrl)
            assertNull(conversationDao.getGroupIconUrl("group"))

            conversationDao.updateGeneratedGroupIconUrl("group", "/cache/default.png")
            assertEquals("/cache/default.png", conversationDao.getGroupIconUrl("group"))
        }
    }

    @Test
    fun generatedAvatarCanReplaceAnOlderLocalCache() {
        conversationDao.updateGeneratedGroupIconUrl("group", "/cache/old.png")
        conversationDao.updateGeneratedGroupIconUrl("group", "/cache/new.png")

        assertEquals("/cache/new.png", conversationDao.getGroupIconUrl("group"))
    }

    private fun syncIcon(iconUrl: String?) {
        conversationDao.updateConversation(
            conversationId = "group",
            ownerId = "owner",
            category = ConversationCategory.GROUP.name,
            name = "Group",
            iconUrl = iconUrl,
            announcement = "",
            muteUntil = "",
            createdAt = "2026-01-01T00:00:00Z",
            expireIn = null,
            status = ConversationStatus.SUCCESS.ordinal,
        )
    }
}
