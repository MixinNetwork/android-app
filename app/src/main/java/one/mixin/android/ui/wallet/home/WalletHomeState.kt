package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.R
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.WalletHomeBannerAction
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.numberFormat8
import one.mixin.android.vo.PendingDisplay
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

data class WalletHomeState(
    val walletType: WalletHomeType,
    val cards: List<WalletHomeCardType> = emptyList(),
    val isLoading: Boolean = false,
    val fiatTotal: String = "0.00",
    val tokenFiatTotal: String? = null,
    val btcTotal: String = "0.00",
    val fiatSymbol: String = "",
    val privacyTokens: List<TokenItem> = emptyList(),
    val web3Tokens: List<Web3TokenItem> = emptyList(),
    val privacyTransactions: List<SnapshotItem> = emptyList(),
    val web3Transactions: List<Web3TransactionItem> = emptyList(),
    val positions: List<PerpsPositionItem> = emptyList(),
    val positionSummary: WalletHomePositionSummary? = null,
    val cashAccount: WalletHomeCashAccount? = null,
    val totalTokenCount: Int = 0,
    val totalTransactionCount: Int = 0,
    val totalPositionCount: Int = 0,
    val topMovers: List<PerpsMarket> = emptyList(),
    val allTokensHidden: Boolean = false,
    val isWatchWallet: Boolean = false,
    val pendingIndicator: WalletHomePendingIndicator? = null,
    val watchIndicator: WalletHomeWatchIndicator? = null,
    val importKeyAction: WalletHomeImportKeyAction? = null,
    val hideActions: Boolean = false,
    val quoteColorReversed: Boolean = false,
    val showAddWalletBanner: Boolean = false,
    val isDynamicBannerLoaded: Boolean = false,
    val dynamicBanners: List<WalletHomeBanner> = emptyList(),
    val showReferralBanner: Boolean = false,
    val showBuyBadge: Boolean = false,
    val showSwapBadge: Boolean = false,
    val showImportSafetyFooter: Boolean = true,
)

data class WalletHomePositionSummary(
    val valueText: String,
    val pnlText: String,
    val isProfit: Boolean?,
)

data class WalletHomeBalanceSnapshot(
    val fiatTotal: String,
    val tokenFiatTotal: String?,
    val btcTotal: String,
    val fiatSymbol: String,
    val totalTokenCount: Int,
)

interface WalletHomeCallbacks {
    fun onAddWalletClicked()
    fun onBannerClosed()
    fun onDynamicBannerClicked(banner: WalletHomeBanner) = Unit
    fun onDynamicBannerActionClicked(banner: WalletHomeBanner, action: WalletHomeBannerAction) = Unit
    fun onDynamicBannerClosed(banner: WalletHomeBanner) = Unit
    fun onReferralClicked()
    fun onReferralClosed()
    fun onCashClicked()
    fun onSupportClicked()
    fun onHelpCenterClicked()
    fun onBuyClicked()
    fun onReceiveClicked()
    fun onSendClicked()
    fun onSwapClicked()
    fun onPendingIndicatorClicked()
    fun onWatchIndicatorClicked()
    fun onImportKeyClicked()
    fun onImportKeyLearnMoreClicked()
    fun onViewMoreTokensClicked()
    fun onAllTokensBackClicked()
    fun onViewMoreTransactionsClicked()
    fun onViewMorePositionsClicked()
    fun onTokenClicked(index: Int)
    fun onTransactionClicked(index: Int)
    fun onTransactionUserClicked(userId: String)
    fun onPositionClicked(index: Int)
    fun onTopMoverClicked(index: Int)
    fun onTopMoverMarketClicked(market: PerpsMarket) = Unit
    fun onViewMoreTopMoversClicked() = Unit
}

data class WalletHomePendingIndicator(
    val kind: WalletHomePendingKind,
    val value: String,
    val iconUrls: List<String?>,
    val singleAssetId: String? = null,
)

enum class WalletHomePendingKind {
    SINGLE_DEPOSIT,
    MULTIPLE_DEPOSITS,
    SINGLE_TRANSACTION,
    MULTIPLE_TRANSACTIONS,
}

data class WalletHomeWatchIndicator(
    val kind: WalletHomeWatchKind,
    val value: String,
)

enum class WalletHomeWatchKind {
    SINGLE_ADDRESS,
    MULTIPLE_ADDRESSES,
}

data class WalletHomeImportKeyAction(
    val kind: WalletHomeImportKeyKind,
    val buttonTextRes: Int,
    val descriptionRes: Int,
    val learnMoreUrlRes: Int,
)

enum class WalletHomeImportKeyKind {
    MNEMONIC_PHRASE,
    PRIVATE_KEY,
}

fun List<PendingDisplay>.toWalletHomePendingIndicator(): WalletHomePendingIndicator? {
    if (isEmpty()) return null
    return if (size == 1) {
        val pending = first()
        WalletHomePendingIndicator(
            kind = WalletHomePendingKind.SINGLE_DEPOSIT,
            value = "${pending.amount} ${pending.symbol}",
            iconUrls = listOf(pending.iconUrl),
            singleAssetId = pending.assetId,
        )
    } else {
        WalletHomePendingIndicator(
            kind = WalletHomePendingKind.MULTIPLE_DEPOSITS,
            value = size.toString(),
            iconUrls = take(2).map { it.iconUrl },
        )
    }
}

fun formatWalletHomeBtcTotal(totalBtc: BigDecimal): String {
    val formatted = totalBtc.numberFormat8()
    return try {
        if (formatted.toFloat() == 0f) {
            "0.00"
        } else {
            formatted
        }
    } catch (ignored: NumberFormatException) {
        formatted
    }
}

fun walletHomePendingTransactionIndicator(count: Int): WalletHomePendingIndicator? {
    if (count <= 0) return null
    return WalletHomePendingIndicator(
        kind = if (count == 1) WalletHomePendingKind.SINGLE_TRANSACTION else WalletHomePendingKind.MULTIPLE_TRANSACTIONS,
        value = count.toString(),
        iconUrls = emptyList(),
    )
}

fun walletHomePendingTransactionCount(
    rawPendingCount: Int,
    transactionPendingCount: Int,
): Int = maxOf(rawPendingCount, transactionPendingCount)

fun walletHomeWatchIndicator(addresses: List<String>): WalletHomeWatchIndicator? {
    if (addresses.isEmpty()) return null
    return if (addresses.size == 1) {
        val address = addresses.first()
        WalletHomeWatchIndicator(
            kind = WalletHomeWatchKind.SINGLE_ADDRESS,
            value = "${address.take(6)}..${address.takeLast(4)}",
        )
    } else {
        WalletHomeWatchIndicator(
            kind = WalletHomeWatchKind.MULTIPLE_ADDRESSES,
            value = addresses.size.toString(),
        )
    }
}

fun walletHomeImportKeyAction(
    category: String,
    hasLocalPrivateKey: Boolean,
): WalletHomeImportKeyAction? {
    if (hasLocalPrivateKey) return null
    return when (category) {
        WalletCategory.IMPORTED_MNEMONIC.value -> WalletHomeImportKeyAction(
            kind = WalletHomeImportKeyKind.MNEMONIC_PHRASE,
            buttonTextRes = R.string.import_mnemonic_phrase,
            descriptionRes = R.string.Import_Mnemonic_Phrase_Desc,
            learnMoreUrlRes = R.string.import_mnemonic_phrase_url,
        )
        WalletCategory.IMPORTED_PRIVATE_KEY.value -> WalletHomeImportKeyAction(
            kind = WalletHomeImportKeyKind.PRIVATE_KEY,
            buttonTextRes = R.string.import_private_key,
            descriptionRes = R.string.Import_Private_Key_Desc,
            learnMoreUrlRes = R.string.import_private_key_url,
        )
        else -> null
    }
}
