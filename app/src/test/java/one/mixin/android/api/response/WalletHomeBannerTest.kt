package one.mixin.android.api.response

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.Instant

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
    fun filterByChainsKeepsGlobalAndMatchingBanners() {
        val banners = listOf(
            WalletHomeBanner(bannerId = "global"),
            WalletHomeBanner(bannerId = "blank", chains = listOf("")),
            WalletHomeBanner(bannerId = "ethereum", chains = listOf("ethereum")),
            WalletHomeBanner(bannerId = "multi-chain", chains = listOf("bitcoin", "ethereum")),
            WalletHomeBanner(bannerId = "solana", chains = listOf("solana")),
        )

        assertEquals(
            listOf("global", "blank", "ethereum", "multi-chain"),
            banners.filterWalletHomeBannersByChains(listOf("ethereum")).map { it.bannerId },
        )
    }

    @Test
    fun filterByChainsWithoutWalletChainsKeepsAllBanners() {
        val banners = listOf(
            WalletHomeBanner(bannerId = "global"),
            WalletHomeBanner(bannerId = "ethereum", chains = listOf("ethereum")),
        )

        assertEquals(banners, banners.filterWalletHomeBannersByChains(emptyList()))
    }

    @Test
    fun syncClosedBannerIdsKeepsExistingIdsWhenRemoteResponseIsPartial() {
        val remoteBanners = listOf(
            WalletHomeBanner(bannerId = "remote-1"),
            WalletHomeBanner(bannerId = "remote-2"),
        )

        assertEquals(
            setOf("remote-1", "other-wallet-banner", "missing-after-failed-request"),
            setOf("remote-1", "other-wallet-banner", "missing-after-failed-request")
                .syncedWalletHomeClosedBannerIds(remoteBanners),
        )
    }

    @Test
    fun visibleBannersExcludeExpiredItems() {
        val now = Instant.parse("2026-07-08T10:00:00Z")

        val banners = listOf(
            visibleBanner("expired", endAt = "2026-07-08T09:59:59Z"),
            visibleBanner("future", endAt = "2026-07-08T10:00:01Z"),
            visibleBanner("open-ended"),
        ).visibleWalletHomeBanners(emptySet(), now)

        assertEquals(listOf("future", "open-ended"), banners.map { it.bannerId })
    }

    @Test
    fun visibleBannersOnlyIncludeWalletPlacements() {
        val banners = listOf(
            visibleBanner("legacy"),
            visibleBanner("wallet", placement = WalletHomeBanner.BANNER_PLACEMENT_WALLET),
            visibleBanner("other", placement = "home_banner"),
        ).visibleWalletHomeBanners(emptySet())

        assertEquals(listOf("legacy", "wallet"), banners.map { it.bannerId })
    }

    private fun visibleBanner(
        id: String,
        endAt: String = "",
        placement: String = "",
    ) = WalletHomeBanner(
        bannerId = id,
        placement = placement,
        title = id,
        actionUrl = "mixin://$id",
        endAt = endAt,
    )

}
