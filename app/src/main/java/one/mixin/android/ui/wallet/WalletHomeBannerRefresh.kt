package one.mixin.android.ui.wallet

internal fun shouldApplyClassicWalletHomeBannerResponse(
    requestWalletId: String,
    currentWalletId: String,
): Boolean = requestWalletId == currentWalletId

internal fun shouldRefreshWalletHomeBannersAfterHiddenChanged(hidden: Boolean): Boolean = !hidden

internal fun shouldShowWalletHomeBannerCard(
    showAddWalletBanner: Boolean,
    isDynamicBannerLoaded: Boolean,
    hasVisibleDynamicBanners: Boolean,
): Boolean = showAddWalletBanner || (isDynamicBannerLoaded && hasVisibleDynamicBanners)
