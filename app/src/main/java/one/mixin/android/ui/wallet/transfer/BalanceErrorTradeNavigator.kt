package one.mixin.android.ui.wallet.transfer

import android.content.Context
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.TradeFragment

object BalanceErrorTradeNavigator {
    fun showSwapTrade(
        context: Context,
        input: String?,
        output: String?,
        amount: String? = null,
        referral: String? = null,
        inMixin: Boolean = true,
        walletId: String? = null,
    ) {
        SwapActivity.show(
            context = context,
            input = input,
            output = output,
            amount = amount,
            referral = referral,
            inMixin = inMixin,
            walletId = walletId,
            initialTab = TradeFragment.TAB_SIMPLE,
        )
    }
}
