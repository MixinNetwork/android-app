package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.CashAccount
import one.mixin.android.extension.numberFormat2
import java.math.BigDecimal

data class WalletHomeCashAccount(
    val balanceUsd: BigDecimal,
) {
    val balanceAmountText: String
        get() = balanceUsd.numberFormat2()
}

internal fun CashAccount?.toWalletHomeCashAccount(): WalletHomeCashAccount? {
    val account = this ?: return null

    return WalletHomeCashAccount(
        balanceUsd = account.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    )
}

internal fun walletHomeCashBalanceUsd(
    account: WalletHomeCashAccount?,
): BigDecimal = account?.balanceUsd ?: BigDecimal.ZERO
