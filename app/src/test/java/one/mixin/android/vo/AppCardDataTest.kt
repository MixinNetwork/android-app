package one.mixin.android.vo

import one.mixin.android.util.GsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppCardDataTest {
    @Test
    fun nestedCoverUrlCountsAsCover() {
        val coverUrl = "https://example.com/cover.png"
        val appCardData = AppCardData(
            appId = "app-id",
            iconUrl = null,
            coverUrl = null,
            cover = Cover(
                height = 100,
                width = 100,
                mimeType = "image/png",
                url = coverUrl,
                thumbnail = null,
            ),
            title = "title",
            description = "description",
            action = null,
            updatedAt = null,
            shareable = null,
        )
        val message = create(MessageCategory.APP_CARD.name).copy(
            content = GsonHelper.customGson.toJson(appCardData),
        )

        assertTrue(appCardData.hasCover)
        assertEquals(coverUrl, message.appCardCoverUrl())
    }
}
