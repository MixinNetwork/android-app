package one.mixin.android.ui.home.web3.swap

import PageScaffold
import android.content.Context
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.route.OrderState
import one.mixin.android.vo.route.SwapOrderItem
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SwapOrderDetailPage(
    orderId: String,
    onShare: (String, String) -> Unit,
    onTryAgain: (String, String) -> Unit,
    pop: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SwapViewModel>()
    var order by remember { mutableStateOf<SwapOrderItem?>(null) }

    LaunchedEffect(orderId) {
        order = viewModel.getOrderById(orderId)
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Order_Details),
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                order?.let { swapOrder ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
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
                                    .clip(CircleShape)
                                    .border(3.dp, MixinAppTheme.colors.background, CircleShape),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(
                            text = "${swapOrder.assetSymbol} → ${swapOrder.receiveAssetSymbol}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (swapOrder.state) {
                                        OrderState.SUCCESS.value -> MixinAppTheme.colors.walletGreen.copy(alpha = 0.2f)
                                        OrderState.FAILED.value -> MixinAppTheme.colors.walletRed.copy(alpha = 0.2f)
                                        else -> MixinAppTheme.colors.textMinor.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                formatOrderState(context, swapOrder.state), color = when (swapOrder.state) {
                                    OrderState.SUCCESS.value -> MixinAppTheme.colors.walletGreen
                                    OrderState.FAILED.value -> MixinAppTheme.colors.walletRed
                                    else -> MixinAppTheme.colors.textPrimary
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MixinAppTheme.colors.backgroundWindow),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.Swap_Again),
                                color = MixinAppTheme.colors.textPrimary,
                                fontWeight = FontWeight.W500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .clickable() {
                                        onTryAgain.invoke(swapOrder.payAssetId, swapOrder.receiveAssetId)
                                    }
                                    .padding(vertical = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(Color(0x0D000000))
                            )
                            Text(
                                text = stringResource(R.string.Share_Pair),
                                color = MixinAppTheme.colors.textPrimary,
                                fontWeight = FontWeight.W500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .clickable() {
                                        onShare.invoke(swapOrder.payAssetId, swapOrder.receiveAssetId)
                                    }
                                    .padding(vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .wrapContentHeight()
                            .cardBackground(
                                MixinAppTheme.colors.background,
                                MixinAppTheme.colors.borderColor
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        DetailItem(
                            context.getString(R.string.swap_order_paid).uppercase(),
                            "-${swapOrder.payAmount} ${swapOrder.assetSymbol}",
                            MixinAppTheme.colors.walletRed,
                            swapOrder.assetIconUrl,
                            swapOrder.payChainName ?: ""
                        )
                        DetailItem(
                            if (swapOrder.state == OrderState.SUCCESS.value) context.getString(R.string.swap_order_received).uppercase() else context.getString(R.string.Estimated_Receive).uppercase(),
                            "+${swapOrder.receiveAmount} ${swapOrder.receiveAssetSymbol}",
                            MixinAppTheme.colors.walletGreen,
                            swapOrder.receiveAssetIconUrl,
                            swapOrder.receiveChainName ?: ""
                        )
                        DetailPriceItem(
                            swapOrder
                        )
                        DetailItem(
                            label = stringResource(R.string.Type).uppercase(),
                            value = if (swapOrder.type == "swap") context.getString(R.string.order_type_swap) else if (swapOrder.type == "limit") context.getString(R.string.order_type_limit) else swapOrder.type,
                        )
                        DetailItem(
                            label = stringResource(R.string.Order_Created).uppercase(),
                            value = swapOrder.createdAt.fullDate()
                        )
                        DetailItem(
                            label = stringResource(R.string.Order_ID).uppercase(),
                            value = swapOrder.orderId,
                            onCopy = {
                                context.getClipboardManager().setPrimaryClip(
                                    android.content.ClipData.newPlainText(
                                        null,
                                        swapOrder.orderId
                                    )
                                )
                                toast(R.string.copied_to_clipboard)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPriceItem(
    orderItem: SwapOrderItem
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (orderItem.state == OrderState.SUCCESS.value) context.getString(R.string.Price) else context.getString(R.string.Estimated_Price).uppercase(),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = runCatching { "1 ${orderItem.receiveAssetSymbol} ≈ ${BigDecimal(orderItem.payAmount).divide(BigDecimal(orderItem.receiveAmount), 8, RoundingMode.HALF_UP)} ${orderItem.assetSymbol}" }.getOrDefault("N/A"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = runCatching { "1 ${orderItem.assetSymbol} ≈ ${BigDecimal(orderItem.receiveAmount).divide(BigDecimal(orderItem.payAmount), 8, RoundingMode.HALF_UP)} ${orderItem.receiveAmount}" }.getOrDefault("N/A"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            onCopy?.let { onCopy ->
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy_gray),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = {
                                onCopy.invoke()
                            }
                        )
                        .padding(start = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
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
    chain: String,
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
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
    }
}

fun formatOrderState(context: Context, state: String): String {
    return when (state) {
        OrderState.CREATED.value -> context.getString(R.string.State_Created)
        OrderState.PENDING.value -> context.getString(R.string.State_Pending)
        OrderState.SUCCESS.value -> context.getString(R.string.State_Success)
        OrderState.FAILED.value -> context.getString(R.string.State_Failed)
        OrderState.REFUNDED.value -> context.getString(R.string.State_Refunded)
        else -> state
    }
}
