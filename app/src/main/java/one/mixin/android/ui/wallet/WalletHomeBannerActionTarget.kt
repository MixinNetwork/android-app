package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import one.mixin.android.extension.SpotTradeAction
import one.mixin.android.extension.toPerpsTradeAction
import one.mixin.android.extension.toSpotTradeAction

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
    data object PerpsTab : WalletHomeBannerActionTarget
    data object Buy : WalletHomeBannerActionTarget
    data class Web(val url: String) : WalletHomeBannerActionTarget
}

internal fun String.toClassicWalletHomeBannerActionTarget(): WalletHomeBannerActionTarget {
    toSpotTradeAction()?.let { return WalletHomeBannerActionTarget.SpotTrade(it) }
    toPerpsTradeAction()?.let { action ->
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
