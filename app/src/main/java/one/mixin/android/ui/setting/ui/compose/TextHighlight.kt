package one.mixin.android.ui.setting.ui.compose

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

private const val TAG_URL = "URL"

@Composable
fun HighlightStarText(
    modifier: Modifier = Modifier,
    source: String,
    links: Array<String>,
    highlightStyle: SpanStyle = SpanStyle(color = MixinAppTheme.colors.accent),
    textStyle: TextStyle = TextStyle(color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp),
    onClick: (link: String) -> Unit
) {
    val annotatedString = remember {
        buildAnnotatedString {
            var start: Int
            var end: Int
            val stringBuilder = StringBuilder(source)
            val targets = arrayListOf<Pair<Int, Int>>()

            kotlin.runCatching {
                while (stringBuilder.indexOf("**").also { start = it } != -1) {
                    stringBuilder.replace(start, start + 2, "")
                    end = stringBuilder.indexOf("**")
                    if (end >= 0) {
                        stringBuilder.replace(end, end + 2, "")
                        targets.add(start to end)
                    }
                }
            }

            append(stringBuilder.toString())

            kotlin.runCatching {
                for (i in targets.indices) {
                    val (highlightStart, highlightEnd) = targets[i]
                    addStyle(
                        highlightStyle,
                        highlightStart, highlightEnd
                    )
                    addStringAnnotation(TAG_URL, annotation = links[i], highlightStart, highlightEnd)
                }
            }
        }
    }

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        onClick = { position ->
            annotatedString.getStringAnnotations(position, position).firstOrNull()?.let {
                if (it.tag == TAG_URL) {
                    onClick(it.item)
                }
            }
        },
        style = textStyle,
    )
}

@Composable
@Preview
fun HighlightLinkTextPreview() {
    MixinAppTheme {
        Surface(color = MixinAppTheme.colors.background) {
            HighlightStarText(
                source = "Test **Mixin** Test **One**",
                links = arrayOf("https://www.mixin.one/", "https://www.mixin.one/"),
                onClick = {
                    println(it)
                }
            )
        }
    }
}
