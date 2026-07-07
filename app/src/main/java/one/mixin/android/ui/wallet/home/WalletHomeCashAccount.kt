package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.CashAccount
import one.mixin.android.extension.numberFormat2
import java.math.BigDecimal

data class WalletHomeCashAccount(
    val balanceUsd: BigDecimal,
    val rewardApy: String?,
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
