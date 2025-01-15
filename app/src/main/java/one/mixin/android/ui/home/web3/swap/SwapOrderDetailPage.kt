package one.mixin.android.ui.home.web3.swap

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.route.SwapOrderItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SwapOrderDetailPage(
    orderId: String,
    pop: () -> Unit,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    var order by remember { mutableStateOf<SwapOrderItem?>(null) }

    LaunchedEffect(orderId) {
        order = viewModel.getOrderById(orderId)
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Swap_Order_Detail),
            pop = pop,
            verticalScrollable = true
        ) {
            order?.let { swapOrder ->
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground(
                                MixinAppTheme.colors.background,
                                MixinAppTheme.colors.borderColor
                            )
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .wrapContentSize()
                                .padding()
                        ) {
                            CoilImage(
                                swapOrder.assetIconUrl,
                                modifier = Modifier
                                    .width(47.dp)
                                    .height(47.dp),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                            CoilImage(
                                swapOrder.receiveAssetIconUrl,
                                modifier = Modifier
                                    .offset(x = 16.dp, y = 16.dp)
                                    .width(54.dp)
                                    .height(54.dp)
                                    .border(3.dp, MixinAppTheme.colors.background, CircleShape),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${swapOrder.assetSymbol} -> ${swapOrder.receiveAssetSymbol}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MixinAppTheme.colors.backgroundWindow),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .clickable {

                                    }
                            ) {
                                Text(
                                    "Swap Again",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.W500,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(Color(0x0D000000))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    clip (RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .clickable {

                                    }
                            ) {
                                Text(
                                    "Share Pair",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.W500,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .cardBackground(
                                MixinAppTheme.colors.background,
                                MixinAppTheme.colors.borderColor
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        DetailItem(
                            "PAID",
                            "${swapOrder.payAmount} ${swapOrder.assetSymbol}",
                            MixinAppTheme.colors.walletRed,
                            swapOrder.assetIconUrl,
                            getChainNetwork(swapOrder.payAssetId, swapOrder.payChainId ?: "", null)
                                ?: ""
                        )
                        DetailItem(
                            "RECEIVED",
                            "${swapOrder.receiveAmount} ${swapOrder.receiveAssetSymbol}",
                            MixinAppTheme.colors.walletGreen,
                            swapOrder.receiveAssetIconUrl,
                            getChainNetwork(
                                swapOrder.receiveAssetId,
                                swapOrder.receiveChainId ?: "",
                                null
                            ) ?: ""
                        )
                        DetailItem(
                            label = "订单状态",
                            value = formatOrderState(swapOrder.state)
                        )
                        DetailItem(
                            label = "创建时间",
                            value = formatDateTime(swapOrder.createdAt)
                        )
                        DetailItem(
                            label = "订单ID",
                            value = swapOrder.orderId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    color: Color,
    icon: String?,
    chain: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoilImage(
                model = icon,
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                color = color
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = chain,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
    }
}

private fun formatDateTime(dateTime: String): String {
    val instant = Instant.parse(dateTime)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun formatOrderState(state: String): String {
    return when (state) {
        "pending" -> "等待中"
        "success" -> "已完成"
        "failed" -> "失败"
        else -> state
    }
}
