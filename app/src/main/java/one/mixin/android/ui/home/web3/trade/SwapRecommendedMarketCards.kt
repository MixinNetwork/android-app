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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

private const val RECOMMENDED_MARKET_COLUMNS = 4
private const val RECOMMENDED_MARKET_LIMIT = 8

enum class SwapRecommendedMarketType {
    Trending,
    TopGainers,
    TopLosers,
}

private data class RecommendedMarketUiItem(
    val symbol: String,
    val iconUrl: String?,
    val price: String?,
    val changePercent: String?,
    val isPositive: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun SwapRecommendedMarketCards(
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
            items = trendingMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
        RecommendedMarketCardData(
            titleRes = R.string.top_gainers,
            type = SwapRecommendedMarketType.TopGainers,
            items = topGainerMarkets.take(RECOMMENDED_MARKET_LIMIT).map { market ->
                market.toRecommendedMarketUiItem { onMarketClick(market) }
            },
        ),
        RecommendedMarketCardData(
            titleRes = R.string.top_losers,
            type = SwapRecommendedMarketType.TopLosers,
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
        ) {
            Text(
                text = stringResource(card.titleRes),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (card.showViewAll) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        if (card.showViewAll) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.view_all),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.accent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onViewAllClick)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RecommendedMarketGridItem(
    item: RecommendedMarketUiItem,
    risingColor: Color,
    fallingColor: Color,
) {
    val compactTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            CoilImage(
                model = item.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            item.changePercent?.let { changePercent ->
                Text(
                    text = changePercent,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    style = compactTextStyle,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            color = if (item.isPositive) risingColor else fallingColor,
                            shape = RoundedCornerShape(3.dp),
                        )
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.symbol,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            style = compactTextStyle,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        item.price?.let { price ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = price,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                style = compactTextStyle,
                color = MixinAppTheme.colors.textAssist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
        isPositive = changeValue?.let { it >= BigDecimal.ZERO } ?: true,
        onClick = onClick,
    )
}

private fun String?.formatFiatPrice(): String? {
    val value = this?.toBigDecimalOrNull() ?: return null
    if (value <= BigDecimal.ZERO) return null
    return "${Fiats.getSymbol()}${value.multiply(BigDecimal(Fiats.getRate())).priceFormat()}"
}

private fun BigDecimal?.formatSignedPercent(): String? {
    val value = this ?: return null
    val sign = if (value >= BigDecimal.ZERO) "+" else ""
    return "$sign${value.numberFormat2()}%"
}
