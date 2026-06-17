package one.mixin.android.ui.home.inscription.component

import android.util.Log
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

sealed class AutoSizeConstraint(open val min: TextUnit = TextUnit.Unspecified) {
    data class Width(override val min: TextUnit = TextUnit.Unspecified): AutoSizeConstraint(min)
    data class Height(override val min: TextUnit = TextUnit.Unspecified): AutoSizeConstraint(min)
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = (24 * 1.1).sp,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 12,
    style: TextStyle = LocalTextStyle.current,
    constraint: AutoSizeConstraint = AutoSizeConstraint.Height(min = 12.sp),
) {
    val initialLineHeight: TextUnit = if (lineHeight != TextUnit.Unspecified) lineHeight else fontSize * 1.1f
    var textStyle by remember {
        mutableStateOf(
            style.copy(
                fontSize = fontSize,
                lineHeight = initialLineHeight,
            )
        )
    }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        text = text,
        color = color,
        fontSize = textStyle.fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = textStyle,
        onTextLayout = { result ->
            fun constrain() {
                val reducedSize = textStyle.fontSize * 0.9f
                val ratio: Float = if (lineHeight != TextUnit.Unspecified && fontSize != TextUnit.Unspecified) {
                    (lineHeight.value / fontSize.value)
                } else {
                    1.1f
                }
                if (constraint.min != TextUnit.Unspecified && reducedSize <= constraint.min) {
                    textStyle = textStyle.copy(
                        fontSize = constraint.min,
                        lineHeight = constraint.min * ratio,
                    )
                    readyToDraw = true
                } else if (reducedSize != TextUnit.Unspecified) {
                    textStyle = textStyle.copy(
                        fontSize = reducedSize,
                        lineHeight = reducedSize * ratio,
                    )
                }
                Log.d("AutoSizeText", "Text size reduced to: ${textStyle.fontSize}")
            }

            when (constraint) {
                is AutoSizeConstraint.Height -> {
                    if (result.didOverflowHeight) {
                        constrain()
                    } else {
                        readyToDraw = true
                    }
                }
                is AutoSizeConstraint.Width -> {
                    if (result.didOverflowWidth) {
                        constrain()
                    } else {
                        readyToDraw = true
                    }
                }
            }
        }
    )
}