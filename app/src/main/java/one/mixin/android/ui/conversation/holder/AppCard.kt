package one.mixin.android.ui.conversation.holder

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.AppCardData
import java.util.regex.Pattern

@Composable
fun AppCard(appCardData: AppCardData, isMe: Boolean, timeAgo: String? = null, urlClick: (String) -> Unit, width: Int? = null) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxItemWidth = (screenWidthDp * 4 / 5)

    MixinAppTheme {
        Column(modifier = Modifier.width(width?.let { with(LocalDensity.current) { it.toDp() } } ?: maxItemWidth)) {
            CoilImage(
                model = appCardData.iconUrl,
                placeholder = R.drawable.bot_default,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(start = 7.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.run {
                if (isMe) {
                    padding(start = 8.dp, end = 16.dp)
                } else {
                    padding(start = 16.dp, end = 8.dp)
                }
            }) {
                Text(
                    text = appCardData.title,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ClickableTextWithUrls(
                    text = appCardData.description, urlClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(timeAgo ?: "", Modifier.align(Alignment.End), color = Color(0xFF83919E), fontWeight = FontWeight.W300, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

const val URL_PATTERN = "\\b[a-zA-Z+]+:(?://)?[\\w-]+(?:\\.[\\w-]+)*(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?\\b/?"

@Composable
fun ClickableTextWithUrls(text: String, urlClick: (String) -> Unit) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val urlPattern = Pattern.compile(URL_PATTERN)
            val matcher = urlPattern.matcher(text)

            var lastIndex = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val url = matcher.group()

                append(text.substring(lastIndex, start))
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(color = Color(0xFF5FA7E4), textDecoration = TextDecoration.Underline)) {
                    append(url)
                }
                pop()
                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    var layoutResult: TextLayoutResult? = null

    SelectionContainer {
        Text(
            text = annotatedString,
            style = TextStyle(fontSize = 13.sp, color = MixinAppTheme.colors.textPrimary, lineHeight = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { offsetPosition ->
                        layoutResult?.let {
                            val position = it.getOffsetForPosition(offsetPosition)
                            annotatedString
                                .getStringAnnotations(tag = "URL", start = position, end = position)
                                .firstOrNull()
                                ?.let { annotation ->
                                    urlClick(annotation.item)
                                }
                        }
                    }
                },
            onTextLayout = { textLayoutResult ->
                layoutResult = textLayoutResult
            }
        )
    }
}