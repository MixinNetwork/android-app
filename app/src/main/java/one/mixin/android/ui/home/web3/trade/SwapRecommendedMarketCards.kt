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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
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

internal const val SWAP_RECOMMENDED_MARKET_LIMIT = 8
private val RecommendedMarketIconSize = 42.dp

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
    limit: Int = SWAP_RECOMMENDED_MARKET_LIMIT,
): List<SwapRecommendedMarketSection> {
    val safeLimit = limit.coerceAtLeast(0)
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
            markets = spotTrending.take(safeLimit),
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.STOCKS,
            markets = stockMarkets.take(safeLimit),
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.TOP_GAINERS,
            markets = spotGainers.take(safeLimit),
        ),
        SwapRecommendedMarketSection(
            category = SwapRecommendedCategory.TOP_LOSERS,
            markets = spotLosers.take(safeLimit),
        ),
    ).filter { section -> section.markets.isNotEmpty() }
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
                limit = SWAP_RECOMMENDED_MARKET_LIMIT,
            )
        }

    if (sections.isEmpty()) return

    var selectedCategory by rememberSaveable { mutableStateOf(sections.first().category) }
    val activeSection = sections.firstOrNull { it.category == selectedCategory } ?: sections.first()
    val tabListState = rememberLazyListState()
    val selectedIndex = sections.indexOf(activeSection)
    LaunchedEffect(activeSection.category, selectedIndex) {
        tabListState.animateScrollToCenteredItem(selectedIndex)
    }
    val fiatSymbol = Fiats.getSymbol()
    val fiatRate = Fiats.getRate()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        LazyRow(
            state = tabListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(sections, key = { it.category }) { section ->
                val isSelected = section.category == activeSection.category
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
                        .clickable { selectedCategory = section.category }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(section.category.titleRes),
                        color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            activeSection.markets.forEach { market ->
                SwapRecommendedMarketRow(
                    market = market,
                    fiatSymbol = fiatSymbol,
                    fiatRate = fiatRate,
                    quoteColorReversed = quoteColorReversed,
                    priceChangePeriod = priceChangePeriod,
                    onClick = { onMarketClick(market) },
                )
            }
        }
    }
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = priceText,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = changeText,
                    color = changeColor,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
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
