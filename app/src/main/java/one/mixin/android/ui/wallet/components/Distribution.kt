package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import kotlin.math.min

@Composable
fun Distribution(distributions: List<AssetDistribution>, destination: WalletDestination?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val topThree = distributions
        when {
            topThree.isEmpty() -> {
                if (destination == null)
                    LegendItem(
                        percentage = 0f,
                        color = MixinAppTheme.colors.backgroundWindow,
                        currency = "",
                        modifier = Modifier.weight(1f)
                    )
                else
                    LegendAssetItem(
                        percentage = 0f,
                        icons = emptyList(),
                        count = 0,
                        modifier = Modifier.weight(1f)
                    )
            }

            topThree.size == 1 -> {
                if (destination == null)
                    LegendItem(
                        percentage = 1f,
                        color = MixinAppTheme.colors.walletBlue,
                        currency = topThree[0].symbol,
                        modifier = Modifier.weight(1f)
                    )
                else
                    LegendAssetItem(
                        percentage = 1f,
                        icons = topThree[0].icons,
                        count = topThree[0].count,
                        modifier = Modifier.weight(1f)
                    )
            }

            destination == null -> {
                topThree.forEachIndexed { index, asset ->
                    when (index) {
                        0 -> LegendItem(
                            percentage = asset.percentage,
                            color = MixinAppTheme.colors.walletBlue,
                            currency = asset.symbol,
                            modifier = Modifier.weight(1f)
                        )

                        1 -> LegendItem(
                            percentage = asset.percentage,
                            color = MixinAppTheme.colors.walletPurple,
                            currency = asset.symbol,
                            modifier = Modifier.weight(1f)
                        )

                        2 -> {
                            LegendItem(
                                percentage = asset.percentage,
                                color = MixinAppTheme.colors.walletYellow,
                                currency = asset.symbol,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            else -> {
                topThree.forEachIndexed { index, asset ->
                    when (index) {
                        0 -> LegendAssetItem(
                            percentage = asset.percentage,
                            icons = asset.icons,
                            count = asset.count,
                            modifier = Modifier.weight(1f)
                        )

                        1 -> LegendAssetItem(
                            percentage = asset.percentage,
                            icons = asset.icons,
                            count = asset.count,
                            modifier = Modifier.weight(1f)
                        )

                        2 -> {
                            LegendAssetItem(
                                percentage = asset.percentage,
                                icons = asset.icons,
                                count = asset.count,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        repeat(3 - topThree.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LegendItem(modifier: Modifier, percentage: Float, color: Color, currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(3.dp, 9.dp)
                .background(color, RoundedCornerShape(1.5.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${(percentage * 100).toInt()}% $currency",
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}
@Composable
private fun LegendAssetItem(
    modifier: Modifier,
    percentage: Float,
    icons: List<String>,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        when {
            icons.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MixinAppTheme.colors.backgroundWindow)
                )
            }
            count == 1 -> {
                CoilImage(
                    model = icons[0],
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )
            }
            else -> {
                Box(
                    modifier = Modifier.width(
                        when {
                            count <= 1 -> 18.dp
                            count == 2 -> 30.dp
                            count == 3 -> 42.dp
                            else -> 54.dp
                        }
                    )
                ) {
                    val displayIcons = when {
                        count <= 1 -> icons.take(1)
                        count == 2 -> icons.take(min(2, icons.size))
                        count == 3 -> icons.take(min(3, icons.size))
                        else -> icons.take(min(3, icons.size))
                    }

                    displayIcons.forEachIndexed { index, icon ->
                        CoilImage(
                            model = icon,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = if (index == 0) 0.dp else (12 * index).dp)
                                .zIndex(displayIcons.size - index.toFloat())
                                .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                                .clip(CircleShape),
                            placeholder = R.drawable.ic_avatar_place_holder,
                        )
                    }

                    if (count > displayIcons.size) {
                        Surface(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = if (displayIcons.isEmpty()) 0.dp else (12 * displayIcons.size).dp)
                                .zIndex(0f)
                                .clip(CircleShape),
                            color = MixinAppTheme.colors.backgroundWindow
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Text(
                                    text = if (count - displayIcons.size >= 99) {
                                        "+99"
                                    } else {
                                        "${count - displayIcons.size}"
                                    },
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 6.sp,
                                    lineHeight = 6.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        Text(
            text = "${(percentage * 100).toInt()}%",
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}