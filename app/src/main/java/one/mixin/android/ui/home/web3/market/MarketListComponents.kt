package one.mixin.android.ui.home.web3.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Icon
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

internal val MarketChangeColumnWidth = 66.dp
internal val MarketPriceChangeGap = 4.dp
internal val MarketHorizontalPadding = 16.dp
internal val MarketLeadingIconInset = 6.dp
internal val MarketRowStartPadding = MarketHorizontalPadding - MarketLeadingIconInset
internal val MarketLeadingGap = 6.dp
internal val MarketListFavoriteIconSize = 18.dp

private val MarketHeaderPriceWidth = 96.dp
private val MarketHeaderPriceChangeMinGap = 20.dp
private val MarketSortIconRightOffset = 5.dp

internal enum class PerpetualMarketBadgeStyle {
    PERPETUAL,
    LEVERAGE,
}

@Composable
internal fun MarketHeaderSortLabels(
    period: MarketPriceChangePeriod,
    showMarketCap: Boolean,
    sortState: MarketSortState,
    onSort: (MarketSortColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            MarketSortLabel(
                text =
                    stringResource(
                        if (showMarketCap) {
                            R.string.Market_Cap
                        } else {
                            R.string.market_volume_short
                        },
                    ),
                column = MarketSortColumn.VOLUME,
                sortState = sortState,
                horizontalArrangement = Arrangement.Start,
                onSort = onSort,
            )
            Box(
                modifier = Modifier.width(MarketHeaderPriceWidth),
                contentAlignment = Alignment.CenterEnd,
            ) {
                MarketSortLabel(
                    text = stringResource(R.string.Price),
                    column = MarketSortColumn.PRICE,
                    sortState = sortState,
                    horizontalArrangement = Arrangement.End,
                    onSort = onSort,
                )
            }
            MarketSortLabel(
                text = priceChangePeriodLabel(period),
                column = MarketSortColumn.CHANGE,
                sortState = sortState,
                modifier = Modifier.offset(x = MarketSortIconRightOffset),
                horizontalArrangement = Arrangement.End,
                onSort = onSort,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val pricePlaceable = measurables[1].measure(looseConstraints)
        val changePlaceable = measurables[2].measure(looseConstraints)
        val width = constraints.maxWidth
        val changeX = (width - changePlaceable.width).coerceAtLeast(0)
        val preferredPriceRight = width - (MarketChangeColumnWidth + MarketPriceChangeGap).roundToPx()
        val fallbackPriceRight = changeX - MarketHeaderPriceChangeMinGap.roundToPx()
        val priceRight = minOf(preferredPriceRight, fallbackPriceRight)
        val priceX = (priceRight - pricePlaceable.width).coerceAtLeast(0)
        val volumePlaceable = measurables[0].measure(looseConstraints.copy(maxWidth = priceX))
        val height =
            maxOf(
                volumePlaceable.height,
                pricePlaceable.height,
                changePlaceable.height,
            ).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            volumePlaceable.placeRelative(0, (height - volumePlaceable.height) / 2)
            pricePlaceable.placeRelative(priceX, (height - pricePlaceable.height) / 2)
            changePlaceable.placeRelative(changeX, (height - changePlaceable.height) / 2)
        }
    }
}

@Composable
private fun MarketSortLabel(
    text: String,
    column: MarketSortColumn,
    sortState: MarketSortState,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onSort: (MarketSortColumn) -> Unit,
) {
    val icon =
        when {
            sortState.column != column -> R.drawable.ic_perps_sort_default
            sortState.direction == MarketSortDirection.ASCENDING -> R.drawable.ic_perps_sort_asc
            sortState.direction == MarketSortDirection.DESCENDING -> R.drawable.ic_perps_sort_desc
            else -> R.drawable.ic_perps_sort_default
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSort(column) }
                .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            textAlign = if (horizontalArrangement == Arrangement.End) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun MarketListRowFrame(
    stableId: String,
    isFavored: Boolean,
    onFavorite: ((Boolean) -> Unit) -> Unit,
    favoriteEnabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    var favoriteAnimationIntent by
        remember(stableId) {
            mutableStateOf<MarketFavoriteAnimationIntent?>(null)
        }
    var favoriteAnimationIntentId by remember(stableId) { mutableStateOf(0) }
    var completedFavoriteAnimationIntentId by remember(stableId) { mutableStateOf<Int?>(null) }
    var favoriteRequestPending by remember(stableId) { mutableStateOf(false) }
    var favoriteRequestResult by remember(stableId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(
        isFavored,
        favoriteAnimationIntent,
        favoriteRequestResult,
        completedFavoriteAnimationIntentId,
    ) {
        val intent = favoriteAnimationIntent ?: return@LaunchedEffect
        if (
            shouldClearFavoriteAnimationIntent(
                intent = intent,
                requestResult = favoriteRequestResult,
                isFavored = isFavored,
                completedIntentId = completedFavoriteAnimationIntentId,
            )
        ) {
            favoriteAnimationIntent = null
            favoriteRequestPending = false
            favoriteRequestResult = null
        }
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start = MarketRowStartPadding,
                    top = 10.dp,
                    end = MarketHorizontalPadding,
                    bottom = 14.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(enabled = favoriteEnabled && !favoriteRequestPending) {
                        if (favoriteRequestPending) return@clickable
                        favoriteAnimationIntentId += 1
                        val intent =
                            MarketFavoriteAnimationIntent(
                                id = favoriteAnimationIntentId,
                                targetFavored = !isFavored,
                            )
                        favoriteAnimationIntent = intent
                        favoriteRequestPending = true
                        favoriteRequestResult = null
                        onFavorite { success ->
                            if (favoriteAnimationIntent?.id == intent.id) {
                                favoriteRequestResult = success
                            }
                        }
                    },
        ) {
            MarketFavoriteIcon(
                isFavored = isFavored,
                unselectedIconRes = R.drawable.ic_title_favorites,
                contentDescription =
                    stringResource(
                        if (isFavored) {
                            R.string.Remove_from_Watchlist
                        } else {
                            R.string.Add_to_Watchlist
                        },
                    ),
                modifier = Modifier.size(MarketListFavoriteIconSize),
                unselectedTint = MixinAppTheme.colors.textAssist,
                animationIntent = favoriteAnimationIntent,
                onAnimationFinished = { completedFavoriteAnimationIntentId = it },
            )
        }
        Spacer(modifier = Modifier.width(MarketLeadingGap))
        content()
    }
}

@Composable
internal fun RowScope.MarketPriceText(text: String) {
    BasicText(
        text = text,
        style =
            TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            ),
        maxLines = 1,
        softWrap = false,
        autoSize =
            TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 14.sp,
                stepSize = 0.5.sp,
            ),
        modifier = Modifier.weight(1f),
    )
}

@Composable
internal fun PerpsMarketListItem(
    market: PerpsMarket,
    isFavored: Boolean,
    quoteColorReversed: Boolean,
    badgeStyle: PerpetualMarketBadgeStyle,
    onFavorite: ((Boolean) -> Unit) -> Unit,
    favoriteEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    MarketListRowFrame(
        stableId = market.marketId,
        isFavored = isFavored,
        onFavorite = onFavorite,
        favoriteEnabled = favoriteEnabled,
        onClick = onClick,
    ) {
        PerpsMarketRowContent(
            market = market,
            quoteColorReversed = quoteColorReversed,
            badgeStyle = badgeStyle,
        )
    }
}

@Composable
private fun RowScope.PerpsMarketRowContent(
    market: PerpsMarket,
    quoteColorReversed: Boolean,
    badgeStyle: PerpetualMarketBadgeStyle,
) {
    val change = market.changePercentValue()
    MarketIcon(url = market.iconUrl, size = 38.dp)
    Spacer(modifier = Modifier.width(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = market.tokenSymbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            Spacer(modifier = Modifier.width(5.dp))
            PerpetualMarketBadge(
                leverage = market.leverage,
                style = badgeStyle,
            )
        }
        Text(
            text =
                stringResource(
                    R.string.volume_label,
                    runCatching { BigDecimal(market.volume).numberFormatCompact() }.getOrDefault(market.volume),
                ),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            maxLines = 1,
        )
    }
    MarketPriceText(text = "$${market.last}")
    Spacer(modifier = Modifier.width(MarketPriceChangeGap))
    MarketChangeColumn(
        change = change,
        sparkline = null,
        quoteColorReversed = quoteColorReversed,
        fontSize = 14.sp,
        modifier = Modifier.width(MarketChangeColumnWidth),
    )
}

@Composable
internal fun MarketRecommendationCard(
    entry: MarketListEntry,
    selected: Boolean,
    quoteColorReversed: Boolean,
    perpetualBadgeStyle: PerpetualMarketBadgeStyle,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val compactTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val symbol =
        when (entry) {
            is MarketListEntry.Spot -> entry.market.symbol
            is MarketListEntry.Perpetual -> entry.market.tokenSymbol
        }
    val iconUrl =
        when (entry) {
            is MarketListEntry.Spot -> entry.market.iconUrl
            is MarketListEntry.Perpetual -> entry.market.iconUrl
        }
    val change =
        if (entry is MarketListEntry.Perpetual) {
            entry.market.changePercentValue()
        } else {
            null
        }
    val subtitle =
        when (entry) {
            is MarketListEntry.Spot -> formatSpotVolume(entry.market.marketCap)
            is MarketListEntry.Perpetual -> change?.let(::formatMarketPercent) ?: "--"
        }
    val subtitleColor =
        if (change == null) {
            MixinAppTheme.colors.textAssist
        } else if (change >= BigDecimal.ZERO) {
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MixinAppTheme.colors.borderColor, shape)
                .clip(shape)
                .selectable(
                    selected = selected,
                    role = Role.Checkbox,
                    onClick = onSelect,
                )
                .padding(vertical = 12.dp)
                .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketIcon(url = iconUrl, size = 24.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = symbol,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    style = compactTextStyle,
                    maxLines = 1,
                )
                if (entry is MarketListEntry.Perpetual) {
                    Spacer(modifier = Modifier.width(4.dp))
                    PerpetualMarketBadge(
                        leverage = entry.market.leverage,
                        style = perpetualBadgeStyle,
                    )
                }
            }
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                style = compactTextStyle,
                maxLines = 1,
            )
        }
        Icon(
            painter =
                painterResource(
                    if (selected) {
                        R.drawable.ic_asset_add_checked
                    } else {
                        R.drawable.ic_asset_add_unchecked
                    },
                ),
            contentDescription =
                stringResource(
                    if (selected) {
                        R.string.Remove_from_Watchlist
                    } else {
                        R.string.Add_to_Watchlist
                    },
                ),
            tint = Color.Unspecified,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun PerpetualMarketBadge(
    leverage: Int,
    style: PerpetualMarketBadgeStyle,
) {
    val isLeverage = style == PerpetualMarketBadgeStyle.LEVERAGE
    Text(
        text = if (isLeverage) "${leverage}x" else stringResource(R.string.Perp),
        fontSize = 12.sp,
        fontWeight = FontWeight.W400,
        color = MixinAppTheme.colors.textAssist,
        lineHeight = 14.sp,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MixinAppTheme.colors.marketBadgeBackground)
                .padding(
                    horizontal = 2.dp,
                    vertical = if (isLeverage) 1.dp else 0.dp,
                ),
    )
}

@Composable
internal fun MarketIcon(
    url: String,
    size: Dp = 34.dp,
) {
    CoilImage(
        model = url,
        placeholder = R.drawable.ic_avatar_place_holder,
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape),
    )
}

@Composable
internal fun MarketChangeColumn(
    change: BigDecimal?,
    sparkline: String?,
    quoteColorReversed: Boolean,
    fontSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier,
) {
    val isRising = change?.let { it >= BigDecimal.ZERO } ?: true
    val changeText = remember(change) { change?.let(::formatMarketPercent) ?: "--" }
    val changeColor =
        if (change == null) {
            MixinAppTheme.colors.textAssist
        } else if (isRising) {
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        }
    val sparklineColor =
        if (isRising) {
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        }
    if (sparkline.isNullOrBlank()) {
        Box(
            modifier = modifier.height(36.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            BasicText(
                text = changeText,
                style =
                    TextStyle(
                        color = changeColor,
                        fontSize = fontSize,
                        textAlign = TextAlign.End,
                    ),
                maxLines = 1,
                softWrap = false,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = fontSize,
                        stepSize = 0.5.sp,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
        ) {
            AsyncImage(
                model = sparkline,
                contentDescription = null,
                colorFilter = ColorFilter.tint(sparklineColor),
                contentScale = ContentScale.FillBounds,
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(20.dp),
            )
            BasicText(
                text = changeText,
                style =
                    TextStyle(
                        color = changeColor,
                        fontSize = fontSize,
                        textAlign = TextAlign.End,
                    ),
                maxLines = 1,
                softWrap = false,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = fontSize,
                        stepSize = 0.5.sp,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun priceChangePeriodLabel(period: MarketPriceChangePeriod): String =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> stringResource(R.string.hours_count_short, 24)
        MarketPriceChangePeriod.SEVEN_DAYS -> stringResource(R.string.days_count_short, 7)
    }

private fun formatMarketPercent(change: BigDecimal): String {
    val prefix = if (change > BigDecimal.ZERO) "+" else ""
    return "$prefix${change.numberFormat2()}%"
}

private fun formatSpotVolume(value: String): String =
    runCatching {
        "${Fiats.getSymbol()}${BigDecimal(value).multiply(BigDecimal(Fiats.getRate())).numberFormatCompact()}"
    }.getOrDefault(value)
