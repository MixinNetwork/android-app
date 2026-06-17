package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private const val RECOMMENDED_MARKET_COLUMNS = 4
private const val RECOMMENDED_MARKET_LIMIT = 8
private val RecommendedMarketIconSize = 42.dp
private val RecommendedMarketChangeHeight = 16.dp
private val RecommendedMarketChangeMinWidth = 44.dp
private val RecommendedMarketChangeMaxWidth = 52.dp
private val RecommendedMarketSymbolHeight = 18.dp
private val RecommendedMarketPriceHeight = 16.dp

enum class SwapRecommendedMarketType {
    Stocks,
    Trending,
    TopGainers,
    TopLosers,
}

private data class RecommendedMarketUiItem(
    val symbol: String,
    val iconUrl: String?,
    val price: String?,
    val changePercent: String?,
    val shrinkChangePercent: Boolean,
    val isPositive: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun SwapRecommendedMarketCards(
    stockMarkets: List<MarketItem>,
    trendingMarkets: List<MarketItem>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    onMarketClick: (MarketItem) -> Unit,
    onViewAllClick: (SwapRecommendedMarketType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = listOf(
        RecommendedMarketCardData(
            titleRes = R.string.Trending,
            type = SwapRecommendedMarketType.Trending,
            showViewAll = false,
            items = trendingMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
        RecommendedMarketCardData(
            titleRes = R.string.Stocks,
            type = SwapRecommendedMarketType.Stocks,
            showViewAll = false,
            items = stockMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
        RecommendedMarketCardData(
            titleRes = R.string.top_gainers,
            type = SwapRecommendedMarketType.TopGainers,
            showViewAll = false,
            items = topGainerMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
        RecommendedMarketCardData(
            titleRes = R.string.top_losers,
            type = SwapRecommendedMarketType.TopLosers,
            showViewAll = false,
            items = topLoserMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
    ).filter { it.items.isNotEmpty() }

    if (cards.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        cards.forEachIndexed { index, card ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            RecommendedMarketCard(
                card = card,
                onViewAllClick = { onViewAllClick(card.type) },
            )
        }
    }
}

private data class RecommendedMarketCardData(
    val titleRes: Int,
    val type: SwapRecommendedMarketType,
    val showViewAll: Boolean = false,
    val items: List<RecommendedMarketUiItem>,
)

@Composable
private fun RecommendedMarketCard(
    card: RecommendedMarketCardData,
    onViewAllClick: () -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
            .padding(vertical = 16.dp),
    ) {
        val headerModifier = if (card.showViewAll) {
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAllClick)
                .padding(horizontal = 16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        }

        Row(
            modifier = headerModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(card.titleRes),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            if (card.showViewAll) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_gray_right),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp).offset(x = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val rows = card.items.chunked(RECOMMENDED_MARKET_COLUMNS)
        rows.forEachIndexed { index, rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        RecommendedMarketGridItem(
                            item = item,
                            risingColor = risingColor,
                            fallingColor = fallingColor,
                        )
                    }
                }
                repeat(RECOMMENDED_MARKET_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (index != rows.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RecommendedMarketGridItem(
    item: RecommendedMarketUiItem,
    risingColor: Color,
    fallingColor: Color,
) {
    Column(
        modifier = Modifier
            .offset(y = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(width = RecommendedMarketChangeMaxWidth, height = 46.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            CoilImage(
                model = item.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(RecommendedMarketIconSize)
                    .clip(CircleShape),
            )
            item.changePercent?.let { changePercent ->
                val changePercentFontSize = if (item.shrinkChangePercent) 10.sp else 12.sp
                Box(
                    modifier = Modifier
                        .offset(y = 32.dp)
                        .widthIn(min = RecommendedMarketChangeMinWidth, max = RecommendedMarketChangeMaxWidth)
                        .height(RecommendedMarketChangeHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (item.isPositive) risingColor else fallingColor)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = changePercent,
                        style = TextStyle(
                            fontSize = changePercentFontSize,
                            lineHeight = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = changePercentFontSize,
                            stepSize = 0.5.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RecommendedMarketSymbolHeight),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = item.symbol,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = 14.sp,
                    stepSize = 0.5.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RecommendedMarketPriceHeight),
            contentAlignment = Alignment.Center,
        ) {
            item.price?.let { price ->
                BasicText(
                    text = price,
                    style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 13.sp,
                        stepSize = 0.5.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun MarketItem.toRecommendedMarketUiItem(onClick: () -> Unit): RecommendedMarketUiItem {
    val changeValue = priceChangePercentage24H.toBigDecimalOrNull()
    return RecommendedMarketUiItem(
        symbol = symbol,
        iconUrl = iconUrl,
        price = currentPrice.formatFiatPrice(),
        changePercent = changeValue.formatSignedPercent(),
        shrinkChangePercent = changeValue?.abs()?.let { it >= BigDecimal("100") } ?: false,
        isPositive = changeValue?.let { it >= BigDecimal.ZERO } ?: true,
        onClick = onClick,
    )
}

private fun String?.formatFiatPrice(): String? {
    val value = this?.toBigDecimalOrNull() ?: return null
    return formatRecommendedMarketFiatPrice(
        value = value.multiply(BigDecimal(Fiats.getRate())),
        fiatSymbol = Fiats.getSymbol(),
    )
}

internal fun formatRecommendedMarketFiatPrice(
    value: BigDecimal,
    fiatSymbol: String,
): String? {
    if (value <= BigDecimal.ZERO) return null
    if (value < BigDecimal("0.0001")) return "<${fiatSymbol}0.0001"
    val pattern = if (value >= BigDecimal.ONE) ",##0.00" else ",##0.0000"
    val formatted = DecimalFormat(pattern).apply {
        roundingMode = RoundingMode.DOWN
    }.format(value)
    return "$fiatSymbol$formatted"
}

private fun BigDecimal?.formatSignedPercent(): String? {
    val value = this ?: return null
    val sign = if (value >= BigDecimal.ZERO) "+" else ""
    return "$sign${value.numberFormat2()}%"
}
