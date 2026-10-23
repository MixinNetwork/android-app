package one.mixin.android.vo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.util.GsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class AppCardForwardingTest {
    @Before
    fun setUp() {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `forwarding follows case insensitive send prefix exclusions`() {
        val cases = mapOf(
            "mixin://apps/example" to true,
            "MIXIN://users/example" to true,
            "https://example.com" to true,
            "http://example.com" to true,
            "custom://example" to true,
            "mixin://send?user=example" to false,
            "mixin://mixin.one/send?text=hello" to false,
            "https://mixin.one/send?user=example" to false,
            "MiXiN://SeNd" to false,
            "MIXIN://MIXIN.ONE/SEND" to false,
            "HTTPS://MIXIN.ONE/SEND" to false,
            "mixin://sender" to false,
        )
        for ((action, expected) in cases) {
            val card = card(listOf(action))
            assertEquals(action, expected, card.canShare)
            assertTranscript(card, expected)
        }
    }

    @Test
    fun `forwarding preserves empty actions and removes mixed send actions`() {
        for (actions in listOf(null, emptyList<ActionButtonData>())) {
            val card = card().copy(actions = actions)
            assertTrue(card.canShare)
            assertTranscript(card, true)
        }
        val mixed = card(listOf("mixin://apps/example", "mixin://send?user=example"))
        assertFalse(mixed.canShare)
        assertTranscript(mixed, false)
    }

    @Test
    fun `forwarding removes buttons when sharing is not enabled`() {
        for (shareable in listOf(false, null)) {
            val card = card().copy(shareable = shareable)
            assertFalse(card.canShare)
            assertTranscript(card, false)
        }
    }

    private fun card(actions: List<String> = listOf("mixin://apps/example")) = AppCardData(
        appId = "app-id",
        iconUrl = null,
        coverUrl = null,
        cover = null,
        title = "title",
        description = "description",
        action = null,
        updatedAt = null,
        shareable = true,
        actions = actions.map { ActionButtonData("button", "", it) },
    )

    private fun assertTranscript(card: AppCardData, preserveActions: Boolean) {
        val gson = GsonHelper.customGson
        val content = gson.toJson(card)
        val message = MessageItem(
            messageId = "message-id",
            conversationId = "conversation-id",
            userId = "user-id",
            userFullName = "sender",
            userIdentityNumber = "12345",
            type = MessageCategory.APP_CARD.name,
            content = content,
            createdAt = "2026-09-08T00:00:00.000Z",
            status = MessageStatus.READ.name,
            mediaStatus = null,
            userAvatarUrl = null,
            mediaName = null,
            mediaMimeType = null,
            mediaSize = null,
            thumbUrl = null,
            mediaWidth = null,
            mediaHeight = null,
            thumbImage = null,
            mediaUrl = null,
            mediaDuration = null,
            participantFullName = null,
            participantUserId = null,
            actionName = null,
            snapshotId = null,
            snapshotType = null,
            snapshotMemo = null,
            snapshotAmount = null,
            assetId = null,
            assetType = null,
            assetSymbol = null,
            assetIcon = null,
            assetCollectionHash = null,
            assetUrl = null,
            assetHeight = null,
            assetWidth = null,
            albumId = null,
            stickerId = null,
            assetName = null,
            appId = card.appId,
        )
        val transcript = message.toTranscript("transcript-id")
        val expected = if (preserveActions) card else card.copy(actions = null)
        assertEquals(expected, gson.fromJson(transcript.content, AppCardData::class.java))
        if (preserveActions) assertEquals(content, transcript.content)
    }
}
