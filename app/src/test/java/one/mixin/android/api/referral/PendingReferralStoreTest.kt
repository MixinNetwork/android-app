package one.mixin.android.api.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingReferralStoreTest {
    @Test
    fun extractCodeFromDeepLink_readsSub1ForReferralValue() {
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromDeepLink("referral", "CRMLSV"),
        )
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromDeepLink("REFERRAL", "  crmlsv "),
        )
    }

    @Test
    fun extractCodeFromDeepLink_ignoresEmptyOrOtherActions() {
        assertNull(PendingReferralStore.extractCodeFromDeepLink("referral", ""))
        assertNull(PendingReferralStore.extractCodeFromDeepLink("referral", "   "))
        assertNull(PendingReferralStore.extractCodeFromDeepLink("trade", "CRMLSV"))
        assertNull(PendingReferralStore.extractCodeFromDeepLink(null, "CRMLSV"))
    }

    @Test
    fun extractCodeFromDeepLink_supportsReferralUrlFallback() {
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromDeepLink("https://mixin.one/referrals/crmlsv", null),
        )
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromDeepLink("mixin://mixin.one/referrals/CRMLSV", null),
        )
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromDeepLink("mixin://referrals/CRMLSV", null),
        )
    }

    @Test
    fun extractCodeFromUrl_handlesQueryAndRejectsNonReferralUrls() {
        assertEquals(
            "CRMLSV",
            PendingReferralStore.extractCodeFromUrl("https://mixin.one/referrals/CRMLSV?utm_source=share"),
        )
        assertNull(PendingReferralStore.extractCodeFromUrl("https://mixin.one/referrals"))
        assertNull(PendingReferralStore.extractCodeFromUrl("https://mixin.one/users/123"))
        assertNull(PendingReferralStore.extractCodeFromUrl("https://example.com/referrals/CRMLSV"))
    }

    @Test
    fun isOneLinkUrl_matchesOnlyMixinOneLinkReferralTemplateHttps() {
        assertTrue(
            PendingReferralStore.isOneLinkUrl(
                "https://mixin.onelink.me/rlR3?af_xp=referral&pid=User_invite&c=Referral&deep_link_value=referral&deep_link_sub1=WLKETH",
            ),
        )
        assertTrue(PendingReferralStore.isOneLinkUrl("https://mixin.onelink.me/rlR3/shortId"))
        assertFalse(PendingReferralStore.isOneLinkUrl("https://mixin.onelink.me/dHGb?pid=referral"))
        assertFalse(PendingReferralStore.isOneLinkUrl("https://mixin.onelink.me/rlR3x"))
        assertFalse(PendingReferralStore.isOneLinkUrl("http://mixin.onelink.me/rlR3"))
        assertFalse(PendingReferralStore.isOneLinkUrl("https://mixin.one/referrals/CRMLSV"))
        assertFalse(PendingReferralStore.isOneLinkUrl(null))
    }
}
