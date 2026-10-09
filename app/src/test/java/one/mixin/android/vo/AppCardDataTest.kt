package one.mixin.android.vo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCardDataTest {
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

    @Test
    fun `safe image url accepts public https url`() {
        assertTrue("https://cdn.example.com/cover.jpg".safeAppCardImageUrl() != null)
    }

    @Test
    fun `safe image url rejects non-https and local targets`() {
        assertFalse("http://cdn.example.com/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://localhost/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://127.0.0.1/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://10.0.0.1/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://[::1]/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://[fd00::1]/cover.jpg".safeAppCardImageUrl() != null)
        assertFalse("https://router.local/cover.jpg".safeAppCardImageUrl() != null)
    }

    @Test
    fun `unsafe media cover is not exposed`() {
        val appCardData = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = "https://127.0.0.1/cover.jpg",
            cover = null,
            title = "title",
            description = null,
            action = null,
            updatedAt = null,
            shareable = null,
        )

        assertFalse(appCardData.hasMediaCover)
        assertFalse(appCardData.hashCover)
    }
}
