package one.mixin.android.ui.url

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlInterpreterActivityTest {
    @Test
    fun routeWithoutCodeDoesNotUseRouteNameAsReferral() {
        assertEquals("", referralCodeFromMixinPath(listOf("referrals")))
    }

    @Test
    fun readsReferralCodeImmediatelyAfterRoute() {
        assertEquals("CODE123", referralCodeFromMixinPath(listOf("referrals", "CODE123")))
        assertEquals(
            "CODE123",
            referralCodeFromMixinPath(listOf("referrals", "CODE123", "ignored")),
        )
    }
}
