package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyKind
import one.mixin.android.ui.wallet.home.WalletHomePendingIndicator
import one.mixin.android.ui.wallet.home.WalletHomePendingKind
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.WalletHomeWatchIndicator
import one.mixin.android.ui.wallet.home.WalletHomeWatchKind

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
            .clip(RoundedCornerShape(24.dp))
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
internal fun BalanceCard(state: WalletHomeState, callbacks: WalletHomeCallbacks) {
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
            fontSize = 32.sp,
            fontWeight = FontWeight.W600,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${state.btcTotal} BTC",
            color = MixinAppTheme.colors.textAssist,
            fontSize = 13.sp,
        )
        if (state.pendingIndicator != null || state.watchIndicator != null || state.importKeyAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.pendingIndicator?.let {
                    PendingIndicator(
                        indicator = it,
                        onClick = callbacks::onPendingIndicatorClicked,
                    )
                }
                state.watchIndicator?.let {
                    WatchIndicator(
                        indicator = it,
                        onClick = callbacks::onWatchIndicatorClicked,
                    )
                }
                state.importKeyAction?.let {
                    ImportKeyAction(
                        action = it,
                        onClick = callbacks::onImportKeyClicked,
                    )
                }
            }
        }
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
            fontWeight = FontWeight.W500,
        )
        if (indicator.kind == WalletHomePendingKind.SINGLE_DEPOSIT || indicator.kind == WalletHomePendingKind.MULTIPLE_DEPOSITS) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_gray_right),
                contentDescription = null,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

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
                    .clip(CircleShape),
            )
        }
        val hiddenCount = indicator.value.toIntOrNull()?.minus(2) ?: 0
        if (hiddenCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.backgroundWindow),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$hiddenCount",
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W600,
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
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
        )
        Image(
            painter = painterResource(id = R.drawable.ic_watch_arrow),
            contentDescription = null,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun ImportKeyAction(
    action: WalletHomeImportKeyAction,
    onClick: () -> Unit,
) {
    IndicatorPill(onClick = onClick) {
        val iconRes = when (action.kind) {
            WalletHomeImportKeyKind.MNEMONIC_PHRASE -> R.drawable.ic_menu_mnemonic_phrase
            WalletHomeImportKeyKind.PRIVATE_KEY -> R.drawable.ic_menu_private_key
        }
        val textRes = when (action.kind) {
            WalletHomeImportKeyKind.MNEMONIC_PHRASE -> R.string.import_mnemonic_phrase
            WalletHomeImportKeyKind.PRIVATE_KEY -> R.string.import_private_key
        }
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(textRes),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
        )
        Image(
            painter = painterResource(id = R.drawable.ic_watch_arrow),
            contentDescription = null,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
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
