package one.mixin.android.ui.web

import one.mixin.android.vo.App
import org.junit.Assert.assertEquals
import org.junit.Test

class WebLoadErrorTargetTest {
    @Test
    fun `bot target identifies app without exposing domain`() {
        assertEquals(
            "Mixin Route (70001001000)",
            webLoadErrorTarget(botApp, FAILING_URL),
        )
    }

    @Test
    fun `regular web target keeps failing url`() {
        assertEquals(FAILING_URL, webLoadErrorTarget(null, FAILING_URL))
    }

    private companion object {
        const val FAILING_URL = "route.mixin.space"

        val botApp =
            App(
                appId = "bot-id",
                appNumber = "70001001000",
                homeUri = "https://route.mixin.space",
                redirectUri = "",
                name = "Mixin Route",
                iconUrl = "",
                category = null,
                description = "",
                appSecret = "",
                capabilities = null,
                creatorId = "creator-id",
                resourcePatterns = null,
                updatedAt = null,
            )
    }
}
