package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import one.mixin.android.ui.wallet.home.WalletHomeCashAccount
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomePositionSummary
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
    if (card == WalletHomeCardType.CASH && state.cashAccount == null) return

    val contentPadding = when {
        card.hasSelfPaddedItems() -> Modifier
        card == WalletHomeCardType.BALANCE -> Modifier.padding(top = 20.dp, bottom = 12.dp)
        else -> Modifier.padding(20.dp)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .then(
                if (card == WalletHomeCardType.CASH && state.cashAccount != null) {
                    Modifier.clickable { callbacks.onCashClicked() }
                } else {
                    Modifier
                },
            )
            .then(contentPadding),
    ) {
        when (card) {
            WalletHomeCardType.EMPTY_GUIDE -> EmptyGuideCard(callbacks)
            WalletHomeCardType.BALANCE -> BalanceCard(
                state,
                callbacks,
                contentHorizontalPadding = 20.dp,
            )
            WalletHomeCardType.BANNER -> Unit
            WalletHomeCardType.CASH -> CashAccountCard(
                cashAccount = state.cashAccount,
                callbacks = callbacks,
            )
            WalletHomeCardType.POSITIONS -> SectionCard(
                title = stringResource(R.string.positions_count, state.totalPositionCount),
                showViewAll = WalletHomeSection.hasMore(state.totalPositionCount),
                onClick = callbacks::onViewMorePositionsClicked,
                contentUsesOwnPadding = true,
                headerTrailing = {
                    PositionSummaryHeader(
                        summary = state.positionSummary,
                    )
                },
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
                        titleRes = R.string.Perpetual,
                    )
                }
            }
            WalletHomeCardType.TOKENS -> {
                val privacyTokens = state.privacyTokens.filter { it.hidden != true }
                val web3Tokens = state.web3Tokens.filter { it.hidden != true }
                val hasTokens = !state.allTokensHidden && (
                    if (state.walletType == WalletHomeType.PRIVACY) {
                        privacyTokens.isNotEmpty()
                    } else {
                        web3Tokens.isNotEmpty()
                    }
                )
                SectionCard(
                    title = stringResource(R.string.wallet_home_tokens),
                    showViewAll = WalletHomeSection.hasMore(state.totalTokenCount),
                    onClick = callbacks::onViewMoreTokensClicked,
                    contentUsesOwnPadding = true,
                    showBottomSpacer = hasTokens,
                    headerTrailing = {
                        TokenBalanceHeader("${state.fiatSymbol}${state.tokenFiatTotal ?: state.fiatTotal}")
                    },
                ) {
                    if (!state.allTokensHidden) {
                        if (state.walletType == WalletHomeType.PRIVACY) {
                            PrivacyTokenRecycler(
                                tokens = privacyTokens.take(PREVIEW_LIMIT),
                                onClick = callbacks::onTokenClicked,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Web3TokenRecycler(
                                tokens = web3Tokens.take(PREVIEW_LIMIT),
                                onClick = callbacks::onTokenClicked,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            WalletHomeCardType.TRANSACTIONS -> SectionCard(
                title = stringResource(R.string.Transactions),
                showViewAll = WalletHomeSection.hasMore(state.totalTransactionCount),
                onClick = callbacks::onViewMoreTransactionsClicked,
                contentUsesOwnPadding = true,
            ) {
                if (state.walletType == WalletHomeType.PRIVACY) {
                    PrivacyTransactionRecycler(
                        transactions = state.privacyTransactions.take(PREVIEW_LIMIT),
                        onClick = callbacks::onTransactionClicked,
                        onUserClick = callbacks::onTransactionUserClicked,
                        contentHorizontalPadding = 20.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Web3TransactionRecycler(
                        transactions = state.web3Transactions.take(PREVIEW_LIMIT),
                        onClick = callbacks::onTransactionClicked,
                        contentHorizontalPadding = 20.dp,
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
private fun CashAccountCard(
    cashAccount: WalletHomeCashAccount?,
    callbacks: WalletHomeCallbacks,
) {
    if (cashAccount == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_wallet_home_cash),
            contentDescription = null,
            modifier = Modifier.size(42.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cash_balance),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.W400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_gray_right),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp).offset(x = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.W600)) {
                            append(cashAccount.balanceAmountText)
                        }
                        append(" ")
                        withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.W500)) {
                            append("USD")
                        }
                    },
                    color = MixinAppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                cashAccount.apyText?.let { apyText ->
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = apyText,
                        color = Color(0xFF5ECF72),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.W400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionSummaryHeader(
    summary: WalletHomePositionSummary?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (summary != null) {
            Text(
                text = summary.valueText,
                color = MixinAppTheme.colors.textMinor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 190.dp).offset(x = 4.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_gray_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp).offset(x = 4.dp),
        )
    }
}

@Composable
private fun TokenBalanceHeader(balanceText: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = balanceText,
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 190.dp).offset(x = 4.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_gray_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp).offset(x = 4.dp),
        )
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
        Spacer(modifier = Modifier.height(10.dp))
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
            lineHeight = 12.sp,
            textAlign = TextAlign.Start,
        ) {
            callbacks.onImportKeyLearnMoreClicked()
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

private fun WalletHomeCardType.hasSelfPaddedItems(): Boolean =
    this == WalletHomeCardType.POSITIONS ||
        this == WalletHomeCardType.TOP_MOVERS ||
        this == WalletHomeCardType.TOKENS ||
        this == WalletHomeCardType.TRANSACTIONS ||
        this == WalletHomeCardType.REFERRAL
