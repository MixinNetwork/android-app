package one.mixin.android.ui.home.web3.trade

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.home.web3.market.MarketPageMapper
import one.mixin.android.ui.home.web3.market.MarketPriceChangePeriod
import one.mixin.android.ui.home.web3.market.animateScrollToCenteredItem
import one.mixin.android.ui.home.web3.market.changePercent
import one.mixin.android.ui.home.web3.market.formatMarketListPrice
import one.mixin.android.ui.home.web3.market.formatMarketPercent
import one.mixin.android.ui.home.web3.market.formatSpotVolume
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private val RecommendedMarketIconSize = 42.dp
private val RecommendedMarketCardCornerRadius = 8.dp
private val RecommendedMarketCardBorderWidth = 0.8.dp

enum class SwapRecommendedMarketType {
    Stocks,
    Trending,
    TopGainers,
    TopLosers,
}

internal enum class SwapRecommendedCategory(
    @param:StringRes val titleRes: Int,
) {
    TRENDING(R.string.Trending),
    STOCKS(R.string.Stocks),
    TOP_GAINERS(R.string.top_gainers),
    TOP_LOSERS(R.string.top_losers),
}

internal data class SwapRecommendedMarketSection(
    val category: SwapRecommendedCategory,
    val markets: List<MarketItem>,
)

internal class SwapRecommendedMarketState(
    val sections: List<SwapRecommendedMarketSection>,
    val activeSection: SwapRecommendedMarketSection,
    val tabListState: LazyListState,
    val fiatSymbol: String,
    val fiatRate: Double,
    val onCategorySelected: (SwapRecommendedCategory) -> Unit,
)

/**
 * Builds categorized recommended market sections aligned with [one.mixin.android.ui.home.web3.market.MarketPageViewModel].
 */
internal fun buildSwapRecommendedMarketSections(
    trendingMarkets: List<MarketItem>,
    stockMarkets: List<MarketItem>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    allMarkets: List<MarketItem> = emptyList(),
    priceChangePeriod: MarketPriceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
): List<SwapRecommendedMarketSection> {
    val stockCoinIds: Set<String> = stockMarkets.mapTo(mutableSetOf()) { market -> market.coinId }

    val spotTrending = MarketPageMapper
        .spotMarkets(markets = trendingMarkets, fallbackMarkets = allMarkets)
        .filter { market -> market.coinId !in stockCoinIds }
    val spotGainers = MarketPageMapper
        .spotMarkets(markets = topGainerMarkets, fallbackMarkets = allMarkets)
        .filter { market -> market.coinId !in stockCoinIds }
        .sortedByDescending { market -> market.changePercent(priceChangePeriod) ?: BigDecimal.ZERO }
    val spotLosers = MarketPageMapper
        .spotMarkets(markets = topLoserMarkets, fallbackMarkets = allMarkets)
        .filter { market -> market.coinId !in stockCoinIds }
        .sortedBy { market -> market.changePercent(priceChangePeriod) ?: BigDecimal.ZERO }

    return listOf(
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.TRENDING,
            markets = spotTrending,
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.STOCKS,
            markets = stockMarkets,
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.TOP_GAINERS,
            markets = spotGainers,
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.TOP_LOSERS,
            markets = spotLosers,
        ),
    ).filter { section -> section.markets.isNotEmpty() }
}

@Composable
internal fun rememberSwapRecommendedMarketState(
    stockMarkets: List<MarketItem>,
    trendingMarkets: List<MarketItem>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    allMarkets: List<MarketItem> = emptyList(),
    priceChangePeriod: MarketPriceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
): SwapRecommendedMarketState? {
    val sections =
        remember(
            trendingMarkets,
            stockMarkets,
            topGainerMarkets,
            topLoserMarkets,
            allMarkets,
            priceChangePeriod,
        ) {
            buildSwapRecommendedMarketSections(
                trendingMarkets = trendingMarkets,
                stockMarkets = stockMarkets,
                topGainerMarkets = topGainerMarkets,
                topLoserMarkets = topLoserMarkets,
                allMarkets = allMarkets,
                priceChangePeriod = priceChangePeriod,
            )
        }

    if (sections.isEmpty()) return null

    var selectedCategory by rememberSaveable { mutableStateOf(sections.first().category) }
    val activeSection = sections.firstOrNull { it.category == selectedCategory } ?: sections.first()
    val tabListState = rememberLazyListState()
    val selectedIndex = sections.indexOf(activeSection)
    LaunchedEffect(activeSection.category, selectedIndex) {
        tabListState.animateScrollToCenteredItem(selectedIndex)
    }
    return SwapRecommendedMarketState(
        sections = sections,
        activeSection = activeSection,
        tabListState = tabListState,
        fiatSymbol = Fiats.getSymbol(),
        fiatRate = Fiats.getRate(),
        onCategorySelected = { selectedCategory = it },
    )
}

internal fun LazyListScope.swapRecommendedMarketCardItems(
    state: SwapRecommendedMarketState?,
    priceChangePeriod: MarketPriceChangePeriod,
    quoteColorReversed: Boolean,
    onMarketClick: (MarketItem) -> Unit,
) {
    if (state == null) return
    val activeSection = state.activeSection

    item(key = "recommended_markets_header") {
        val backgroundColor = MixinAppTheme.colors.background
        val borderColor = MixinAppTheme.colors.borderColor
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = RecommendedMarketCardCornerRadius,
                            topEnd = RecommendedMarketCardCornerRadius,
                        ),
                    )
                    .background(backgroundColor)
                    .recommendedCardBorder(RecommendedCardEdge.TOP, borderColor)
                    .padding(top = 20.dp, bottom = 16.dp),
            ) {
                SwapRecommendedCategoryTabs(
                    sections = state.sections,
                    activeCategory = activeSection.category,
                    tabListState = state.tabListState,
                    onCategorySelected = state.onCategorySelected,
                )
            }
        }
    }

    itemsIndexed(
        items = activeSection.markets,
        key = { index, market -> "recommended_market_${activeSection.category.name}_${market.coinId}_$index" },
    ) { index, market ->
        val backgroundColor = MixinAppTheme.colors.background
        val borderColor = MixinAppTheme.colors.borderColor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(backgroundColor)
                .recommendedCardBorder(RecommendedCardEdge.MIDDLE, borderColor)
                .padding(start = 16.dp, end = 16.dp, top = if (index > 0) 20.dp else 0.dp),
        ) {
            SwapRecommendedMarketRow(
                market = market,
                fiatSymbol = state.fiatSymbol,
                fiatRate = state.fiatRate,
                quoteColorReversed = quoteColorReversed,
                priceChangePeriod = priceChangePeriod,
                onClick = { onMarketClick(market) },
            )
        }
    }

    item(key = "recommended_markets_footer") {
        val backgroundColor = MixinAppTheme.colors.background
        val borderColor = MixinAppTheme.colors.borderColor
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = RecommendedMarketCardCornerRadius,
                            bottomEnd = RecommendedMarketCardCornerRadius,
                        ),
                    )
                    .background(backgroundColor)
                    .recommendedCardBorder(RecommendedCardEdge.BOTTOM, borderColor)
                    .height(20.dp),
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SwapRecommendedMarketCards(
    stockMarkets: List<MarketItem>,
    trendingMarkets: List<MarketItem>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    onMarketClick: (MarketItem) -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onViewAllClick: (SwapRecommendedMarketType) -> Unit = {},
    modifier: Modifier = Modifier,
    allMarkets: List<MarketItem> = emptyList(),
    priceChangePeriod: MarketPriceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
    quoteColorReversed: Boolean =
        LocalContext.current.defaultSharedPreferences
            .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false),
) {
    val state =
        rememberSwapRecommendedMarketState(
            stockMarkets = stockMarkets,
            trendingMarkets = trendingMarkets,
            topGainerMarkets = topGainerMarkets,
            topLoserMarkets = topLoserMarkets,
            allMarkets = allMarkets,
            priceChangePeriod = priceChangePeriod,
        ) ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RecommendedMarketCardCornerRadius))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(vertical = 20.dp),
    ) {
        SwapRecommendedCategoryTabs(
            sections = state.sections,
            activeCategory = state.activeSection.category,
            tabListState = state.tabListState,
            onCategorySelected = state.onCategorySelected,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.activeSection.markets.forEach { market ->
                SwapRecommendedMarketRow(
                    market = market,
                    fiatSymbol = state.fiatSymbol,
                    fiatRate = state.fiatRate,
                    quoteColorReversed = quoteColorReversed,
                    priceChangePeriod = priceChangePeriod,
                    onClick = { onMarketClick(market) },
                )
            }
        }
    }
}

@Composable
private fun SwapRecommendedCategoryTabs(
    sections: List<SwapRecommendedMarketSection>,
    activeCategory: SwapRecommendedCategory,
    tabListState: LazyListState,
    onCategorySelected: (SwapRecommendedCategory) -> Unit,
) {
    LazyRow(
        state = tabListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(sections, key = { it.category }) { section ->
            val isSelected = section.category == activeCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) {
                            MixinAppTheme.colors.accent.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onCategorySelected(section.category) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(section.category.titleRes),
                    color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private enum class RecommendedCardEdge {
    TOP,
    MIDDLE,
    BOTTOM,
}

private fun Modifier.recommendedCardBorder(
    edge: RecommendedCardEdge,
    borderColor: Color,
): Modifier =
    drawBehind {
        val strokePx = RecommendedMarketCardBorderWidth.toPx()
        val halfStroke = strokePx / 2f
        val leftX = halfStroke
        val rightX = size.width - halfStroke
        if (edge == RecommendedCardEdge.MIDDLE) {
            drawLine(
                color = borderColor,
                start = Offset(leftX, 0f),
                end = Offset(leftX, size.height),
                strokeWidth = strokePx,
            )
            drawLine(
                color = borderColor,
                start = Offset(rightX, 0f),
                end = Offset(rightX, size.height),
                strokeWidth = strokePx,
            )
            return@drawBehind
        }
        val cornerPx = (RecommendedMarketCardCornerRadius.toPx() - halfStroke).coerceAtLeast(0f)
        val path =
            Path().apply {
                when (edge) {
                    RecommendedCardEdge.TOP -> {
                        moveTo(leftX, size.height)
                        lineTo(leftX, halfStroke + cornerPx)
                        arcTo(
                            rect = Rect(leftX, halfStroke, leftX + cornerPx * 2f, halfStroke + cornerPx * 2f),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(rightX - cornerPx, halfStroke)
                        arcTo(
                            rect = Rect(rightX - cornerPx * 2f, halfStroke, rightX, halfStroke + cornerPx * 2f),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(rightX, size.height)
                    }

                    RecommendedCardEdge.BOTTOM -> {
                        moveTo(leftX, 0f)
                        lineTo(leftX, size.height - halfStroke - cornerPx)
                        arcTo(
                            rect = Rect(leftX, size.height - halfStroke - cornerPx * 2f, leftX + cornerPx * 2f, size.height - halfStroke),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false,
                        )
                        lineTo(rightX - cornerPx, size.height - halfStroke)
                        arcTo(
                            rect = Rect(rightX - cornerPx * 2f, size.height - halfStroke - cornerPx * 2f, rightX, size.height - halfStroke),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false,
                        )
                        lineTo(rightX, 0f)
                    }

                    RecommendedCardEdge.MIDDLE -> Unit
                }
            }
        drawPath(path = path, color = borderColor, style = Stroke(width = strokePx))
    }

@Composable
private fun SwapRecommendedMarketRow(
    market: MarketItem,
    fiatSymbol: String,
    fiatRate: Double,
    quoteColorReversed: Boolean,
    priceChangePeriod: MarketPriceChangePeriod,
    onClick: () -> Unit,
) {
    val change = market.changePercent(priceChangePeriod)
    val isRising = change?.let { it >= BigDecimal.ZERO } ?: true
    val changeText = change?.let(::formatMarketPercent) ?: "--"
    val changeColor =
        if (change == null) {
            MixinAppTheme.colors.textAssist
        } else if (isRising) {
            if (quoteColorReversed) MixinAppTheme.colors.marketRed else MixinAppTheme.colors.marketGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.marketGreen else MixinAppTheme.colors.marketRed
        }
    val priceText = formatMarketListPrice(market.currentPrice, fiatSymbol, fiatRate)
    val volumeText = stringResource(
        R.string.volume_label,
        formatSpotVolume(market.totalVolume, fiatSymbol, fiatRate),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = market.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(RecommendedMarketIconSize)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = market.symbol,
                    modifier = Modifier.weight(1f),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = priceText,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = volumeText,
                    modifier = Modifier.weight(1f),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = changeText,
                    color = changeColor,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

internal fun formatRecommendedMarketFiatPrice(
    value: BigDecimal,
    fiatSymbol: String,
): String? {
    if (value <= BigDecimal.ZERO) return null
    if (value < BigDecimal("0.0001")) return "<${fiatSymbol}0.0001"
    if (value >= BigDecimal("1000")) return "$fiatSymbol${formatRecommendedMarketPriceCompact(value)}"
    val pattern = if (value >= BigDecimal.ONE) ",##0.00" else ",##0.0000"
    val formatted = DecimalFormat(pattern).apply {
        roundingMode = RoundingMode.DOWN
    }.format(value)
    return "$fiatSymbol$formatted"
}

private fun formatRecommendedMarketPriceCompact(value: BigDecimal): String {
    val thousands = value.divide(BigDecimal("1000"), 2, RoundingMode.DOWN)
    return "${thousands.stripTrailingZeros().toPlainString()}K"
}

internal fun formatRecommendedMarketSignedPercent(value: BigDecimal): String {
    val percentText = "${formatRecommendedMarketPercentDecimal(value)}%"
    return if (value >= BigDecimal.ZERO) {
        "+$percentText"
    } else {
        "-$percentText"
    }
}

private fun formatRecommendedMarketPercentDecimal(value: BigDecimal): String {
    val safeValue = value.abs()
    if (safeValue >= BigDecimal("1000")) {
        val thousands = safeValue.divide(BigDecimal("1000"), 1, RoundingMode.FLOOR)
        return if (thousands.stripTrailingZeros().scale() <= 0) {
            "${thousands.toBigInteger()}K"
        } else {
            "${thousands.stripTrailingZeros().toPlainString()}K"
        }
    }
    return safeValue.numberFormat2()
}
