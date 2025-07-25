package one.mixin.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun Tooltip(
    text: String,
    onDismissRequest: () -> Unit,
    offset: IntOffset = IntOffset(0, 0),
    arrowOffsetX: Dp = 0.dp,
) {
    val yOffset = with(LocalDensity.current) { 16.dp.toPx() }.toInt()
    val arrowHeight = 6.dp
    val arrowWidth = 12.dp

    val tooltipShape = TooltipShape(
        cornerRadius = 8.dp,
        arrowHeight = arrowHeight,
        arrowWidth = arrowWidth,
        arrowOffsetX = arrowOffsetX,
    )

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        offset = IntOffset(offset.x, yOffset + offset.y)
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 1.dp, shape = tooltipShape)
                .background(
                    color = MixinAppTheme.colors.background,
                    shape = tooltipShape
                )
        ) {
            Text(
                text = text,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier
                    .padding(top = arrowHeight)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

class TooltipShape(
    private val cornerRadius: Dp,
    private val arrowHeight: Dp,
    private val arrowWidth: Dp,
    private val arrowOffsetX: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val arrowHeightPx = with(density) { arrowHeight.toPx() }
        val arrowWidthPx = with(density) { arrowWidth.toPx() }
        val arrowOffsetXPx = with(density) { arrowOffsetX.toPx() }

        val path = Path().apply {
            val rect = Rect(
                left = 0f,
                top = arrowHeightPx,
                right = size.width,
                bottom = size.height
            )
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = rect,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                )
            )

            val arrowX = arrowOffsetXPx + arrowWidthPx / 2f
            moveTo(arrowX, 0f)
            lineTo(arrowOffsetXPx, arrowHeightPx)
            lineTo(arrowOffsetXPx + arrowWidthPx, arrowHeightPx)
            close()
        }
        return Outline.Generic(path)
    }
}

