package one.mixin.android.ui.landing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LandingActivityStateRestorationTest {
    @Test
    fun `initializes landing content on first creation`() {
        assertTrue(shouldInitializeLanding(hasSavedInstanceState = false))
    }

    @Test
    fun `keeps restored fragment stack after recreation`() {
        assertFalse(shouldInitializeLanding(hasSavedInstanceState = true))
    }

    @Test
    fun `does not retry failed anonymous login after state restoration`() {
        assertFalse(shouldRequestAnonymousLogin(restoredErrorInfo = "ERROR 429: Rate limit exceeded"))
    }

    @Test
    fun `starts anonymous login without a restored failure`() {
        assertTrue(shouldRequestAnonymousLogin(restoredErrorInfo = null))
    }
}
