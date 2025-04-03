package one.mixin.android.ui.home.web3.swap

import PageScaffold
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dayTime
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.openUrl
import one.mixin.android.vo.route.OrderState
import one.mixin.android.vo.route.SwapOrderItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwapOrderListPage(
    pop: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SwapViewModel>()

    val orders = viewModel.swapOrders.collectAsLazyPagingItems()
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Swap_Orders),
            pop = pop,
            actions = {
                IconButton(onClick = {
                    context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            }
        ) {
            if (orders.itemCount == 0 && orders.loadState.refresh !is LoadState.Loading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(R.drawable.ic_empty_file),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.NO_ORDERS),
                        color = MixinAppTheme.colors.textRemarks
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                OrderList(
                    orders = orders,
                    listState = listState,
                    onOrderClick = onOrderClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrderList(
    orders: LazyPagingItems<SwapOrderItem>,
    listState: LazyListState,
    onOrderClick: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        val orderList = (0 until orders.itemCount).mapNotNull { index -> orders[index] }
        val groupedOrders = orderList.groupBy { it.createdAt.hashForDate() }
        
        groupedOrders.forEach { (_, ordersInGroup) ->
            stickyHeader {
                DateHeader(
                    date = ordersInGroup.firstOrNull()?.createdAt?.dayTime() ?: ""
                )
            }
            
            items(
                count = ordersInGroup.size,
                key = { index -> ordersInGroup[index].orderId }
            ) { index ->
                OrderItem(
                    order = ordersInGroup[index],
                    onClick = { onOrderClick(ordersInGroup[index].orderId) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background)
            .padding(vertical = 8.dp, horizontal = 20.dp)
    ) {
        Text(
            text = date,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OrderItem(
    order: SwapOrderItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            CoilImage(
                order.assetIconUrl,
                modifier = Modifier
                    .width(30.dp)
                    .height(30.dp),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            CoilImage(
                order.receiveAssetIconUrl,
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
                    text = "${order.assetSymbol ?: ""} → ${order.receiveAssetSymbol ?: ""}",
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${order.payAmount} ${order.assetSymbol}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletRed,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (order.type == "swap") context.getString(R.string.order_type_swap) else if (order.type == "limit") context.getString(R.string.order_type_limit) else order.type,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${order.receiveAmount} ${order.receiveAssetSymbol}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.walletGreen,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatOrderState(context, order.state),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    color = when (order.state) {
                        OrderState.SUCCESS.value -> MixinAppTheme.colors.walletGreen
                        OrderState.FAILED.value -> MixinAppTheme.colors.walletRed
                        else -> MixinAppTheme.colors.textAssist
                    }
                )
            }
        }
    }
}
