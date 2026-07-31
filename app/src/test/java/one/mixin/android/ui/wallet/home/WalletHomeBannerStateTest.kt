package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.WalletHomeBanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletHomeBannerStateTest {
    @Test
    fun cachedStateShowsAddWalletAndDynamicBannerTogether() {
        val banner = WalletHomeBanner(bannerId = "banner-1")

        val state = cachedState().withDynamicBanners(
            dynamicBanners = listOf(banner),
            showAddWalletBanner = true,
        )

        assertTrue(state.showAddWalletBanner)
        assertEquals(listOf(banner), state.dynamicBanners)
        assertEquals(1, state.cards.count { it == WalletHomeCardType.BANNER })
        assertTrue(state.isDynamicBannerLoaded)
    }

    @Test
    fun cachedStateKeepsAddWalletBannerWithoutDynamicBanners() {
        val state = cachedState().withDynamicBanners(
            dynamicBanners = emptyList(),
            showAddWalletBanner = true,
        )

        assertTrue(state.showAddWalletBanner)
        assertTrue(WalletHomeCardType.BANNER in state.cards)
        assertTrue(state.dynamicBanners.isEmpty())
    }

    @Test
    fun cachedStateRemovesBannerWhenNoBannerShouldBeShown() {
        val state = cachedState()
            .withDynamicBanners(
                dynamicBanners = listOf(WalletHomeBanner(bannerId = "banner-1")),
                showAddWalletBanner = true,
            )
            .withDynamicBanners(
                dynamicBanners = emptyList(),
                showAddWalletBanner = false,
            )

        assertFalse(state.showAddWalletBanner)
        assertFalse(WalletHomeCardType.BANNER in state.cards)
        assertTrue(state.dynamicBanners.isEmpty())
    }

    private fun cachedState(): WalletHomeState =
        WalletHomeCache(
            walletType = WalletHomeType.CLASSIC,
            fiatTotal = "0.00",
            btcTotal = "0.00",
            fiatSymbol = "\$",
            totalTokenCount = 1,
            totalTransactionCount = 0,
        ).toState()
}
