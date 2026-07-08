package one.mixin.android.ui.wallet

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import kotlin.math.hypot

private val MarketPerpsButtonVerticalPadding = 11.dp

@Composable
fun MarketPerpsAction(
    onLongClick: () -> Unit,
    onShortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val longColor = if (quoteColorReversed) MixinAppTheme.colors.marketRed else MixinAppTheme.colors.marketGreen
    val shortColor = if (quoteColorReversed) MixinAppTheme.colors.marketGreen else MixinAppTheme.colors.marketRed

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        val buttonWidth = (maxWidth + 10.dp) / 2f
        MarketPerpsButton(
            text = stringResource(R.string.Long),
            color = longColor,
            side = MarketPerpsButtonSide.LONG,
            onClick = onLongClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(buttonWidth),
        )
        MarketPerpsButton(
            text = stringResource(R.string.Short),
            color = shortColor,
            side = MarketPerpsButtonSide.SHORT,
            onClick = onShortClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(buttonWidth),
        )
    }
}

@Composable
private fun MarketPerpsButton(
    text: String,
    color: Color,
    side: MarketPerpsButtonSide,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember(side) { MarketPerpsButtonShape(side) }
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(vertical = MarketPerpsButtonVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (side == MarketPerpsButtonSide.LONG) {
                        R.drawable.ic_market_perps_long
                    } else {
                        R.drawable.ic_market_perps_short
                    }
                ),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private enum class MarketPerpsButtonSide {
    LONG,
    SHORT,
}

private class MarketPerpsButtonShape(
    private val side: MarketPerpsButtonSide,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { 8.dp.toPx() }.coerceAtMost(size.height / 2)
        val slant = with(density) { 18.dp.toPx() }.coerceAtMost(size.width / 3)
        val slantRadius = radius.coerceAtMost(slant / 2)
        val diagonalLength = hypot(slant.toDouble(), size.height.toDouble()).toFloat()
        val diagonalInsetX = slantRadius * slant / diagonalLength
        val diagonalInsetY = slantRadius * size.height / diagonalLength
        val path = Path().apply {
            if (side == MarketPerpsButtonSide.LONG) {
                moveTo(radius, 0f)
                lineTo(size.width - slantRadius, 0f)
                quadraticTo(size.width, 0f, size.width - diagonalInsetX, diagonalInsetY)
                lineTo(size.width - slant + diagonalInsetX, size.height - diagonalInsetY)
                quadraticTo(size.width - slant, size.height, size.width - slant - slantRadius, size.height)
                lineTo(radius, size.height)
                quadraticTo(0f, size.height, 0f, size.height - radius)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
            } else {
                moveTo(slant + slantRadius, 0f)
                lineTo(size.width - radius, 0f)
                quadraticTo(size.width, 0f, size.width, radius)
                lineTo(size.width, size.height - radius)
                quadraticTo(size.width, size.height, size.width - radius, size.height)
                lineTo(slantRadius, size.height)
                quadraticTo(0f, size.height, diagonalInsetX, size.height - diagonalInsetY)
                lineTo(slant - diagonalInsetX, diagonalInsetY)
                quadraticTo(slant, 0f, slant + slantRadius, 0f)
            }
            close()
        }
        return Outline.Generic(path)
    }
}
