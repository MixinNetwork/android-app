package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MultiColorProgressBar(
    distributions: List<AssetDistribution>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeHeight = 4.dp
    val cornerRadius = 2.dp

    val blueGradient =
        Brush.horizontalGradient(
            colors = listOf(
                MixinAppTheme.colors.walletBlue,
                MixinAppTheme.colors.walletBlue.copy(alpha = 0.7f)
            )
        )

    val purpleGradient =
        Brush.horizontalGradient(
            colors = listOf(
                MixinAppTheme.colors.walletPurple,
                MixinAppTheme.colors.walletPurple.copy(alpha = 0.7f)
            )
        )

    val yellowGradient =
        Brush.horizontalGradient(
            colors = listOf(
                MixinAppTheme.colors.walletYellow,
                MixinAppTheme.colors.walletYellow.copy(alpha = 0.7f)
            )
        )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(strokeHeight)
                .graphicsLayer {
                    this.shadowElevation = 4f
                    this.shape = RoundedCornerShape(cornerRadius)
                    this.clip = true
                }
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    clip = true,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .background(MixinAppTheme.colors.backgroundWindow, RoundedCornerShape(cornerRadius))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                val filteredDistributions = distributions.take(3)
                
                filteredDistributions.forEachIndexed { index, asset ->
                    val brush = when (index) {
                        0 -> blueGradient
                        1 -> purpleGradient
                        else -> yellowGradient
                    }
                    
                    val shape = when {
                        filteredDistributions.size == 1 -> RoundedCornerShape(cornerRadius)
                        index == 0 -> RoundedCornerShape(
                            topStart = cornerRadius,
                            bottomStart = cornerRadius,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                        index == filteredDistributions.size - 1 -> RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = cornerRadius,
                            bottomEnd = cornerRadius
                        )
                        else -> RoundedCornerShape(0.dp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(asset.percentage)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(brush)
                    )
                }
            }
        }
    }
}