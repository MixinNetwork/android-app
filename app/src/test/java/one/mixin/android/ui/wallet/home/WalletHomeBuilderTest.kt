package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.WalletHomeBannerAction
import one.mixin.android.api.response.visibleWalletHomeBanners
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeBuilderTest {
    @Test
    fun privacyWalletWithPositionsDoesNotShowTopMovers() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.POSITIONS,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun privacyWalletWithoutPositionsShowsTopMoversAfterTransactions() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.TOP_MOVERS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun classicWalletDoesNotShowPerpetualCards() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun visibleDynamicBannerShowsBannerCard() {
        val banners = listOf(
            WalletHomeBanner(
                bannerId = "banner-1",
                title = "Promo",
                actionUrl = "https://example.com",
                status = "active",
            ),
        ).visibleWalletHomeBanners(closedBannerIds = emptySet())

        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = banners.isNotEmpty(),
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun classicWalletShowsDynamicBannerCard() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = true,
            showBanner = true,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun closedDynamicBannerDoesNotShowBannerCard() {
        val banners = listOf(
            WalletHomeBanner(
                bannerId = "banner-1",
                title = "Promo",
                actionUrl = "https://example.com",
                status = "active",
            ),
        ).visibleWalletHomeBanners(closedBannerIds = setOf("banner-1"))

        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = banners.isNotEmpty(),
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun visibleDynamicBannersPreferPriorityAndIgnoreInactive() {
        val banners = listOf(
            WalletHomeBanner(
                bannerId = "low",
                title = "Low",
                actionUrl = "https://example.com/low",
                status = "active",
                priority = 1,
            ),
            WalletHomeBanner(
                bannerId = "inactive",
                title = "Inactive",
                actionUrl = "https://example.com/inactive",
                status = "inactive",
                priority = 100,
            ),
            WalletHomeBanner(
                bannerId = "closed",
                title = "Closed",
                actionUrl = "https://example.com/closed",
                status = "active",
                priority = 90,
            ),
            WalletHomeBanner(
                bannerId = "high",
                title = "High",
                actionUrl = "https://example.com/high",
                status = "active",
                priority = 10,
            ),
        ).visibleWalletHomeBanners(closedBannerIds = setOf("closed"))

        assertEquals(listOf("high", "low"), banners.map { it.bannerId })
    }

    @Test
    fun dynamicBannerWithActionsShowsBannerCard() {
        val banners = listOf(
            WalletHomeBanner(
                bannerId = "banner-1",
                title = "Promo",
                status = "active",
                actions = listOf(WalletHomeBannerAction(label = "Claim", action = "mixin://buy")),
            ),
        ).visibleWalletHomeBanners(closedBannerIds = emptySet())

        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = banners.isNotEmpty(),
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }
}
