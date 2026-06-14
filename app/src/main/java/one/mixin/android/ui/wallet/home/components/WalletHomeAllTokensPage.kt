package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.PrivacyWalletTokenItem
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.Web3WalletTokenItem

@Composable
fun WalletHomeAllTokensPage(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    MixinAppTheme {
        val isPrivacy = state.walletType == WalletHomeType.PRIVACY
        val tokensEmpty = if (isPrivacy) {
            state.privacyTokens.isEmpty()
        } else {
            state.web3Tokens.isEmpty()
        }
        val backgroundColor = MixinAppTheme.colors.background
        val borderColor = MixinAppTheme.colors.borderColor
        if (tokensEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                WalletHomeCard(WalletHomeCardType.BALANCE, state, callbacks)
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .cardBackground(backgroundColor, borderColor),
                ) {
                    EmptyTokens()
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            val count = if (isPrivacy) state.privacyTokens.size else state.web3Tokens.size
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                item(key = "balance") {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        WalletHomeCard(WalletHomeCardType.BALANCE, state, callbacks)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                items(
                    count = count,
                    key = { index ->
                        if (isPrivacy) {
                            state.privacyTokens[index].assetId
                        } else {
                            val token = state.web3Tokens[index]
                            "${token.assetId}-${token.chainId}"
                        }
                    },
                ) { index ->
                    TokenCardSegment(
                        isFirst = index == 0,
                        isLast = index == count - 1,
                        borderColor = borderColor,
                    ) {
                        if (isPrivacy) {
                            PrivacyWalletTokenItem(
                                token = state.privacyTokens[index],
                                onClick = { callbacks.onTokenClicked(index) },
                            )
                        } else {
                            Web3WalletTokenItem(
                                token = state.web3Tokens[index],
                                onClick = { callbacks.onTokenClicked(index) },
                            )
                        }
                    }
                }
                item(key = "bottom") {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TokenCardSegment(
    isFirst: Boolean,
    isLast: Boolean,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .tokenCardBorder(isFirst, isLast, borderColor)
            .padding(
                top = if (isFirst) 20.dp else 0.dp,
                bottom = if (isLast) 20.dp else 0.dp,
            ),
    ) {
        content()
        if (!isLast) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun Modifier.tokenCardBorder(
    isFirst: Boolean,
    isLast: Boolean,
    color: Color,
    width: Dp = 0.8.dp,
    cornerRadius: Dp = 8.dp,
): Modifier = drawBehind {
    val stroke = width.toPx()
    val radius = cornerRadius.toPx()
    val half = stroke / 2f
    val left = half
    val right = size.width - half
    val top = half
    val bottom = size.height - half
    val diameter = radius * 2f

    val sideTop = if (isFirst) top + radius else 0f
    val sideBottom = if (isLast) bottom - radius else size.height
    drawLine(color, Offset(left, sideTop), Offset(left, sideBottom), stroke)
    drawLine(color, Offset(right, sideTop), Offset(right, sideBottom), stroke)

    if (isFirst) {
        drawLine(color, Offset(left + radius, top), Offset(right - radius, top), stroke)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(diameter, diameter),
            style = Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - diameter, top),
            size = Size(diameter, diameter),
            style = Stroke(stroke),
        )
    }
    if (isLast) {
        drawLine(color, Offset(left + radius, bottom), Offset(right - radius, bottom), stroke)
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, bottom - diameter),
            size = Size(diameter, diameter),
            style = Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - diameter, bottom - diameter),
            size = Size(diameter, diameter),
            style = Stroke(stroke),
        )
    }
}

@Composable
private fun EmptyTokens() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_file),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.No_asset),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
        )
    }
}
