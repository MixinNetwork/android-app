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
    fun `preserves captcha failure without an error message`() {
        assertFalse(shouldRequestAnonymousLogin(restoredRequestFailed = true))
    }

    @Test
    fun `starts anonymous login when no failure was restored`() {
        assertTrue(shouldRequestAnonymousLogin(restoredRequestFailed = false))
    }
}
