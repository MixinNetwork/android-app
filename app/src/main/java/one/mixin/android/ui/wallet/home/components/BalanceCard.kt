package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomePendingIndicator
import one.mixin.android.ui.wallet.home.WalletHomePendingKind
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.WalletHomeWatchIndicator
import one.mixin.android.ui.wallet.home.WalletHomeWatchKind
import one.mixin.android.vo.Fiats

private val WalletHomeActionWidth = 64.dp
private val WalletHomeActionIconSize = 42.dp
private val WalletHomeActionRowHorizontalPadding = 9.dp

@Composable
internal fun EmptyGuideCard(callbacks: WalletHomeCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { callbacks.onReceiveClicked() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_wallet_home_fund),
            contentDescription = null,
            modifier = Modifier.size(70.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.wallet_home_empty_title),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            WalletHomeButton(
                textRes = R.string.wallet_home_receive_crypto,
                onClick = callbacks::onReceiveClicked,
                primary = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
    cornerRadius: Int = 12,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow)
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
internal fun BalanceCard(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
    contentHorizontalPadding: Dp = 0.dp,
) {
    val compactTextStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .holidaySurpriseEffect(),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentHorizontalPadding),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
            text = stringResource(R.string.Total_Balance),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            style = compactTextStyle,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.fiatTotal.uppercase(),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontFamily = FontFamily(Font(R.font.mixin_font)),
                style = compactTextStyle,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = Fiats.getAccountCurrencyAppearance(),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                style = compactTextStyle,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${state.btcTotal} BTC",
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            style = compactTextStyle,
        )
        state.watchIndicator?.let {
            Spacer(modifier = Modifier.height(28.dp))
            WatchIndicator(
                indicator = it,
                onClick = callbacks::onWatchIndicatorClicked,
            )
        }
        state.importKeyAction?.let {
            Spacer(modifier = Modifier.height(28.dp))
            ImportKeyAction(
                action = it,
                onClick = callbacks::onImportKeyClicked,
            )
        }
        }
        val actions = listOf(
            WalletHomeAction(
                iconRes = R.drawable.ic_wallet_buy,
                labelRes = R.string.Buy,
                showBadge = state.showBuyBadge,
                onClick = callbacks::onBuyClicked,
            ),
            WalletHomeAction(
                iconRes = R.drawable.ic_wallet_receive,
                labelRes = R.string.Receive,
                showBadge = false,
                onClick = callbacks::onReceiveClicked,
            ),
            WalletHomeAction(
                iconRes = R.drawable.ic_wallet_send,
                labelRes = R.string.Send_transfer,
                showBadge = false,
                onClick = callbacks::onSendClicked,
            ),
            WalletHomeAction(
                iconRes = R.drawable.ic_wallet_swap,
                labelRes = R.string.Trade,
                showBadge = state.showSwapBadge,
                onClick = callbacks::onSwapClicked,
            ),
        )
        val showActions = state.importKeyAction == null &&
            !state.hideActions &&
            !(state.walletType == WalletHomeType.CLASSIC && state.isWatchWallet)
        if (showActions) {
            Spacer(modifier = Modifier.height(19.5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = walletHomeActionRowHorizontalPadding(actions.size)),
                horizontalArrangement = walletHomeActionArrangement(actions.size),
                verticalAlignment = Alignment.Top,
            ) {
                actions.forEach { action ->
                    ActionItem(
                        iconRes = action.iconRes,
                        labelRes = action.labelRes,
                        showBadge = action.showBadge,
                        modifier = Modifier.width(WalletHomeActionWidth),
                        onClick = action.onClick,
                    )
                }
            }
        }
        state.pendingIndicator?.let {
            Spacer(modifier = Modifier.height(if (showActions) 12.dp else 14.dp))
            Box(modifier = Modifier.padding(horizontal = contentHorizontalPadding)) {
                PendingIndicator(
                    indicator = it,
                    onClick = callbacks::onPendingIndicatorClicked,
                )
            }
        }
    }
}

@Composable
private fun PendingIndicator(
    indicator: WalletHomePendingIndicator,
    onClick: () -> Unit,
) {
    IndicatorPill(onClick = onClick) {
        PendingIconGroup(indicator)
        if (indicator.iconUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = pendingIndicatorText(indicator),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.W400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (indicator.showsArrow()) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_gray_right),
                contentDescription = null,
                modifier = Modifier.padding(start = 2.dp).offset(x = 4.dp),
            )
        }
    }
}

private fun WalletHomePendingIndicator.showsArrow(): Boolean =
    kind == WalletHomePendingKind.SINGLE_DEPOSIT ||
        kind == WalletHomePendingKind.MULTIPLE_DEPOSITS ||
        kind == WalletHomePendingKind.SINGLE_TRANSACTION ||
        kind == WalletHomePendingKind.MULTIPLE_TRANSACTIONS

@Composable
private fun PendingIconGroup(indicator: WalletHomePendingIndicator) {
    val icons = indicator.iconUrls.take(2)
    if (icons.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        icons.forEach { iconUrl ->
            CoilImage(
                model = iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(20.dp)
                    .border(1.dp, Color.White, CircleShape)
                    .clip(CircleShape),
            )
        }
        val hiddenCount = indicator.value.toIntOrNull()?.minus(2) ?: 0
        if (hiddenCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$hiddenCount",
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W400,
                )
            }
        }
    }
}

@Composable
private fun pendingIndicatorText(indicator: WalletHomePendingIndicator): String =
    when (indicator.kind) {
        WalletHomePendingKind.SINGLE_DEPOSIT -> stringResource(R.string.Deposit_Pending_Confirmation, indicator.value)
        WalletHomePendingKind.MULTIPLE_DEPOSITS -> stringResource(R.string.Deposits_Pending_Confirmation, indicator.value.toIntOrNull() ?: 0)
        WalletHomePendingKind.SINGLE_TRANSACTION -> stringResource(R.string.Transaction_Pending_Confirmation)
        WalletHomePendingKind.MULTIPLE_TRANSACTIONS -> stringResource(R.string.Transactions_Pending_Confirmation, indicator.value.toIntOrNull() ?: 0)
    }

@Composable
private fun WatchIndicator(
    indicator: WalletHomeWatchIndicator,
    onClick: () -> Unit,
) {
    IndicatorPill(onClick = onClick) {
        Image(
            painter = painterResource(id = R.drawable.ic_wallet_watch),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when (indicator.kind) {
                WalletHomeWatchKind.SINGLE_ADDRESS -> stringResource(R.string.watching_address, indicator.value)
                WalletHomeWatchKind.MULTIPLE_ADDRESSES -> stringResource(R.string.watching_addresses, indicator.value.toIntOrNull() ?: 0)
            },
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 12.sp,
        )
        Image(
            painter = painterResource(id = R.drawable.ic_watch_arrow),
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun ImportKeyAction(
    action: WalletHomeImportKeyAction,
    onClick: () -> Unit,
) {
    WalletHomeButton(
        textRes = action.buttonTextRes,
        onClick = onClick,
        primary = true,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8,
    )
}

private data class WalletHomeAction(
    val iconRes: Int,
    val labelRes: Int,
    val showBadge: Boolean,
    val onClick: () -> Unit,
)

private fun walletHomeActionArrangement(actionCount: Int) = when (actionCount) {
    3 -> Arrangement.SpaceAround
    4 -> Arrangement.SpaceBetween
    else -> Arrangement.SpaceAround
}

private fun walletHomeActionRowHorizontalPadding(actionCount: Int) = when (actionCount) {
    4 -> WalletHomeActionRowHorizontalPadding
    else -> 0.dp
}

@Composable
private fun IndicatorPill(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
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
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Box {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(WalletHomeActionIconSize),
                )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MixinAppTheme.colors.tipError),
                )
            }
        }
        Spacer(modifier = Modifier.height(11.dp))
        Text(
            text = stringResource(labelRes),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}
