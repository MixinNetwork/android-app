package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem

@Composable
fun Distribution(distributions: List<AssetDistribution>, destination: WalletDestination?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val topThree = distributions.take(3)
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
                        icon = null,
                        currency = "",
                        modifier = Modifier.weight(1f)
                    )
            }

            topThree.size == 1 -> {
                if (destination == null)
                    LegendItem(
                        percentage = 1f,
                        color = MixinAppTheme.colors.walletBlue,
                        currency = topThree[0].currency,
                        modifier = Modifier.weight(1f)
                    )
                else
                    LegendAssetItem(
                        percentage = 0f,
                        icon = topThree[0].icon,
                        currency = topThree[0].currency,
                        modifier = Modifier.weight(1f)
                    )
            }

            destination == null -> {
                topThree.forEachIndexed { index, asset ->
                    when (index) {
                        0 -> LegendItem(
                            percentage = asset.percentage,
                            color = MixinAppTheme.colors.walletBlue,
                            currency = asset.currency,
                            modifier = Modifier.weight(1f)
                        )

                        1 -> LegendItem(
                            percentage = asset.percentage,
                            color = MixinAppTheme.colors.walletPurple,
                            currency = asset.currency,
                            modifier = Modifier.weight(1f)
                        )

                        2 -> {
                            val remainingPercentage =
                                1f - topThree[0].percentage - topThree[1].percentage
                            LegendItem(
                                percentage = remainingPercentage,
                                color = MixinAppTheme.colors.walletYellow,
                                currency = if (distributions.size > 3) "Other" else asset.currency,
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
                            icon = asset.icon,
                            currency = asset.currency,
                            modifier = Modifier.weight(1f)
                        )

                        1 -> LegendAssetItem(
                            percentage = asset.percentage,
                            icon = asset.icon,
                            currency = asset.currency,
                            modifier = Modifier.weight(1f)
                        )

                        2 -> {
                            val remainingPercentage =
                                1f - topThree[0].percentage - topThree[1].percentage
                            LegendAssetItem(
                                percentage = remainingPercentage,
                                icon = asset.icon,
                                currency = if (distributions.size > 3) "Other" else asset.currency,
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
private fun LegendAssetItem(modifier: Modifier, percentage: Float, icon: String?, currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        CoilImage(
            model = icon,
            modifier =
                Modifier
                    .size(18.dp)
                    .clip(CircleShape),
            placeholder = R.drawable.ic_avatar_place_holder,
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