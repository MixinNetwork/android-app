package one.mixin.android.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCardDataTest {
    @Test
    fun `share validation allows empty content on action cards`() {
        val card = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = "https://example.com/cover.jpg",
            cover = null,
            title = null,
            description = "",
            action = null,
            updatedAt = null,
            shareable = true,
            actions = listOf(ActionButtonData("Open", "#000000", "https://example.com")),
        )

        assertTrue(card.shareDataErrorReasons().isEmpty())
        assertEquals(listOf("titleLength=0"), card.copy(description = "text").shareDataErrorReasons())
        assertEquals(listOf("descriptionLength=0"), card.copy(title = "title").shareDataErrorReasons())
        assertEquals(listOf("titleLength=0", "descriptionLength=0"), card.copy(action = "https://example.com").shareDataErrorReasons())
        val invalidCover = Cover(1, 1, "image/jpeg", null, null)
        assertEquals(listOf("coverSize=1x1"), card.copy(cover = invalidCover).shareDataErrorReasons())
    }

    @Test
    fun `hashCover returns true when nested cover url exists`() {
        val appCardData = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = null,
            cover = Cover(
                height = 320,
                width = 640,
                mimeType = "image/jpeg",
                url = "https://example.com/cover.jpg",
                thumbnail = null,
            ),
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )

        assertTrue(appCardData.hashCover)
    }

    @Test
    fun `hashCover returns false when cover urls are blank`() {
        val appCardData = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = "",
            cover = Cover(
                height = 320,
                width = 640,
                mimeType = "image/jpeg",
                url = "",
                thumbnail = null,
            ),
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )

        assertFalse(appCardData.hashCover)
    }

    @Test
    fun `hashCover keeps cover url compatibility`() {
        val appCardData = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = "https://example.com/cover.jpg",
            cover = null,
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )

        assertTrue(appCardData.hashCover)
    }

    @Test
    fun `hasMediaCover returns true only when cover url exists`() {
        val coverUrlOnly = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = "https://example.com/cover.jpg",
            cover = null,
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )
        val nestedCover = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = null,
            cover = Cover(
                height = 320,
                width = 640,
                mimeType = "image/jpeg",
                url = "https://example.com/cover.jpg",
                thumbnail = null,
            ),
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )

        assertTrue(coverUrlOnly.hasMediaCover)
        assertFalse(nestedCover.hasMediaCover)
    }
}
