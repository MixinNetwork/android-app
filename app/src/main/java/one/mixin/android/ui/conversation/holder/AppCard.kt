package one.mixin.android.ui.conversation.holder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.vo.AppCardData
import java.util.regex.Pattern

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    appCardData: AppCardData,
    contentClick: () -> Unit,
    contentLongClick: () -> Unit,
    urlClick: (String) -> Unit,
    urlLongClick: (String) -> Unit,
    botClick: (String) -> Unit,
    width: Int? = null,
    createdAt: String? = null,
    isLast: Boolean = false,
    isMe: Boolean = false,
    status: String? = null,
    isPin: Boolean = false,
    isRepresentative: Boolean = false,
    isSecret: Boolean = false,
    isWhite: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val textSize = (context.defaultSharedPreferences.getInt(Constants.Account.PREF_TEXT_SIZE, 14).textDp)
    val titleSize = ((context.defaultSharedPreferences.getInt(Constants.Account.PREF_TEXT_SIZE, 14) + 2).textDp)

    MixinAppTheme {
        Column(modifier = Modifier
            .width(width?.let { with(LocalDensity.current) { it.toDp() } } ?: min(340.dp, max(240.dp, (screenWidthDp * 3 / 4))))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = contentClick, onLongClick = contentLongClick
            )) {
            Box(
                modifier = Modifier.padding(
                    start = if (isMe) 0.dp else 7.dp, end = if (isMe) {
                        if (isLast) 6.dp else 7.dp
                    } else 0.dp
                )
            ) {
                if (!appCardData.coverUrl.isNullOrBlank()) {
                    CoilImage(
                        model = appCardData.coverUrl,
                        placeholder = R.drawable.bot_default,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f)
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                } else if (appCardData.cover != null) {
                    CoilImage(
                        model = appCardData.cover.url,
                        placeholder = appCardData.cover.thumbnailDrawable,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(appCardData.cover.radio)
                            .wrapContentHeight()

                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier
                .fillMaxWidth()
                .run {
                    if (isMe) {
                        padding(start = 10.dp, end = 16.dp)
                    } else {
                        padding(start = 16.dp, end = 10.dp)
                    }
                }) {
                if (
                    !appCardData.title.isNullOrBlank()
                ) {
                    Text(
                        text = appCardData.title ?: "",
                        fontSize = titleSize,
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (
                    !appCardData.description.isNullOrEmpty()
                ) {
                    ClickableTextWithUrlsAndBots(
                        text = appCardData.description ?: "", textSize, contentClick, contentLongClick, urlClick, urlLongClick, botClick
                    )
                }
                if (createdAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TimeBubble(modifier = Modifier.align(Alignment.End), createdAt, isMe, status, isPin, isRepresentative, isSecret, isWhite)
                }
            }
        }
    }
}

private fun Int.textDp(density: Density): TextUnit = with(density) {
    this@textDp.dp.toSp()
}

val Int.textDp: TextUnit
    @Composable get() = this.textDp(density = LocalDensity.current)

private const val URL_PATTERN = "\\b[a-zA-Z+]+:(?://)?[\\w-]+(?:\\.[\\w-]+)*(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?\\b/?"
private const val BOT_PATTERN = "(?<=^|\\D)(7000|7000\\d{6})(?=\$|\\D)"

private const val LONG_CLICK_TIME = 200L


@Composable
fun ClickableTextWithUrlsAndBots(
    text: String,
    fontSize: TextUnit,
    contentClick: () -> Unit,
    contentLongClick: () -> Unit,
    urlClick: (String) -> Unit,
    urlLongClick: (String) -> Unit,
    botClick: (String) -> Unit
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val urlPattern = Pattern.compile(URL_PATTERN)
            val botPattern = Pattern.compile(BOT_PATTERN)

            val urlMatcher = urlPattern.matcher(text)
            val botMatcher = botPattern.matcher(text)

            var lastIndex = 0

            var nextUrlMatch = if (urlMatcher.find()) urlMatcher else null
            var nextBotMatch = if (botMatcher.find()) botMatcher else null

            while (nextUrlMatch != null || nextBotMatch != null) {
                val isUrlNext = nextUrlMatch != null && (nextBotMatch == null || nextUrlMatch.start() < nextBotMatch.start())

                val start: Int
                val end: Int
                val detectedText: String
                val isUrl: Boolean

                if (isUrlNext) {
                    start = nextUrlMatch!!.start()
                    end = nextUrlMatch.end()
                    detectedText = nextUrlMatch.group()
                    isUrl = true
                    nextUrlMatch = if (urlMatcher.find()) urlMatcher else null
                } else {
                    start = nextBotMatch!!.start()
                    end = nextBotMatch.end()
                    detectedText = nextBotMatch.group()
                    isUrl = false
                    nextBotMatch = if (botMatcher.find()) botMatcher else null
                }

                append(text.substring(lastIndex, start))
                pushStringAnnotation(tag = if (isUrl) "URL" else "BOT", annotation = detectedText)
                withStyle(
                    style = SpanStyle(
                        color = if (isUrl) Color(0xFF5FA7E4) else Color(0xFF00FF00),
                        textDecoration = if (isUrl) TextDecoration.Underline else TextDecoration.None
                    )
                ) {
                    append(detectedText)
                }
                pop()
                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var highlightedUrl by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var isLongPressed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    BasicText(
        text = buildAnnotatedString {
            val urlPattern = Pattern.compile(URL_PATTERN)
            val botPattern = Pattern.compile(BOT_PATTERN)

            val urlMatcher = urlPattern.matcher(text)
            val botMatcher = botPattern.matcher(text)

            var lastIndex = 0

            var nextUrlMatch = if (urlMatcher.find()) urlMatcher else null
            var nextBotMatch = if (botMatcher.find()) botMatcher else null

            while (nextUrlMatch != null || nextBotMatch != null) {
                val isUrlNext = nextUrlMatch != null && (nextBotMatch == null || nextUrlMatch!!.start() < nextBotMatch!!.start())

                val start: Int
                val end: Int
                val detectedText: String
                val isUrl: Boolean

                if (isUrlNext) {
                    start = nextUrlMatch!!.start()
                    end = nextUrlMatch!!.end()
                    detectedText = nextUrlMatch!!.group()
                    isUrl = true
                    nextUrlMatch = if (urlMatcher.find()) urlMatcher else null
                } else {
                    start = nextBotMatch!!.start()
                    end = nextBotMatch!!.end()
                    detectedText = nextBotMatch!!.group()
                    isUrl = false
                    nextBotMatch = if (botMatcher.find()) botMatcher else null
                }

                append(text.substring(lastIndex, start))
                pushStringAnnotation(tag = if (isUrl) "URL" else "BOT", annotation = detectedText)
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF5FA7E4),
                        textDecoration = TextDecoration.None,
                        background = if (highlightedUrl == detectedText) Color(0x660D94FC) else Color.Transparent
                    )
                ) {
                    append(detectedText)
                }
                pop()
                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        },
        style = TextStyle(fontSize = fontSize, color = MixinAppTheme.colors.textPrimary, lineHeight = fontSize * 1.25),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offsetPosition ->
                    if (!isLongPressed) {
                        layoutResult?.let {
                            val position = it.getOffsetForPosition(offsetPosition)
                            val annotation = annotatedString
                                .getStringAnnotations(start = position, end = position)
                                .firstOrNull()

                            if (annotation != null) {
                                if (annotation.tag == "URL") {
                                    urlClick(annotation.item)
                                } else if (annotation.tag == "BOT") {
                                    botClick(annotation.item)
                                }
                            } else {
                                contentClick()
                            }
                        }
                    }
                }, onLongPress = { offsetPosition ->
                    layoutResult?.let {
                        val position = it.getOffsetForPosition(offsetPosition)
                        val annotation = annotatedString
                            .getStringAnnotations(start = position, end = position)
                            .firstOrNull()

                        if (annotation != null && annotation.tag == "URL") {
                            job = coroutineScope.launch {
                                delay(LONG_CLICK_TIME)
                                highlightedUrl = annotation.item
                                urlLongClick(annotation.item)
                                isLongPressed = true
                            }
                        } else {
                            contentLongClick()
                        }
                    }
                })
            },
        onTextLayout = { textLayoutResult ->
            layoutResult = textLayoutResult
        }
    )

    LaunchedEffect(isLongPressed) {
        if (isLongPressed) {
            delay(LONG_CLICK_TIME)
            isLongPressed = false
            highlightedUrl = null
        }
    }
}