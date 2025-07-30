package one.mixin.android.ui.tip.wc.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemContent(
    title: String,
    subTitle: String,
    label: String? = null,
    footer: String? = null,
    isAddress: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(4.dp))

        if (label != null) {
            TextWithRoundedLabel(subTitle, label, isAddress)
        } else {
            Text(
                text = subTitle,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
        }

        if (footer != null) {
            Box(modifier = Modifier.height(4.dp))
            Text(
                text = footer,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
        }
    }
}



@Composable
fun TextWithRoundedLabel(
    content: String,
    label: String,
    isAddress: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cornerRadius = with(density) { 4.dp.toPx() }
    val horizontalPadding = with(density) { 4.dp.toPx() }
    val verticalPadding = with(density) { 2.dp.toPx() }

    val fullText = "$content  $label"
    val labelStart = fullText.lastIndexOf(label)
    val labelEnd = labelStart + label.length
    val accentColor = if(!isAddress) Color(0xB34B7CDD) else Color(0xFF8DCC99)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = buildAnnotatedString {
            append(fullText)
            addStyle(
                style = SpanStyle(
                    fontSize = 0.8.em,
                    color = Color.White
                ),
                start = labelStart,
                end = labelEnd
            )
        },
        color = MixinAppTheme.colors.textPrimary,
        fontSize = 14.sp,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.drawBehind {
            textLayoutResult?.let { layout ->
                val labelStartOffset = layout.getHorizontalPosition(labelStart, true)
                val labelEndOffset = layout.getHorizontalPosition(labelEnd, true)
                val lineTop = layout.getLineTop(layout.getLineForOffset(labelStart))
                val lineBottom = layout.getLineBottom(layout.getLineForOffset(labelStart))
                val lineHeight = lineBottom - lineTop

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(
                        labelStartOffset - horizontalPadding,
                        lineTop + verticalPadding
                    ),
                    size = Size(
                        labelEndOffset - labelStartOffset + horizontalPadding * 2,
                        lineHeight - verticalPadding * 2
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    )
}
