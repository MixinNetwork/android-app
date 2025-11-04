package one.mixin.android.ui.home.web3.trade

import PageScaffold
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.vo.route.Order
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun LimitOrderDetailPage(
    orderId: String,
    onShare: (String, String) -> Unit,
    onTryAgain: (String, String) -> Unit,
    pop: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SwapViewModel = hiltViewModel()
    val orderItem by viewModel.observeOrder(orderId).collectAsState(null)
    val payAsset by viewModel.assetItemFlow(orderItem?.payAssetId ?: "").collectAsState(null)
    val receiveAsset by viewModel.assetItemFlow(orderItem?.receiveAssetId ?: "").collectAsState(null)

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Order_Details),
            subtitle = {
                val text = stringResource(id = R.string.Privacy_Wallet)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MixinAppTheme.colors.textAssist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_privacy),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(12.dp)
                        )
                }
            },
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
                orderItem?.let { limitOrder ->
                    if (payAsset == null || receiveAsset == null) {
                        // show loading
                        return@let
                    }

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
                                payAsset?.iconUrl,
                                modifier = Modifier
                                    .width(47.dp)
                                    .height(47.dp),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                            CoilImage(
                                receiveAsset?.iconUrl,
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
                            text = "${payAsset?.symbol} → ${receiveAsset?.symbol}",
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
                                    when (limitOrder.state.lowercase()) {
                                        "settled" -> MixinAppTheme.colors.walletGreen.copy(alpha = 0.2f)
                                        "failed", "cancelled", "expired" -> MixinAppTheme.colors.walletRed.copy(alpha = 0.2f)
                                        else -> MixinAppTheme.colors.textMinor.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                formatLimitOrderState(context, limitOrder.state), color = when (limitOrder.state.lowercase()) {
                                    "settled" -> MixinAppTheme.colors.walletGreen
                                    "failed", "cancelled", "expired" -> MixinAppTheme.colors.walletRed
                                    else -> MixinAppTheme.colors.textPrimary
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        val scope = rememberCoroutineScope()
                        when (limitOrder.state.lowercase()) {
                            "pricing", "created", "quoting" -> {
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
                                        text = stringResource(R.string.cancel_order),
                                        color = MixinAppTheme.colors.walletRed,
                                        fontWeight = FontWeight.W500,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    viewModel.cancelLimitOrder(limitOrder.orderId)
                                                    pop()
                                                }
                                            }
                                            .padding(vertical = 10.dp)
                                    )
                                }
                            }
                            else -> {
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
                                            .background(
                                                MixinAppTheme.colors.backgroundWindow,
                                                RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                            )
                                            .clickable {
                                                onTryAgain.invoke(
                                                    limitOrder.payAssetId,
                                                    limitOrder.receiveAssetId
                                                )
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
                                            .background(
                                                MixinAppTheme.colors.backgroundWindow,
                                                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                            )
                                            .clickable {
                                                onShare.invoke(
                                                    limitOrder.payAssetId,
                                                    limitOrder.receiveAssetId
                                                )
                                            }
                                            .padding(vertical = 10.dp)
                                    )
                                }
                            }
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
                            "-${limitOrder.payAmount} ${payAsset?.symbol}",
                            MixinAppTheme.colors.walletRed,
                            payAsset?.iconUrl,
                            payAsset?.chainName ?: ""
                        )
                        DetailItem(
                            if (limitOrder.state.lowercase() == "settled") context.getString(R.string.swap_order_received).uppercase() else context.getString(R.string.Estimated_Receive).uppercase(),
                            "+${limitOrder.expectedReceiveAmount} ${receiveAsset?.symbol}",
                            MixinAppTheme.colors.walletGreen,
                            receiveAsset?.iconUrl,
                            receiveAsset?.chainName ?: ""
                        )
                        DetailPriceItem(
                            limitOrder,
                            payAsset?.symbol ?: "",
                            receiveAsset?.symbol ?: ""
                        )
                        run {
                            val filledReceive = runCatching { BigDecimal(limitOrder.filledReceiveAmount) }.getOrDefault(BigDecimal.ZERO)
                            val expectedReceive = runCatching { BigDecimal(limitOrder.expectedReceiveAmount) }.getOrDefault(BigDecimal.ZERO)
                            val percentStr = if (expectedReceive > BigDecimal.ZERO) {
                                filledReceive
                                    .divide(expectedReceive, 6, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal(100))
                                    .setScale(2, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                            } else {
                                "0"
                            }
                            val endText = "${filledReceive.stripTrailingZeros().toPlainString()} ${payAsset?.symbol ?: ""}"
                            DetailItem(
                                label = stringResource(R.string.Filled).uppercase(),
                                value = "$percentStr%",
                                end = endText,
                            )
                        }
                        DetailItem(
                            label = stringResource(R.string.Type).uppercase(),
                            value = context.getString(R.string.order_type_limit),
                        )
                        DetailItem(
                            label = stringResource(R.string.Order_Created).uppercase(),
                            value = limitOrder.createdAt.fullDate()
                        )
                        DetailItem(
                            label = stringResource(R.string.Order_ID).uppercase(),
                            value = limitOrder.orderId,
                            onCopy = {
                                context.getClipboardManager().setPrimaryClip(
                                    android.content.ClipData.newPlainText(
                                        null,
                                        limitOrder.orderId
                                    )
                                )
                                toast(R.string.copied_to_clipboard)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DetailPriceItem(
    orderItem: Order,
    paySymbol: String,
    receiveSymbol: String
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (orderItem.state.lowercase() == "settled") context.getString(R.string.Price) else context.getString(R.string.Estimated_Price).uppercase(),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = runCatching { "1 $paySymbol ≈ ${BigDecimal(orderItem.expectedReceiveAmount).divide(BigDecimal(orderItem.payAmount), 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} $receiveSymbol" }.getOrDefault("N/A"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = runCatching { "1 $receiveSymbol ≈ ${BigDecimal(orderItem.payAmount).divide(BigDecimal(orderItem.expectedReceiveAmount), 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} $paySymbol" }.getOrDefault("N/A"),
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
    end: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = end,
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
