package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.perps.TopMoversCard
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.PositionRecycler
import one.mixin.android.ui.wallet.home.PrivacyTokenRecycler
import one.mixin.android.ui.wallet.home.PrivacyTransactionRecycler
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomeSection
import one.mixin.android.ui.wallet.home.WalletHomeSection.PREVIEW_LIMIT
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.Web3TokenRecycler
import one.mixin.android.ui.wallet.home.Web3TransactionRecycler

@Composable
internal fun WalletHomeCard(
    card: WalletHomeCardType,
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    state.importKeyAction?.let { importKeyAction ->
        if (card == WalletHomeCardType.BALANCE) {
            BalanceCardGroup(state, importKeyAction, callbacks)
            return
        }
    }

    val contentPadding = if (card.hasSelfPaddedItems()) Modifier else Modifier.padding(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .then(contentPadding),
    ) {
        when (card) {
            WalletHomeCardType.EMPTY_GUIDE -> EmptyGuideCard(callbacks)
            WalletHomeCardType.BALANCE -> BalanceCard(state, callbacks)
            WalletHomeCardType.BANNER -> Unit
            WalletHomeCardType.POSITIONS -> SectionCard(
                title = stringResource(R.string.wallet_home_positions),
                showViewAll = WalletHomeSection.hasMore(state.totalPositionCount),
                onClick = callbacks::onViewMorePositionsClicked,
                contentUsesOwnPadding = true,
            ) {
                PositionRecycler(
                    positions = state.positions.take(PREVIEW_LIMIT),
                    onClick = callbacks::onPositionClicked,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            WalletHomeCardType.TOP_MOVERS -> {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    TopMoversCard(
                        markets = state.topMovers,
                        quoteColorReversed = state.quoteColorReversed,
                        onViewAllClick = callbacks::onViewMoreTopMoversClicked,
                        onMarketItemClick = callbacks::onTopMoverMarketClicked,
                    )
                }
            }
            WalletHomeCardType.TOKENS -> SectionCard(
                title = stringResource(R.string.wallet_home_tokens),
                showViewAll = WalletHomeSection.hasMore(state.totalTokenCount),
                onClick = callbacks::onViewMoreTokensClicked,
                contentUsesOwnPadding = true,
            ) {
                if (!state.allTokensHidden) {
                    if (state.walletType == WalletHomeType.PRIVACY) {
                        PrivacyTokenRecycler(
                            tokens = state.privacyTokens.take(PREVIEW_LIMIT),
                            onClick = callbacks::onTokenClicked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Web3TokenRecycler(
                            tokens = state.web3Tokens.take(PREVIEW_LIMIT),
                            onClick = callbacks::onTokenClicked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            WalletHomeCardType.TRANSACTIONS -> SectionCard(
                title = stringResource(R.string.wallet_home_transactions),
                showViewAll = WalletHomeSection.hasMore(state.totalTransactionCount),
                onClick = callbacks::onViewMoreTransactionsClicked,
                contentUsesOwnPadding = true,
            ) {
                if (state.walletType == WalletHomeType.PRIVACY) {
                    PrivacyTransactionRecycler(
                        transactions = state.privacyTransactions.take(PREVIEW_LIMIT),
                        onClick = callbacks::onTransactionClicked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Web3TransactionRecycler(
                        transactions = state.web3Transactions.take(PREVIEW_LIMIT),
                        onClick = callbacks::onTransactionClicked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            WalletHomeCardType.REFERRAL -> ReferralBannerCard(callbacks)
            WalletHomeCardType.SUPPORT -> Unit
        }
    }
}

@Composable
private fun BalanceCardGroup(
    state: WalletHomeState,
    importKeyAction: WalletHomeImportKeyAction,
    callbacks: WalletHomeCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                .padding(20.dp),
        ) {
            BalanceCard(state, callbacks)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HighlightedTextWithClick(
            fullText = stringResource(
                importKeyAction.descriptionRes,
                stringResource(R.string.Learn_More),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            stringResource(R.string.Learn_More),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Start,
        ) {
            callbacks.onImportKeyLearnMoreClicked()
        }
    }
}

private fun WalletHomeCardType.hasSelfPaddedItems(): Boolean =
    this == WalletHomeCardType.POSITIONS ||
        this == WalletHomeCardType.TOP_MOVERS ||
        this == WalletHomeCardType.TOKENS ||
        this == WalletHomeCardType.TRANSACTIONS ||
        this == WalletHomeCardType.REFERRAL
