package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
                        currency = "",
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
                        currency = topThree[0].symbol,
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
                            val remainingPercentage =
                                1f - topThree[0].percentage - topThree[1].percentage
                            LegendItem(
                                percentage = remainingPercentage,
                                color = MixinAppTheme.colors.walletYellow,
                                currency = if (distributions.size > 3) "其他" else asset.symbol,
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
                            currency = asset.symbol,
                            count = asset.count,
                            modifier = Modifier.weight(1f)
                        )

                        1 -> LegendAssetItem(
                            percentage = asset.percentage,
                            icons = asset.icons,
                            currency = asset.symbol,
                            count = asset.count,
                            modifier = Modifier.weight(1f)
                        )

                        2 -> {
                            val remainingPercentage =
                                1f - topThree[0].percentage - topThree[1].percentage
                            LegendAssetItem(
                                percentage = remainingPercentage,
                                icons = asset.icons,
                                currency = asset.symbol,
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
            text = "${(percentage * 100).toInt()}%",
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
    currency: String,
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
                Box(modifier = Modifier.wrapContentSize()) {
                    val displayIcons = when {
                        count <= 1 -> icons.take(1)
                        count == 2 -> icons.take(2)
                        count == 3 -> icons.take(3)
                        else -> icons.take(2)
                    }
                    
                    displayIcons.forEachIndexed { index, icon ->
                        CoilImage(
                            model = icon,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .zIndex(displayIcons.size - index.toFloat())
                                .offset(x = (index * -6).dp),
                            placeholder = R.drawable.ic_avatar_place_holder,
                        )
                    }
                    
                    if (count > 3) {
                        Surface(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .zIndex(0f)
                                .offset(x = (displayIcons.size * -6).dp),
                            color = MixinAppTheme.colors.backgroundWindow
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${min(count - displayIcons.size, 99)}",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 8.sp
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