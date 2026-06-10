package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.ui.wallet.alert.vo.AlertType

@Composable
fun PercentagesRow(modifier: Modifier, type: AlertType, onPercentageClick: (Float) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = modifier
    ) {
        val percentages = when (type) {
            AlertType.PRICE_REACHED -> {
                listOf("-20%", "-10%", "-5%", "+5%", "+10%", "+20%")
            }

            AlertType.PRICE_INCREASED, AlertType.PERCENTAGE_INCREASED -> {
                listOf("+5%", "+10%", "+20%", "", "", "")
            }

            else -> {
                listOf("-20%", "-10%", "-5%", "", "", "")
            }
        }

        val availableWidth = LocalConfiguration.current.screenWidthDp.dp / percentages.size
        val fontSize = if (availableWidth < 50.dp) 8.sp else 12.sp

        percentages.forEachWithIndex { index, percent ->
            if (index != 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (percent == "") {
                // empty holder
                Box(
                    modifier = Modifier
                        .weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(color = MixinAppTheme.colors.backgroundWindow, shape = RoundedCornerShape(16.dp))
                        .border(1.dp, color = MixinAppTheme.colors.borderColor, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            onClick = {
                                onPercentageClick(
                                    when (percent) {
                                        "-20%" -> -0.2f
                                        "-10%" -> -0.1f
                                        "-5%" -> -0.05f
                                        "+5%" -> 0.05f
                                        "+10%" -> 0.1f
                                        else -> 0.2f
                                    }
                                )
                            },
                            indication = LocalIndication.current,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = percent,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = fontSize,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
