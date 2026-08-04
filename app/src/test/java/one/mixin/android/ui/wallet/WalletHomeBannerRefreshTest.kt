package one.mixin.android.ui.wallet

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletHomeBannerRefreshTest {
    @Test
    fun classicBannerResponseOnlyAppliesToCurrentWallet() {
        assertTrue(shouldApplyClassicWalletHomeBannerResponse("wallet-1", "wallet-1"))
        assertFalse(shouldApplyClassicWalletHomeBannerResponse("wallet-1", "wallet-2"))
    }

    @Test
    fun walletFragmentOnlyRefreshesBannersWhenBecomingVisible() {
        assertFalse(shouldRefreshWalletHomeBannersAfterHiddenChanged(hidden = true))
        assertTrue(shouldRefreshWalletHomeBannersAfterHiddenChanged(hidden = false))
    }

    @Test
    fun walletHomeBannerCardKeepsAddWalletBannerBeforeDynamicLoadCompletes() {
        assertTrue(
            shouldShowWalletHomeBannerCard(
                showAddWalletBanner = true,
                isDynamicBannerLoaded = false,
                hasVisibleDynamicBanners = false,
            ),
        )
        assertFalse(
            shouldShowWalletHomeBannerCard(
                showAddWalletBanner = false,
                isDynamicBannerLoaded = false,
                hasVisibleDynamicBanners = true,
            ),
        )
        assertTrue(
            shouldShowWalletHomeBannerCard(
                showAddWalletBanner = false,
                isDynamicBannerLoaded = true,
                hasVisibleDynamicBanners = true,
            ),
        )
    }
}
