package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
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
    Stocks,
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
    stockTokens: List<SwapToken>,
    topGainerMarkets: List<MarketItem>,
    topLoserMarkets: List<MarketItem>,
    onMarketClick: (MarketItem) -> Unit,
    onStockClick: (SwapToken) -> Unit,
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
            titleRes = R.string.Stocks,
            type = SwapRecommendedMarketType.Stocks,
            items = stockTokens.take(RECOMMENDED_MARKET_LIMIT).map { token ->
                token.toRecommendedMarketUiItem { onStockClick(token) }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAllClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(card.titleRes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.view_all),
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier.size(16.dp),
            )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        CoilImage(
            model = item.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.symbol,
            fontSize = 14.sp,
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
                fontSize = 11.sp,
                color = MixinAppTheme.colors.textAssist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item.changePercent?.let { changePercent ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = changePercent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.isPositive) risingColor else fallingColor,
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

private fun SwapToken.toRecommendedMarketUiItem(onClick: () -> Unit): RecommendedMarketUiItem {
    val changeValue = changeUsd?.toBigDecimalOrNull()
    return RecommendedMarketUiItem(
        symbol = symbol,
        iconUrl = icon,
        price = price.formatFiatPrice(),
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
