package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal
import java.math.RoundingMode

private const val POSITION_REFRESH_INTERVAL_MS = 3_000L
private const val CLOSED_POSITION_REFRESH_LIMIT = 100

enum class AllPositionsType {
    OPEN,
    CLOSED,
}

@Composable
fun AllPositionsPage(
    positionType: AllPositionsType,
    viewModel: PerpetualViewModel,
    onBack: () -> Unit,
    onSupport: () -> Unit,
    onShowTradingGuide: () -> Unit,
    onOpenPositionClick: (PerpsPositionItem) -> Unit,
    onClosedPositionClick: (PerpsOrderItem) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val walletId = Session.getAccountId().orEmpty()
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)

    val openPositionsSnapshot by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeOpenPositions(walletId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalUnrealizedPnl by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeTotalUnrealizedPnl(walletId)
        } else {
            flowOf(0.0)
        }
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val totalRealizedPnl by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeTotalRealizedPnl(walletId)
        } else {
            flowOf(0.0)
        }
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val openPositionsPaging = remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.getOpenPositionsPaged(walletId)
        } else {
            flowOf(PagingData.empty<PerpsPositionItem>())
        }
    }.collectAsLazyPagingItems()
    val closedPositionsPaging = remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.getOrdersPaged(walletId)
        } else {
            flowOf(PagingData.empty<PerpsOrderItem>())
        }
    }.collectAsLazyPagingItems()
    var previousOpenPositionsCount by remember(walletId) { mutableStateOf<Int?>(null) }

    LaunchedEffect(walletId, lifecycleOwner) {
        if (walletId.isEmpty()) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.startRefreshOrders(walletId, intervalMs = POSITION_REFRESH_INTERVAL_MS)
            try {
                while (isActive) {
                    viewModel.refreshPositions(walletId)
                    delay(POSITION_REFRESH_INTERVAL_MS)
                }
            } finally {
                viewModel.stopRefreshOrders()
            }
        }
    }

    LaunchedEffect(walletId, openPositionsSnapshot.size) {
        if (walletId.isEmpty()) return@LaunchedEffect
        val currentCount = openPositionsSnapshot.size
        val lastCount = previousOpenPositionsCount
        if (lastCount != null && currentCount < lastCount) {
            viewModel.refreshOrders(walletId, limit = CLOSED_POSITION_REFRESH_LIMIT)
        }
        previousOpenPositionsCount = currentCount
    }

    val totalMargin = openPositionsSnapshot.fold(BigDecimal.ZERO) { total, position ->
        total + (position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }
    val totalPnlAmount = BigDecimal.valueOf(totalUnrealizedPnl)
    val totalPnlPercent = calculatePnlPercent(totalPnlAmount, totalMargin)
    val titleRes = if (positionType == AllPositionsType.OPEN) {
        R.string.perps_positions
    } else {
        R.string.perps_activity
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(titleRes),
            subtitleText = null,
            verticalScrollable = false,
            pop = onBack,
            actions = {
                IconButton(onClick = onSupport) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (positionType == AllPositionsType.OPEN) {
                    OpenPositionsContent(
                        positions = openPositionsPaging,
                        totalMargin = totalMargin,
                        totalPnl = totalPnlAmount,
                        totalPnlPercent = totalPnlPercent,
                        quoteColorReversed = quoteColorReversed,
                        onShowTradingGuide = onShowTradingGuide,
                        onPositionClick = onOpenPositionClick,
                    )
                } else {
                    ClosedPositionsContent(
                        positions = closedPositionsPaging,
                        totalRealizedPnl = BigDecimal.valueOf(totalRealizedPnl),
                        quoteColorReversed = quoteColorReversed,
                        onPositionClick = onClosedPositionClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenPositionsContent(
    positions: LazyPagingItems<PerpsPositionItem>,
    totalMargin: BigDecimal,
    totalPnl: BigDecimal,
    totalPnlPercent: BigDecimal,
    quoteColorReversed: Boolean,
    onShowTradingGuide: () -> Unit,
    onPositionClick: (PerpsPositionItem) -> Unit,
) {
    val refreshState = positions.loadState.refresh
    val isEmpty = refreshState is LoadState.NotLoading && positions.itemCount == 0

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isEmpty) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                item {
                    TotalPositionValueCard(
                        totalMargin = totalMargin,
                        totalPnl = totalPnl,
                        totalPnlPercent = totalPnlPercent,
                        quoteColorReversed = quoteColorReversed,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                openPositionItems(
                    positions = positions,
                    onPositionClick = onPositionClick,
                )
                appendLoadingItem(positions.loadState.append)
            }
        }

        when {
            refreshState is LoadState.Loading && positions.itemCount == 0 -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    color = MixinAppTheme.colors.accent,
                )
            }
            isEmpty -> {
                EmptyPositionsState(
                    text = stringResource(R.string.No_Position),
                    actionText = stringResource(R.string.how_perps_works),
                    onActionClick = onShowTradingGuide,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ClosedPositionsContent(
    positions: LazyPagingItems<PerpsOrderItem>,
    totalRealizedPnl: BigDecimal,
    quoteColorReversed: Boolean,
    onPositionClick: (PerpsOrderItem) -> Unit,
) {
    val refreshState = positions.loadState.refresh
    val isEmpty = refreshState is LoadState.NotLoading && positions.itemCount == 0

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isEmpty) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TotalRealizedPnlCard(
                        totalPnl = totalRealizedPnl,
                        quoteColorReversed = quoteColorReversed,
                    )
                }
                closedPositionItems(
                    positions = positions,
                    onPositionClick = onPositionClick,
                )
                appendLoadingItem(positions.loadState.append)
            }
        }

        when {
            refreshState is LoadState.Loading && positions.itemCount == 0 -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    color = MixinAppTheme.colors.accent,
                )
            }
            isEmpty -> {
                EmptyPositionsState(
                    text = stringResource(R.string.No_Activity),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

private fun LazyListScope.openPositionItems(
    positions: LazyPagingItems<PerpsPositionItem>,
    onPositionClick: (PerpsPositionItem) -> Unit,
) {
    if (positions.itemCount == 0) return
    items(
        count = positions.itemCount,
        key = positions.itemKey { it.positionId },
        contentType = positions.itemContentType { "open_position" },
    ) { index ->
        val position = positions[index] ?: return@items
        val isFirst = index == 0
        val isLast = index == positions.itemCount - 1
        val shape = when {
            isFirst && isLast -> RoundedCornerShape(8.dp)
            isFirst -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            isLast -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            else -> RoundedCornerShape(0.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .groupedItemBorder(
                    backgroundColor = MixinAppTheme.colors.background,
                    borderColor = MixinAppTheme.colors.borderColor,
                    isFirst = isFirst,
                    isLast = isLast,
                )
        ) {
            OpenPositionItem(
                position = position,
                onClick = { onPositionClick(position) },
            )
        }
    }
}

private fun Modifier.groupedItemBorder(
    backgroundColor: Color,
    borderColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    cornerRadius: Dp = 8.dp,
    borderWidth: Dp = 0.8.dp,
): Modifier = this.drawBehind {
    drawRect(color = backgroundColor)
    val r = cornerRadius.toPx()
    val sw = borderWidth.toPx()
    val half = sw / 2f
    val w = size.width
    val h = size.height
    val left = half
    val top = half
    val right = w - half
    val bottom = h - half
    val path = Path()
    when {
        isFirst && isLast -> path.addRoundRect(
            RoundRect(Rect(left, top, right, bottom), CornerRadius(r, r)),
        )
        isFirst -> {
            path.moveTo(left, h)
            path.lineTo(left, top + r)
            path.arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false)
            path.lineTo(right - r, top)
            path.arcTo(Rect(right - 2 * r, top, right, top + 2 * r), 270f, 90f, false)
            path.lineTo(right, h)
        }
        isLast -> {
            path.moveTo(left, 0f)
            path.lineTo(left, bottom - r)
            path.arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 180f, -90f, false)
            path.lineTo(right - r, bottom)
            path.arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
            path.lineTo(right, 0f)
        }
        else -> {
            path.moveTo(left, 0f)
            path.lineTo(left, h)
            path.moveTo(right, 0f)
            path.lineTo(right, h)
        }
    }
    drawPath(path, color = borderColor, style = Stroke(width = sw))
}

private fun LazyListScope.closedPositionItems(
    positions: LazyPagingItems<PerpsOrderItem>,
    onPositionClick: (PerpsOrderItem) -> Unit,
) {
    items(
        count = positions.itemCount,
        key = positions.itemKey { it.orderId },
        contentType = positions.itemContentType { order ->
            if (order.orderType == PerpsOrder.TYPE_CLOSE) "close" else "open"
        },
    ) { index ->
        val order = positions[index] ?: return@items
        val isFirst = index == 0
        val isLast = index == positions.itemCount - 1
        val shape = when {
            isFirst && isLast -> RoundedCornerShape(8.dp)
            isFirst -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            isLast -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            else -> RoundedCornerShape(0.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .groupedItemBorder(
                    backgroundColor = MixinAppTheme.colors.background,
                    borderColor = MixinAppTheme.colors.borderColor,
                    isFirst = isFirst,
                    isLast = isLast,
                )
        ) {
            if (order.orderType == PerpsOrder.TYPE_CLOSE) {
                ClosedActivityItem(
                    order = order,
                    onClick = { onPositionClick(order) },
                )
            } else {
                OpenedOrderItem(
                    order = order,
                    onClick = { onPositionClick(order) },
                )
            }
        }
    }
}

@Composable
private fun TotalRealizedPnlCard(
    totalPnl: BigDecimal,
    quoteColorReversed: Boolean,
) {
    val gainColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val lossColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val pnlColor = when {
        totalPnl > BigDecimal.ZERO -> gainColor
        totalPnl < BigDecimal.ZERO -> lossColor
        else -> MixinAppTheme.colors.textAssist
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(
                backgroundColor = MixinAppTheme.colors.background,
                borderColor = MixinAppTheme.colors.borderColor,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.Total_Realized_PnL),
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatPerpsSignedUsdDecimal(totalPnl),
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            color = pnlColor,
        )
    }
}

private fun LazyListScope.appendLoadingItem(loadState: LoadState) {
    if (loadState is LoadState.Loading) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MixinAppTheme.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun TotalPositionValueCard(
    totalMargin: BigDecimal,
    totalPnl: BigDecimal,
    totalPnlPercent: BigDecimal,
    quoteColorReversed: Boolean,
) {
    val gainColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val lossColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val pnlColor = when {
        totalPnl > BigDecimal.ZERO -> gainColor
        totalPnl < BigDecimal.ZERO -> lossColor
        else -> MixinAppTheme.colors.textAssist
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(
                backgroundColor = MixinAppTheme.colors.background,
                borderColor = MixinAppTheme.colors.borderColor,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.Total_Position_Value),
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatPerpsUsdDecimal(totalMargin.abs()),
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            color = MixinAppTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatPerpsSignedUsdDecimal(totalPnl)} (${formatPerpsSignedPercent(totalPnlPercent, withSign = false)})",
            fontSize = 13.sp,
            color = pnlColor,
        )
    }
}

@Composable
private fun EmptyPositionsState(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_transaction),
            contentDescription = null,
            tint = MixinAppTheme.colors.backgroundGrayLight,
            modifier = Modifier.size(78.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
            textAlign = TextAlign.Center,
        )
        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = actionText,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.accent,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

private fun calculatePnlPercent(
    pnl: BigDecimal,
    margin: BigDecimal,
): BigDecimal {
    if (margin <= BigDecimal.ZERO) {
        return BigDecimal.ZERO
    }
    return pnl
        .divide(margin, 8, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
}

@Preview(showBackground = true)
@Composable
private fun TotalPositionValueCardPreview() {
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            TotalPositionValueCard(
                totalMargin = BigDecimal("1234.56"),
                totalPnl = BigDecimal("78.90"),
                totalPnlPercent = BigDecimal("6.39"),
                quoteColorReversed = false,
            )
            Spacer(modifier = Modifier.height(16.dp))
            EmptyPositionsState(
                text = "No Position",
                actionText = "How perps works",
                onActionClick = {},
            )
        }
    }
}
