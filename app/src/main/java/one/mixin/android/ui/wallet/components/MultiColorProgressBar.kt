package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MultiColorProgressBar(
    distributions: List<AssetDistribution>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(4.dp))
    ) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))) {
            distributions.take(3).forEachIndexed { index, asset ->
                Box(
                    modifier = Modifier
                        .weight(asset.percentage)
                        .fillMaxHeight()
                        .background(
                            when (index) {
                                0 -> MixinAppTheme.colors.walletBlue
                                1 -> MixinAppTheme.colors.walletPurple
                                else -> MixinAppTheme.colors.walletYellow
                            }
                        )
                )
            }
        }
    }
}