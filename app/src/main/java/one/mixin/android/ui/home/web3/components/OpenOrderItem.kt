package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.numberFormat
import one.mixin.android.ui.home.web3.trade.SwapViewModel
import one.mixin.android.vo.route.OrderState
import one.mixin.android.vo.route.Order

@Composable
fun OpenOrderItem(order: Order, onClick: () -> Unit) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val fromToken by viewModel.assetItemFlow(order.payAssetId).collectAsStateWithLifecycle(null)
    val toToken by viewModel.assetItemFlow(order.receiveAssetId).collectAsStateWithLifecycle(null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            CoilImage(
                fromToken?.iconUrl,
                modifier = Modifier
                    .width(30.dp)
                    .height(30.dp),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            CoilImage(
                toToken?.iconUrl,
                modifier = Modifier
                    .offset(x = 10.dp, y = 10.dp)
                    .width(34.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .border(2.dp, MixinAppTheme.colors.background, CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }

        Spacer(modifier = Modifier.width(22.dp))

        Column {
            // Line 1: symbol -> symbol | time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${fromToken?.symbol ?: ""} → ${toToken?.symbol ?: ""}",
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.createdAt.fullDate(),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.End
                )
            }

            // Line 2: -xx symbol (red) | type
            Row(verticalAlignment = Alignment.CenterVertically) {
                val payAmountText = order.payAmount.ifEmpty { "0" }.numberFormat()
                Text(
                    text = "-${payAmountText} ${fromToken?.symbol ?: ""}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletRed,
                )
                Spacer(modifier = Modifier.weight(1f))
                val typeText = when (order.orderType.lowercase()) {
                    "swap" -> stringResource(R.string.order_type_swap)
                    "limit" -> stringResource(R.string.order_type_limit)
                    else -> order.orderType
                }
                Text(
                    text = typeText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MixinAppTheme.colors.textAssist,
                )
            }

            // Line 3: +xx symbol (color by status) | state with color
            Row(verticalAlignment = Alignment.CenterVertically) {
                val orderState = OrderState.from(order.state)
                val receiveAmountText = (if (orderState.isPending()) {
                    order.expectedReceiveAmount
                } else {
                    order.receiveAmount
                } ?: "0").ifEmpty { "0" }.numberFormat()
                val hasReceivedAmount = !order.receiveAmount.isNullOrEmpty() && order.receiveAmount != "0"
                
                // Pending orders without received amount should be gray
                val leftColor = when {
                    orderState.isPending() && !hasReceivedAmount -> MixinAppTheme.colors.textAssist
                    orderState.isDone() -> MixinAppTheme.colors.walletGreen
                    else -> MixinAppTheme.colors.walletRed
                }
                Text(
                    text = "+${receiveAmountText} ${toToken?.symbol ?: ""}",
                    fontSize = 14.sp,
                    color = leftColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                val rightColor = when {
                    orderState.isPending() -> MixinAppTheme.colors.textAssist
                    orderState.isDone() -> MixinAppTheme.colors.walletGreen
                    else -> MixinAppTheme.colors.walletRed
                }
                val context = LocalContext.current
                val stateText = orderState.format(context)
                Text(
                    text = stateText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = rightColor,
                )
            }
        }
    }
}
