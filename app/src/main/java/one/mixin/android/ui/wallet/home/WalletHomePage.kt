package one.mixin.android.ui.wallet.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.ui.wallet.home.WalletHomeSection.PREVIEW_LIMIT

@Composable
fun WalletHomePage(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.backgroundWindow)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            state.cards.forEach { card ->
                when (card) {
                    WalletHomeCardType.BANNER -> BannerPager(state, callbacks)
                    WalletHomeCardType.SUPPORT -> SupportCard(callbacks)
                    else -> WalletHomeCard(card, state, callbacks)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            ImportSafetyFooter(state.walletType)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun WalletHomeAllTokensPage(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.backgroundWindow)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        WalletHomeCard(WalletHomeCardType.BALANCE, state, callbacks)
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { callbacks.onAllTokensBackClicked() }
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.wallet_home_tokens),
                        color = MixinAppTheme.colors.textMinor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (state.allTokensHidden) {
                Text(
                    text = stringResource(R.string.wallet_home_all_assets_hidden),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                )
            } else if (state.walletType == WalletHomeType.PRIVACY) {
                PrivacyTokenRecycler(
                    tokens = state.privacyTokens,
                    onClick = callbacks::onTokenClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(allTokenListHeight(state.privacyTokens.size)),
                )
            } else {
                Web3TokenRecycler(
                    tokens = state.web3Tokens,
                    onClick = callbacks::onTokenClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(allTokenListHeight(state.web3Tokens.size)),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun WalletHomeCard(
    card: WalletHomeCardType,
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    val contentPadding = if (card.hasSelfPaddedItems()) Modifier else Modifier.padding(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(positionListHeight(state.positions.take(PREVIEW_LIMIT).size)),
                )
            }
            WalletHomeCardType.TOP_MOVERS -> SectionCard(
                title = stringResource(R.string.wallet_home_top_movers),
                showViewAll = false,
                onClick = { callbacks.onTopMoverClicked(0) },
            ) {
                TopMoverRows(state.topMovers.take(PREVIEW_LIMIT), callbacks::onTopMoverClicked)
            }
            WalletHomeCardType.TOKENS -> SectionCard(
                title = stringResource(R.string.wallet_home_tokens),
                showViewAll = WalletHomeSection.hasMore(state.totalTokenCount),
                onClick = callbacks::onViewMoreTokensClicked,
                contentUsesOwnPadding = true,
            ) {
                if (state.allTokensHidden) {
                    Text(
                        text = stringResource(R.string.wallet_home_all_assets_hidden),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                    )
                } else {
                    if (state.walletType == WalletHomeType.PRIVACY) {
                        PrivacyTokenRecycler(
                            tokens = state.privacyTokens.take(PREVIEW_LIMIT),
                            onClick = callbacks::onTokenClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tokenListHeight(state.privacyTokens.take(PREVIEW_LIMIT).size)),
                        )
                    } else {
                        Web3TokenRecycler(
                            tokens = state.web3Tokens.take(PREVIEW_LIMIT),
                            onClick = callbacks::onTokenClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tokenListHeight(state.web3Tokens.take(PREVIEW_LIMIT).size)),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(transactionListHeight(state.privacyTransactions.take(PREVIEW_LIMIT).size)),
                    )
                } else {
                    Web3TransactionRecycler(
                        transactions = state.web3Transactions.take(PREVIEW_LIMIT),
                        onClick = callbacks::onTransactionClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(transactionListHeight(state.web3Transactions.take(PREVIEW_LIMIT).size)),
                    )
                }
            }
            WalletHomeCardType.REFERRAL -> ReferralBannerCard(callbacks)
            WalletHomeCardType.SUPPORT -> Unit
        }
    }
}

private fun WalletHomeCardType.hasSelfPaddedItems(): Boolean =
    this == WalletHomeCardType.POSITIONS ||
        this == WalletHomeCardType.TOP_MOVERS ||
        this == WalletHomeCardType.TOKENS ||
        this == WalletHomeCardType.TRANSACTIONS ||
        this == WalletHomeCardType.REFERRAL

@Composable
private fun EmptyGuideCard(callbacks: WalletHomeCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { callbacks.onReceiveClicked() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_wallet_home_fund),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.wallet_home_empty_title),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.wallet_home_empty_desc),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WalletHomeButton(
                textRes = R.string.wallet_home_buy_crypto,
                onClick = callbacks::onBuyClicked,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
            WalletHomeButton(
                textRes = R.string.wallet_home_receive_crypto,
                onClick = callbacks::onReceiveClicked,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WalletHomeButton(
    textRes: Int,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow,
                shape = RoundedCornerShape(24.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
private fun BalanceCard(state: WalletHomeState, callbacks: WalletHomeCallbacks) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.Total_Balance),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${state.fiatSymbol}${state.fiatTotal}",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.W600,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${state.btcTotal} BTC",
            color = MixinAppTheme.colors.textAssist,
            fontSize = 16.sp,
        )
        val showActions = !(state.walletType == WalletHomeType.CLASSIC && state.isWatchWallet)
        if (showActions) {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ActionItem(
                    iconRes = R.drawable.ic_wallet_buy,
                    labelRes = R.string.Buy,
                    showBadge = state.showBuyBadge,
                    onClick = callbacks::onBuyClicked,
                )
                ActionItem(
                    iconRes = R.drawable.ic_wallet_receive,
                    labelRes = R.string.Receive,
                    showBadge = false,
                    onClick = callbacks::onReceiveClicked,
                )
                ActionItem(
                    iconRes = R.drawable.ic_wallet_send,
                    labelRes = R.string.Send_transfer,
                    showBadge = false,
                    onClick = callbacks::onSendClicked,
                )
                ActionItem(
                    iconRes = R.drawable.ic_wallet_swap,
                    labelRes = R.string.Trade,
                    showBadge = state.showSwapBadge,
                    onClick = callbacks::onSwapClicked,
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    iconRes: Int,
    labelRes: Int,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MixinAppTheme.colors.tipError),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(labelRes),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerPager(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    val pages = remember(state.showAddWalletBanner, state.showCashbackBanner) {
        buildList {
            if (state.showAddWalletBanner) add(WalletHomeBannerPage.ADD_WALLET)
            if (state.showCashbackBanner) add(WalletHomeBannerPage.CASHBACK)
        }
    }
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    when (pages[page]) {
                        WalletHomeBannerPage.ADD_WALLET -> BannerCard(
                            iconRes = R.drawable.ic_wallet_home_add,
                            titleRes = R.string.wallet_home_add_wallet_banner_title,
                            descriptionRes = null,
                            ctaRes = R.string.wallet_home_add_wallet_banner_cta,
                            onClick = callbacks::onAddWalletClicked,
                        )
                        WalletHomeBannerPage.CASHBACK -> BannerCard(
                            iconRes = R.drawable.ic_wallet_home_buy,
                            titleRes = R.string.wallet_home_cashback_banner_title,
                            descriptionRes = null,
                            ctaRes = R.string.wallet_home_cashback_banner_cta,
                            onClick = callbacks::onBuyClicked,
                        )
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_close_grey),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(12.dp)
                    .clickable {
                        when (pages.getOrNull(pagerState.currentPage)) {
                            WalletHomeBannerPage.ADD_WALLET -> callbacks.onBannerClosed()
                            WalletHomeBannerPage.CASHBACK -> callbacks.onCashbackBannerClosed()
                            null -> Unit
                        }
                    },
            )
        }
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MixinAppTheme.colors.accent
                                } else {
                                    MixinAppTheme.colors.borderColor
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerCard(
    iconRes: Int,
    titleRes: Int,
    descriptionRes: Int?,
    ctaRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
            )
            if (descriptionRes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(descriptionRes),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            BannerAction(textRes = ctaRes)
        }
    }
}

@Composable
private fun ReferralBannerCard(callbacks: WalletHomeCallbacks) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_wallet_reffal),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
        )
        Icon(
            painter = painterResource(R.drawable.ic_close_grey),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(12.dp)
                .clickable { callbacks.onReferralClosed() },
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .padding(top = 30.dp, bottom = 20.dp)
                .clickable { callbacks.onReferralClicked() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_wallet_home_referral),
                contentDescription = null,
                modifier = Modifier.size(70.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.Referral),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val description = stringResource(R.string.wallet_home_referral_banner_desc)
            val highlight = "60%"
            Text(
                text = buildAnnotatedString {
                    val start = description.indexOf(highlight)
                    if (start >= 0) {
                        append(description.substring(0, start))
                        withStyle(SpanStyle(color = Color(0xFF7B61FF), fontWeight = FontWeight.W600)) {
                            append(highlight)
                        }
                        append(description.substring(start + highlight.length))
                    } else {
                        append(description)
                    }
                },
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            BannerAction(textRes = R.string.Learn_More, primary = true)
        }
    }
}

@Composable
private fun BannerAction(textRes: Int, primary: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                color = if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class WalletHomeBannerPage {
    ADD_WALLET,
    CASHBACK,
}

@Composable
private fun SectionCard(
    title: String,
    showViewAll: Boolean,
    onClick: () -> Unit,
    contentUsesOwnPadding: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MixinAppTheme.colors.textAssist,
            modifier = Modifier.size(16.dp),
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    if (contentUsesOwnPadding) {
        content()
    } else {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            content()
        }
    }
    if (showViewAll) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.view_all),
                color = MixinAppTheme.colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
            )
        }
    } else {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun tokenListHeight(count: Int) = (84 * WalletHomeSection.previewCount(count)).dp

private fun allTokenListHeight(count: Int) = (84 * count).dp

private fun transactionListHeight(count: Int) = (62 * WalletHomeSection.previewCount(count)).dp

private fun positionListHeight(count: Int) = (82 * WalletHomeSection.previewCount(count)).dp

@Composable
private fun TopMoverRows(
    topMovers: List<TopAssetItem>,
    onClick: (Int) -> Unit,
) {
    topMovers.forEachIndexed { index, item ->
        val pct = item.changeUsd.toDoubleOrNull() ?: 0.0
        val pctText = "${if (pct >= 0) "+" else ""}${String.format("%.2f", pct * 100)}%"
        val pctColor = if (pct >= 0) Color(0xFF00C087) else MixinAppTheme.colors.tipError
        val priceText = String.format("%.2f", item.priceFiat())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onClick(index) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.symbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = priceText,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                )
                Text(
                    text = pctText,
                    color = pctColor,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SupportCard(callbacks: WalletHomeCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
    ) {
        Text(
            text = stringResource(R.string.wallet_home_support),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SupportRow(
            iconRes = R.drawable.ic_help_outline,
            titleRes = R.string.wallet_home_contact_us,
            subtitleRes = R.string.wallet_home_contact_us_desc,
            trailingRes = R.drawable.ic_arrow_gray_right,
            onClick = callbacks::onSupportClicked,
        )
        SupportRow(
            iconRes = R.drawable.ic_bot_category_books,
            titleRes = R.string.wallet_home_help_center,
            subtitleRes = R.string.wallet_home_help_center_desc,
            trailingRes = R.drawable.ic_wallet_home_external_link,
            onClick = callbacks::onHelpCenterClicked,
        )
    }
}

@Composable
private fun SupportRow(
    iconRes: Int,
    titleRes: Int,
    subtitleRes: Int,
    trailingRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitleRes),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 13.sp,
            )
        }
        Icon(
            painter = painterResource(trailingRes),
            contentDescription = null,
            tint = MixinAppTheme.colors.textAssist,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ImportSafetyFooter(walletType: WalletHomeType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        val titleRes: Int
        val bulletRes: List<Int>
        if (walletType == WalletHomeType.PRIVACY) {
            titleRes = R.string.wallet_home_privacy_wallet_reason_title
            bulletRes = listOf(
                R.string.wallet_home_privacy_wallet_reason_1,
                R.string.wallet_home_privacy_wallet_reason_2,
                R.string.wallet_home_privacy_wallet_reason_3,
            )
        } else {
            titleRes = R.string.wallet_home_common_wallet_reason_title
            bulletRes = listOf(
                R.string.wallet_home_common_wallet_reason_1,
                R.string.wallet_home_common_wallet_reason_2,
                R.string.wallet_home_common_wallet_reason_3,
            )
        }
        ImportSafetySection(titleRes = titleRes, bulletRes = bulletRes)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val previews = listOf(
                R.drawable.mixin_import_safety_preview_0,
                R.drawable.mixin_import_safety_preview_1,
                R.drawable.mixin_import_safety_preview_2,
                R.drawable.mixin_import_safety_preview_3,
                R.drawable.mixin_import_safety_preview_4,
                R.drawable.mixin_import_safety_preview_5,
                R.drawable.mixin_import_safety_preview_6,
            )
            previews.forEachIndexed { index, res ->
                if (index != 0) Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun ImportSafetySection(
    titleRes: Int,
    bulletRes: List<Int>,
) {
    Text(
        text = stringResource(titleRes),
        color = MixinAppTheme.colors.textAssist,
        fontSize = 14.sp,
        fontWeight = FontWeight.W600,
    )
    bulletRes.forEachIndexed { index, res ->
        ImportSafetyBullet(res, topPadding = if (index == 0) 12.dp else 8.dp)
    }
}

@Composable
private fun ImportSafetyBullet(textRes: Int, topPadding: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.textAssist),
        )
        Text(
            text = stringResource(textRes),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
    }
}
