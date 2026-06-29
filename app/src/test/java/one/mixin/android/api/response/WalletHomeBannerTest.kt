package one.mixin.android.api.response

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletHomeBannerTest {
    @Test
    fun bannerDefaultsMatchRewardsNullability() {
        val banner = WalletHomeBanner()

        assertEquals("", banner.bannerId)
        assertEquals("", banner.placement)
        assertEquals("", banner.lang)
        assertEquals("", banner.iconUrl)
        assertEquals("", banner.title)
        assertEquals("", banner.description)
        assertEquals(emptyList<WalletHomeBannerAction>(), banner.actions)
        assertNull(banner.actionUrl)
        assertEquals("", banner.trackingKey)
        assertEquals(WalletHomeBanner.BANNER_STATUS_ACTIVE, banner.status)
        assertEquals("", banner.startAt)
        assertEquals("", banner.endAt)
        assertEquals(emptyList<String>(), banner.chains)
        assertEquals("", banner.createdAt)
        assertEquals("", banner.updatedAt)

        val action = WalletHomeBannerAction()
        assertEquals("", action.label)
        assertEquals("", action.action)
    }

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
                actions = listOf(WalletHomeBannerAction(label = "Claim", action = "")),
            ).hasButtonStyle,
        )
    }

    @Test
    fun visibleActionsOnlyUsesFirstValidAction() {
        val banner = WalletHomeBanner(
            actions = listOf(
                WalletHomeBannerAction(label = "", action = "mixin://skip"),
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
