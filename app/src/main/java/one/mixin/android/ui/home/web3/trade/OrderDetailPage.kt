package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.route.OrderState
import one.mixin.android.vo.route.OrderItem
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun OrderDetailPage(
    walletId: String?,
    orderId: String,
    onShare: (String, String, String) -> Unit,
    onTryAgain: (String, String) -> Unit,
    pop: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SwapViewModel = hiltViewModel()
    val orderItem = viewModel.getOrderById(orderId).collectAsState(null)
    var walletDisplayName by remember { mutableStateOf<String?>(null) }
    var toAddress by remember { mutableStateOf<String?>(null) }

@Composable
fun DetailItem(
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

    val currentWalletId = orderItem.value?.walletId ?: walletId

    LaunchedEffect(currentWalletId) {
        if (currentWalletId != null) {
            viewModel.findWeb3WalletById(currentWalletId)?.let {
                if (it.category == WalletCategory.CLASSIC.value ||
                    it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value) {
                    walletDisplayName = it.name
                }
            }
        }
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Order_Details),
            subtitle = {
                val isPrivacy = currentWalletId != null && currentWalletId == Session.getAccountId()
                val text = when {
                    isPrivacy -> stringResource(id = R.string.Privacy_Wallet)
                    walletDisplayName != null -> walletDisplayName!!
                    else -> stringResource(id = R.string.Common_Wallet)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MixinAppTheme.colors.textAssist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPrivacy) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_privacy),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(12.dp)
                        )
                    }
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
                orderItem.value?.let { order ->
                    LaunchedEffect(currentWalletId, order.receiveChainId) {
                        val wId = currentWalletId
                        val chainId = order.receiveChainId
                        if (!wId.isNullOrBlank() && !chainId.isNullOrBlank()) {
                            val addrByChain = viewModel.getAddressesByChainId(wId, chainId)
                            if (addrByChain != null) {
                                toAddress = addrByChain.destination
                            } else {
                                val anyAddr = viewModel.getAddresses(wId).firstOrNull()
                                toAddress = anyAddr?.destination
                            }
                        }
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
                                order.assetIconUrl,
                                modifier = Modifier
                                    .width(47.dp)
                                    .height(47.dp),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                            CoilImage(
                                order.receiveAssetIconUrl,
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
                            text = "${order.assetSymbol} → ${order.receiveAssetSymbol}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val uiState = OrderState.from(order.state)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (uiState) {
                                        OrderState.SETTLED -> MixinAppTheme.colors.walletGreen.copy(alpha = 0.2f)
                                        OrderState.FAILED, OrderState.CANCELLED, OrderState.EXPIRED -> MixinAppTheme.colors.walletRed.copy(alpha = 0.2f)
                                        else -> MixinAppTheme.colors.textMinor.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                formatOrderState(context, order.state), color = when (uiState) {
                                    OrderState.SETTLED -> MixinAppTheme.colors.walletGreen
                                    OrderState.FAILED, OrderState.CANCELLED, OrderState.EXPIRED -> MixinAppTheme.colors.walletRed
                                    else -> MixinAppTheme.colors.textPrimary
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        val scope = rememberCoroutineScope()
                        when {
                            order.type == "limit" && (uiState == OrderState.PRICING || uiState == OrderState.CREATED || uiState == OrderState.QUOTING || uiState == OrderState.PENDING) -> {
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
                                            .weight(1f)
                                            .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                            .clickable {
                                                scope.launch {
                                                    runCatching { viewModel.cancelLimitOrder(order.orderId) }
                                                    pop()
                                                }
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
                                            .clickable { onShare.invoke(order.payAssetId, order.receiveAssetId, order.type) }
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
                                            .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                            .clickable { onTryAgain.invoke(order.payAssetId, order.receiveAssetId) }
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
                                            .clickable { onShare.invoke(order.payAssetId, order.receiveAssetId, order.type) }
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
                            "-${order.payAmount} ${order.assetSymbol}",
                            MixinAppTheme.colors.walletRed,
                            order.assetIconUrl,
                            order.payChainName ?: ""
                        )
                        DetailItem(
                            if (order.state == OrderState.SUCCESS.value) context.getString(R.string.swap_order_received).uppercase() else context.getString(R.string.Estimated_Receive).uppercase(),
                            "+${if (order.state == OrderState.SUCCESS.value) (order.receiveAmount ?: "0") else (order.expectedReceiveAmount ?: "0")} ${order.receiveAssetSymbol}",
                            MixinAppTheme.colors.walletGreen,
                            order.receiveAssetIconUrl,
                            order.receiveChainName ?: ""
                        )
                        if (order.type == "limit") {
                            DetailPriceItemLimit(order)
                        } else {
                            DetailPriceItem(order)
                        }
                        if (order.type == "limit") {
                            val filled = runCatching { BigDecimal(order.filledReceiveAmount ?: "0") }.getOrDefault(BigDecimal.ZERO)
                            val expected = runCatching { BigDecimal(order.expectedReceiveAmount ?: "0") }.getOrDefault(BigDecimal.ZERO)
                            val percentStr = if (expected > BigDecimal.ZERO) {
                                filled
                                    .divide(expected, 6, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal(100))
                                    .setScale(2, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                            } else {
                                "0"
                            }
                            val endText = "${filled.stripTrailingZeros().toPlainString()} ${order.receiveAssetSymbol}"
                            DetailItem(
                                label = stringResource(R.string.Filled).uppercase(),
                                value = "$percentStr%",
                                end = endText,
                            )
                            val isPrivacyWallet = currentWalletId != null && currentWalletId == Session.getAccountId()
                            if (isPrivacyWallet) {
                                ItemWalletContent(title = stringResource(id = R.string.Wallet).uppercase(), fontSize = 16.sp, padding = 0.dp)
                                Spacer(modifier = Modifier.height(20.dp))
                            } else {
                                ItemContent(
                                    title = stringResource(id = R.string.Receivers).uppercase(),
                                    subTitle = toAddress ?: "",
                                    label = walletDisplayName,
                                    isAddress = !toAddress.isNullOrBlank(),
                                    padding = 0.dp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            order.expiredAt?.let { exp ->
                                DetailItem(
                                    label = stringResource(R.string.Expiration).uppercase(),
                                    value = exp.fullDate()
                                )
                            }
                            DetailItem(
                                label = stringResource(R.string.Type).uppercase(),
                                value = context.getString(R.string.order_type_limit),
                            )
                        } else {
                            // Wallet row
                            val isPrivacyWallet = currentWalletId != null && currentWalletId == Session.getAccountId()
                            DetailItem(
                                label = stringResource(R.string.Common_Wallet).uppercase(),
                                value = if (isPrivacyWallet) stringResource(id = R.string.Privacy_Wallet) else (walletDisplayName ?: stringResource(id = R.string.Common_Wallet)),
                            )
                            DetailItem(
                                label = stringResource(R.string.Type).uppercase(),
                                value = context.getString(R.string.order_type_swap),
                            )
                        }
                        DetailItem(
                            label = stringResource(R.string.Order_Created).uppercase(),
                            value = order.createdAt.fullDate()
                        )
                        DetailItem(
                            label = stringResource(R.string.Order_ID).uppercase(),
                            value = order.orderId,
                            onCopy = {
                                context.getClipboardManager().setPrimaryClip(
                                    android.content.ClipData.newPlainText(
                                        null,
                                        order.orderId
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
fun DetailPriceItemLimit(orderItem: OrderItem) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.Price),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        val price = orderItem.price?.takeIf { it.isNotBlank() }?.let { runCatching { BigDecimal(it) }.getOrNull() }
        val pay = runCatching { BigDecimal(orderItem.payAmount) }.getOrNull()
        val recv = runCatching { BigDecimal(orderItem.expectedReceiveAmount ?: orderItem.receiveAmount ?: "") }.getOrNull()

        val forward = when {
            price != null && price.compareTo(BigDecimal.ZERO) > 0 ->
                "1 ${orderItem.assetSymbol} = ${price.stripTrailingZeros().toPlainString()} ${orderItem.receiveAssetSymbol}"
            pay != null && recv != null && pay.compareTo(BigDecimal.ZERO) != 0 ->
                "1 ${orderItem.assetSymbol} = ${recv.divide(pay, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} ${orderItem.receiveAssetSymbol}"
            else -> "N/A"
        }

        Text(
            text = forward,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        val backward = when {
            price != null && price.compareTo(BigDecimal.ZERO) > 0 ->
                "1 ${orderItem.receiveAssetSymbol} = ${BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} ${orderItem.assetSymbol}"
            recv != null && recv.compareTo(BigDecimal.ZERO) != 0 ->
                "1 ${orderItem.receiveAssetSymbol} = ${pay?.divide(recv, 8, RoundingMode.HALF_UP)?.stripTrailingZeros()?.toPlainString() ?: "N/A"} ${orderItem.assetSymbol}"
            else -> "N/A"
        }

        Text(
            text = backward,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun DetailPriceItem(
    orderItem: OrderItem
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
            text = run {
                val pay = runCatching { BigDecimal(orderItem.payAmount) }.getOrNull()
                val recv = runCatching { BigDecimal(orderItem.receiveAmount ?: "") }.getOrNull()
                if (pay != null && recv != null && pay.compareTo(BigDecimal.ZERO) != 0) {
                    "1 ${orderItem.assetSymbol} ≈ ${recv.divide(pay, 8, RoundingMode.HALF_UP)} ${orderItem.receiveAssetSymbol}"
                } else {
                    "N/A"
                }
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MixinAppTheme.colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = run {
                val pay = runCatching { BigDecimal(orderItem.payAmount) }.getOrNull()
                val recv = runCatching { BigDecimal(orderItem.receiveAmount ?: "") }.getOrNull()
                if (pay != null && recv != null && recv.compareTo(BigDecimal.ZERO) != 0) {
                    "1 ${orderItem.receiveAssetSymbol} ≈ ${pay.divide(recv, 8, RoundingMode.HALF_UP)} ${orderItem.assetSymbol}"
                } else {
                    "N/A"
                }
            },
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
        OrderState.CANCELLING.value -> context.getString(R.string.order_state_cancelling)
        else -> state
    }
}
