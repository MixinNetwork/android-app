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
fun HighlightLinkText(
    modifier: Modifier = Modifier,
    source: String,
    texts: Array<String>,
    links: Array<String>,
    highlightStyle: SpanStyle = SpanStyle(color = MixinAppTheme.colors.accent),
    textStyle: TextStyle = TextStyle(color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp),
    onClick: (link: String) -> Unit
) {

    require(texts.size == links.size) { "text's length should equals with links" }

    val annotatedString = remember {
        buildAnnotatedString {
            append(source)
            for (i in texts.indices) {
                val text = texts[i]
                val link = links[i]
                val start = source.indexOf(text)
                require(start != -1) { "start index can not be -1" }

                addStyle(
                    highlightStyle,
                    start, start + text.length
                )
                addStringAnnotation(TAG_URL, annotation = link, start, start + text.length)
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
            HighlightLinkText(
                source = "Test Mixin Test One",
                texts = arrayOf("Mixin", "One"),
                links = arrayOf("https://www.mixin.one/", "https://www.mixin.one/"),
                onClick = {
                    println(it)
                }
            )
        }
    }
}