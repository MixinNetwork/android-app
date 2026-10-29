package one.mixin.android.ui.wallet

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import one.mixin.android.Constants
import one.mixin.android.extension.SpotTradeAction
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.putInt
import one.mixin.android.extension.toPerpsTradeAction
import one.mixin.android.extension.toSpotTradeAction
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet

internal sealed interface WalletHomeBannerActionTarget {
    data class SpotTrade(val action: SpotTradeAction) : WalletHomeBannerActionTarget
    data class PerpsMarket(
        val marketId: String,
        val leaderPositionId: String? = null,
    ) : WalletHomeBannerActionTarget
    data class PerpsOpen(
        val marketId: String,
        val isLong: Boolean?,
        val leverage: Int?,
        val margin: String?,
        val leaderPositionId: String?,
    ) : WalletHomeBannerActionTarget
    data class PerpsAddMargin(val marketId: String, val margin: String?) : WalletHomeBannerActionTarget
    data object PerpsTab : WalletHomeBannerActionTarget
    data object Buy : WalletHomeBannerActionTarget
    data class Web(val url: String) : WalletHomeBannerActionTarget
}

internal fun String.toClassicWalletHomeBannerActionTarget(): WalletHomeBannerActionTarget {
    toSpotTradeAction()?.let { return WalletHomeBannerActionTarget.SpotTrade(it) }
    toPerpsTradeAction()?.let { action ->
        if (action.addMargin != null && action.marketId != null) {
            return WalletHomeBannerActionTarget.PerpsAddMargin(action.marketId, action.addMargin.margin)
        }
        return when (val marketId = action.marketId) {
            null -> WalletHomeBannerActionTarget.PerpsTab
            else -> action.openPosition?.let { openPosition ->
                WalletHomeBannerActionTarget.PerpsOpen(
                    marketId = marketId,
                    isLong = openPosition.isLong,
                    leverage = openPosition.leverage,
                    margin = openPosition.margin,
                    leaderPositionId = action.leaderPositionId,
                )
            } ?: WalletHomeBannerActionTarget.PerpsMarket(marketId, action.leaderPositionId)
        }
    }
    return if (isBuyAction()) {
        WalletHomeBannerActionTarget.Buy
    } else {
        WalletHomeBannerActionTarget.Web(this)
    }
}

private fun String.isBuyAction(): Boolean =
    startsWith(Constants.Scheme.BUY, true) ||
        startsWith(Constants.Scheme.MIXIN_BUY, true) ||
        startsWith(Constants.Scheme.HTTPS_BUY, true)

internal fun Fragment.openWalletHomeBannerAction(
    url: String,
    tradeSource: String,
    perpsSource: String,
    onBuy: () -> Unit,
) {
    when (val target = url.toClassicWalletHomeBannerActionTarget()) {
        is WalletHomeBannerActionTarget.SpotTrade -> {
            val tab = if (target.action.openLimit) TradeFragment.TAB_ADVANCED else TradeFragment.TAB_SIMPLE
            AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, tradeSource)
            defaultSharedPreferences.putInt("${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}${Session.getAccountId().orEmpty()}", tab)
            SwapActivity.show(
                requireActivity(),
                target.action.input,
                target.action.output,
                target.action.amount,
                target.action.referral,
                entrySource = tradeSource,
                entryType = if (target.action.openLimit) {
                    AnalyticsTracker.SpotTradeType.ADVANCED
                } else {
                    AnalyticsTracker.SpotTradeType.SIMPLE
                },
                initialTab = tab,
            )
        }
        is WalletHomeBannerActionTarget.PerpsMarket -> {
            PerpsActivity.showDetail(
                requireActivity(),
                target.marketId,
                "",
                "",
                "",
                perpsSource,
                leaderPositionId = target.leaderPositionId,
            )
        }
        is WalletHomeBannerActionTarget.PerpsOpen,
        is WalletHomeBannerActionTarget.PerpsAddMargin -> {
            LinkBottomSheetDialogFragment.newInstance(
                url,
                tradeSource = perpsSource,
            ).showNow(parentFragmentManager, LinkBottomSheetDialogFragment.TAG)
        }
        WalletHomeBannerActionTarget.PerpsTab -> {
            AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, tradeSource)
            defaultSharedPreferences.putInt("${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}${Session.getAccountId().orEmpty()}", TradeFragment.TAB_PERPETUAL)
            SwapActivity.show(
                requireActivity(),
                entrySource = tradeSource,
                entryType = AnalyticsTracker.SpotTradeType.PERPETUAL,
                initialTab = TradeFragment.TAB_PERPETUAL,
            )
        }
        WalletHomeBannerActionTarget.Buy -> {
            onBuy()
        }
        is WalletHomeBannerActionTarget.Web -> {
            target.url.openAsUrlOrWeb(requireActivity(), null, parentFragmentManager, lifecycleScope)
        }
    }
}
