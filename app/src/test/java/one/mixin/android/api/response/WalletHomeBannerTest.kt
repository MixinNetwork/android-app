package one.mixin.android.api.response

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletHomeBannerTest {
    @Test
    fun bannerWithActionsUsesButtonStyle() {
        assertTrue(
            WalletHomeBanner(
                actions = listOf(WalletHomeBannerAction(label = "Claim", action = "mixin://buy")),
            ).hasButtonStyle,
        )
    }

    @Test
    fun bannerWithoutActionsUsesNoButtonStyle() {
        assertFalse(WalletHomeBanner().hasButtonStyle)
        assertFalse(
            WalletHomeBanner(
                actions = listOf(WalletHomeBannerAction(label = "Claim", action = null)),
            ).hasButtonStyle,
        )
    }

    @Test
    fun visibleActionsOnlyUsesFirstValidAction() {
        val banner = WalletHomeBanner(
            actions = listOf(
                WalletHomeBannerAction(label = null, action = "mixin://skip"),
                WalletHomeBannerAction(label = "First", action = "mixin://first"),
                WalletHomeBannerAction(label = "Second", action = "mixin://second"),
            ),
        )

        assertEquals(listOf("First"), banner.visibleActions.map { it.label })
    }

    @Test
    fun syncClosedBannerIdsKeepsOnlyRemoteBannerKeys() {
        val remoteBanners = listOf(
            WalletHomeBanner(bannerId = "remote-1"),
            WalletHomeBanner(bannerId = "remote-2"),
        )

        assertEquals(
            setOf("remote-1"),
            setOf("remote-1", "removed").syncedWalletHomeClosedBannerIds(remoteBanners),
        )
    }

}
