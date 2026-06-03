package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem

data class WalletHomeState(
    val walletType: WalletHomeType,
    val cards: List<WalletHomeCardType> = emptyList(),
    val isLoading: Boolean = false,
    val fiatTotal: String = "0.00",
    val btcTotal: String = "0.00",
    val fiatSymbol: String = "",
    val privacyTokens: List<TokenItem> = emptyList(),
    val web3Tokens: List<Web3TokenItem> = emptyList(),
    val privacyTransactions: List<SnapshotItem> = emptyList(),
    val web3Transactions: List<Web3TransactionItem> = emptyList(),
    val positions: List<PerpsPositionItem> = emptyList(),
    val totalTokenCount: Int = 0,
    val totalTransactionCount: Int = 0,
    val totalPositionCount: Int = 0,
    val topMovers: List<PerpsMarket> = emptyList(),
    val allTokensHidden: Boolean = false,
    val isWatchWallet: Boolean = false,
    val quoteColorReversed: Boolean = false,
    val showAddWalletBanner: Boolean = false,
    val showCashbackBanner: Boolean = false,
    val showReferralBanner: Boolean = false,
    val showBuyBadge: Boolean = false,
    val showSwapBadge: Boolean = false,
)

interface WalletHomeCallbacks {
    fun onAddWalletClicked()
    fun onBannerClosed()
    fun onCashbackBannerClosed()
    fun onReferralClicked()
    fun onReferralClosed()
    fun onSupportClicked()
    fun onHelpCenterClicked()
    fun onBuyClicked()
    fun onReceiveClicked()
    fun onSendClicked()
    fun onSwapClicked()
    fun onViewMoreTokensClicked()
    fun onAllTokensBackClicked()
    fun onViewMoreTransactionsClicked()
    fun onViewMorePositionsClicked()
    fun onTokenClicked(index: Int)
    fun onTransactionClicked(index: Int)
    fun onPositionClicked(index: Int)
    fun onTopMoverClicked(index: Int)
    fun onTopMoverMarketClicked(market: PerpsMarket) = Unit
    fun onViewMoreTopMoversClicked() = Unit
}
