package one.mixin.android.ui.landing.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun HighlightedTextWithClick(
    fullText: String,
    modifier: Modifier,
    vararg highlightTexts: String,
    color: Color = MixinAppTheme.colors.textAssist,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 19.6.sp,
    onTextClick: (String) -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        var currentIndex = 0

        highlightTexts.forEach { highlightText ->
            val startIndex = fullText.indexOf(highlightText, currentIndex)
            if (startIndex >= 0) {
                append(fullText.substring(currentIndex, startIndex))

                withStyle(style = SpanStyle(color = MixinAppTheme.colors.textBlue, fontSize = fontSize)) {
                    pushStringAnnotation(tag = highlightText, annotation = highlightText)
                    append(highlightText)
                    pop()
                }

                currentIndex = startIndex + highlightText.length
            }
        }

        if (currentIndex < fullText.length) {
            append(fullText.substring(currentIndex))
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            highlightTexts.forEach { highlightText ->
                annotatedText.getStringAnnotations(
                    tag = highlightText,
                    start = 0,
                    end = annotatedText.length
                ).firstOrNull()?.let {
                    onTextClick.invoke(it.item)
                }
            }
        },
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}