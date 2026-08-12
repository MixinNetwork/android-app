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
}
