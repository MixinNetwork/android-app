package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TradeLayout(
    headerCompose: @Composable () -> Unit,
    bottomCompose: @Composable () -> Unit,
    centerCompose: @Composable () -> Unit,
    tailCompose: (@Composable () -> Unit)? = null,
    margin: Dp,
) {
    Layout(
        content = {
            Box { headerCompose() }
            Box { bottomCompose() }
            Box { centerCompose() }
            if (tailCompose != null) {
                Box { tailCompose() }
            }
        },
        modifier =
            Modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .padding(horizontal = 10.dp, vertical = margin),
    ) { measurables, constraints ->
        val marginPx = margin.roundToPx()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val header = measurables[0].measure(looseConstraints)
        val bottom = measurables[1].measure(looseConstraints)
        val center = measurables[2].measure(looseConstraints)
        val tail = measurables.getOrNull(3)?.measure(looseConstraints)

        val width = maxOf(
            header.width,
            bottom.width,
            center.width,
            tail?.width ?: 0,
        ).coerceIn(constraints.minWidth, constraints.maxWidth)

        val bottomY = header.height + marginPx
        val centerY = header.height + (marginPx - center.height) / 2
        val tailY = bottomY + bottom.height + marginPx
        val totalHeight = (tail?.let { tailY + it.height } ?: (bottomY + bottom.height))
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, totalHeight) {
            header.placeRelative((width - header.width) / 2, 0)
            bottom.placeRelative((width - bottom.width) / 2, bottomY)
            center.placeRelative((width - center.width) / 2, centerY)
            tail?.placeRelative((width - tail.width) / 2, tailY)
        }
    }
}



@Preview
@Composable
fun TradeLayoutPreview() {
    TradeLayout(
        headerCompose = {
            Box(
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(color = Color.Red),
            )
        },
        bottomCompose = {
            Box(
                modifier =
                    Modifier
                        .height(140.dp)
                        .fillMaxWidth()
                        .background(color = Color.Green),
            )
        },
        centerCompose = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(color = Color.Blue),
            )
        },
        margin = 2.dp,
    )
}
