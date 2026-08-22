package one.mixin.android.ui.wallet.home

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.CashAccount
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.extension.numberFormat2
import java.math.BigDecimal

data class WalletHomeCashAccount(
    @SerializedName("balanceUsd")
    val balanceUsd: BigDecimal = BigDecimal.ZERO,
    @SerializedName("rewardApy")
    val rewardApy: String? = null,
) {
    val balanceAmountText: String
        get() = balanceUsd.numberFormat2()

    val apyText: String?
        get() = cashAccountApyText(rewardApy)
}

internal fun CashAccount?.toWalletHomeCashAccount(): WalletHomeCashAccount? {
    val account = this ?: return null

    return WalletHomeCashAccount(
        balanceUsd = account.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        rewardApy = normalizeCashAccountRewardApy(account.rewardApy),
    )
}

internal fun normalizeCashAccountRewardApy(rewardApy: String?): String? =
    rewardApy
        ?.trim()
        ?.removeSuffix("%")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

internal fun cashAccountApyText(rewardApy: String?): String? =
    normalizeCashAccountRewardApy(rewardApy)?.let { "$it%" }

internal fun walletHomeCashBalanceUsd(
    account: WalletHomeCashAccount?,
): BigDecimal = account?.balanceUsd ?: BigDecimal.ZERO

internal fun WalletHomeState.withCashAccount(
    cashAccount: WalletHomeCashAccount,
): WalletHomeState {
    if (walletType != WalletHomeType.PRIVACY) return copy(cashAccount = cashAccount)

    val cashCards = if (cards.isEmpty()) {
        WalletHomeBuilder.build(
            walletType = walletType,
            hasAssetValue = false,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasCashAccount = true,
            hasTopMovers = false,
            hasTransactions = false,
            isLoading = false,
        )
    } else {
        cards.withCashCard()
    }
    val state = copy(
        cashAccount = cashAccount,
        cards = cashCards,
        isLoading = false,
    )
    return state.withWealthAccounts(state.wealthAccounts)
}

private fun List<WalletHomeCardType>.withCashCard(): List<WalletHomeCardType> {
    if (WalletHomeCardType.CASH in this) return this
    val balanceIndex = indexOf(WalletHomeCardType.BALANCE)
    if (balanceIndex == -1) return listOf(WalletHomeCardType.CASH) + this
    return take(balanceIndex + 1) + WalletHomeCardType.CASH + drop(balanceIndex + 1)
}

internal fun WalletHomeState.withDynamicBanners(
    dynamicBanners: List<WalletHomeBanner>,
    showAddWalletBanner: Boolean,
): WalletHomeState {
    val showBanner = showAddWalletBanner || dynamicBanners.isNotEmpty()
    val bannerCards = when {
        !showBanner -> cards - WalletHomeCardType.BANNER
        cards.isEmpty() -> WalletHomeBuilder.build(
            walletType = walletType,
            hasAssetValue = false,
            showBanner = true,
            showReferral = false,
            hasPositions = false,
            hasCashAccount = cashAccount != null,
            hasWealthAccount = wealthAccounts.isNotEmpty(),
            hasTopMovers = false,
            hasTransactions = false,
            hasImportKeyAction = importKeyAction != null,
            hasPendingIndicator = pendingIndicator != null,
            isWatchWallet = isWatchWallet,
            isLoading = false,
        )
        WalletHomeCardType.BANNER in cards -> cards
        else -> cards.withBannerCard()
    }
    val state = copy(
        cards = bannerCards,
        isLoading = if (showBanner) false else isLoading,
        showAddWalletBanner = showAddWalletBanner,
        dynamicBanners = dynamicBanners,
        isDynamicBannerLoaded = true,
    )
    return state.withWealthAccounts(state.wealthAccounts)
}

private fun List<WalletHomeCardType>.withBannerCard(): List<WalletHomeCardType> {
    val firstCardIndex = indexOfFirst {
        it == WalletHomeCardType.BALANCE || it == WalletHomeCardType.EMPTY_GUIDE
    }
    if (firstCardIndex == -1) return listOf(WalletHomeCardType.BANNER) + this
    return take(firstCardIndex + 1) + WalletHomeCardType.BANNER + drop(firstCardIndex + 1)
}
