package one.mixin.android.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebOriginPermissionGrantsTest {
    @Test
    fun `microphone grant does not cover a later camera request`() {
        val grants = WebOriginPermissionGrants()
        grants.grant(ORIGIN, arrayOf(AUDIO))

        assertTrue(grants.covers(ORIGIN, arrayOf(AUDIO)))
        assertFalse(grants.covers(ORIGIN, arrayOf(VIDEO)))
        assertFalse(grants.covers(ORIGIN, arrayOf(AUDIO, VIDEO)))
        assertFalse(grants.covers(ORIGIN, emptyArray()))
    }

    @Test
    fun `request is covered only after every resource was granted`() {
        val grants = WebOriginPermissionGrants()
        grants.grant(ORIGIN, arrayOf(AUDIO))
        grants.grant(ORIGIN, arrayOf(VIDEO))

        assertTrue(grants.covers(ORIGIN, arrayOf(AUDIO)))
        assertTrue(grants.covers(ORIGIN, arrayOf(VIDEO)))
        assertTrue(grants.covers(ORIGIN, arrayOf(VIDEO, AUDIO)))
    }

    @Test
    fun `grants stay scoped to the origin that received them`() {
        val grants = WebOriginPermissionGrants()
        grants.grant(ORIGIN, arrayOf(AUDIO, VIDEO))

        assertFalse(grants.covers(OTHER_ORIGIN, arrayOf(AUDIO)))
        assertFalse(grants.covers(OTHER_ORIGIN, arrayOf(VIDEO)))
    }

    @Test
    fun `clear drops remembered grants`() {
        val grants = WebOriginPermissionGrants()
        grants.grant(ORIGIN, arrayOf(AUDIO))
        grants.grant(OTHER_ORIGIN, arrayOf(VIDEO))

        grants.clear()

        assertFalse(grants.covers(ORIGIN, arrayOf(AUDIO)))
        assertFalse(grants.covers(OTHER_ORIGIN, arrayOf(VIDEO)))
    }

    private companion object {
        const val ORIGIN = "https://example.com"
        const val OTHER_ORIGIN = "https://other.example"
        const val AUDIO = "android.webkit.resource.AUDIO_CAPTURE"
        const val VIDEO = "android.webkit.resource.VIDEO_CAPTURE"
    }
}
