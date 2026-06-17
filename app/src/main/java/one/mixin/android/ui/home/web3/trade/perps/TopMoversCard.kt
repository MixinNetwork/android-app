package one.mixin.android.ui.home.web3.trade.perps

import android.graphics.BlurMaskFilter
import android.graphics.Paint
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal

private const val TOP_MOVERS_COLUMNS = 4

@Composable
fun TopMoversCard(
    markets: List<PerpsMarket>,
    quoteColorReversed: Boolean,
    onViewAllClick: () -> Unit,
    onMarketItemClick: (PerpsMarket) -> Unit,
    titleRes: Int = R.string.perps_top_movers,
) {
    if (markets.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAllClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(titleRes),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_gray_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp).offset(x = 4.dp),
        )
    }

    val rows = markets.chunked(TOP_MOVERS_COLUMNS)
    rows.forEachIndexed { index, rowMarkets ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            rowMarkets.forEach { market ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    TopMoverGridItem(
                        market = market,
                        quoteColorReversed = quoteColorReversed,
                        onClick = { onMarketItemClick(market) },
                    )
                }
            }
            repeat(TOP_MOVERS_COLUMNS - rowMarkets.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TopMoverGridItem(
    market: PerpsMarket,
    quoteColorReversed: Boolean,
    onClick: () -> Unit,
) {
    val changePercent = market.changePercent()
    val isPositive = changePercent >= BigDecimal.ZERO
    val changeColor = if (isPositive) {
        if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val changeText = formatPerpsSignedPercent(changePercent)

    Column(
        modifier = Modifier
            .offset(y = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(width = 42.dp, height = 46.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            CoilImage(
                model = market.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
            )
            BasicText(
                text = "${market.leverage}x",
                style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = 12.sp,
                    stepSize = 0.5.sp
                ),
                modifier = Modifier
                    .offset(y = 32.dp)
                    .topMoverLeverageShadow()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MixinAppTheme.colors.background)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = market.tokenSymbol,
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
                stepSize = 0.5.sp
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicText(
            text = changeText,
            style = TextStyle(
                fontSize = 13.sp,
                color = changeColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 13.sp,
                stepSize = 0.5.sp
            ),
        )
    }
}

private fun Modifier.topMoverLeverageShadow(): Modifier = drawBehind {
    val blur = 2.dp.toPx()
    val offsetY = (-1).dp.toPx()
    val radius = 4.dp.toPx()
    val paint = Paint().apply {
        color = Color.Black.copy(alpha = 0.04f).toArgb()
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
        0f,
        offsetY,
        size.width,
        size.height + offsetY,
        radius,
        radius,
        paint,
    )
}
