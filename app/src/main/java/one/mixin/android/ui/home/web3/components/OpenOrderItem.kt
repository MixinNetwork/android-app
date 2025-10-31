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
import one.mixin.android.api.response.LimitOrder
import one.mixin.android.api.response.LimitOrderStatus
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.ui.home.web3.trade.SwapViewModel

@Composable
fun OpenOrderItem(order: LimitOrder, onClick: () -> Unit) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val fromToken by viewModel.assetItemFlow(order.assetId).collectAsStateWithLifecycle(null)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${fromToken?.symbol ?: "..."} → ${toToken?.symbol ?: "..."}",
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.createdAt.fullDate(), fontSize = 14.sp, color = MixinAppTheme.colors.textAssist, textAlign = TextAlign.End
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${order.amount} ${fromToken?.symbol ?: "..."}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletRed,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.limit_price),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${order.expectedReceiveAmount} ${toToken?.symbol ?: "..."}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletGreen,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = order.state.value.replaceFirstChar { it.uppercase() }, fontSize = 14.sp, textAlign = TextAlign.End, color = when (order.state) {
                        LimitOrderStatus.CREATED, LimitOrderStatus.PRICING, LimitOrderStatus.QUOTING -> MixinAppTheme.colors.textAssist
                        LimitOrderStatus.SETTLED -> MixinAppTheme.colors.green
                        else -> MixinAppTheme.colors.red
                    }
                )
            }
        }
    }
}
