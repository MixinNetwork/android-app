package one.mixin.android.ui.home.web3.swap

import PageScaffold
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.route.SwapOrderItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SwapOrderListPage(
    pop: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val scope = rememberCoroutineScope()
    val orders by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.webOrders()
        }
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Swap_Orders),
            pop = pop,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(orders) { order ->
                    OrderItem(
                        order = order,
                        onClick = { onOrderClick(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItem(
    order: SwapOrderItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            CoilImage(
                order.assetIconUrl,
                modifier = Modifier
                    .width(44.dp)
                    .height(44.dp),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            CoilImage(
                order.receiveAssetIconUrl,
                modifier = Modifier
                    .offset(x = 10.dp, y = 10.dp)
                    .width(46.dp)
                    .height(46.dp)
                    .border(2.dp, MixinAppTheme.colors.background, CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }

        Spacer(modifier = Modifier.width(22.dp))

        Column {
            Text(
                text = "${order.assetSymbol ?: ""} -> ${order.receiveAssetSymbol ?: ""}",
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Text(
                text = "-${order.payAmount} ${order.assetSymbol}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.walletRed,
            )
            Text(
                text = "+${order.receiveAmount} ${order.receiveAssetSymbol}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.walletGreen,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDateTime(order.createdAt),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                textAlign = TextAlign.End
            )
            Text(
                text = formatOrderState(order.state),
                fontSize = 14.sp,
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
