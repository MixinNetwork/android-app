package one.mixin.android.ui.landing.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
    textAlign: TextAlign = TextAlign.Center,
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
                    pushStringAnnotation(tag = "CLICKABLE", annotation = highlightText)
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

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            textAlign = textAlign
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "CLICKABLE",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                onTextClick.invoke(annotation.item)
            }
        }
    )
}