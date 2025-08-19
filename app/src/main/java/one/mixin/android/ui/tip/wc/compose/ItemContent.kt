package one.mixin.android.ui.tip.wc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemWalletContent(
    title: String,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.Privacy_Wallet),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_wallet_privacy),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )

        }
    }
}
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
            TextWithRoundedLabelInline(subTitle, label, isAddress)
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
fun TextWithRoundedLabelInline(
    content: String,
    label: String,
    isAddress: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (!isAddress) Color(0xB34B7CDD) else Color(0xFF8DCC99)
    val density = LocalDensity.current

    Text(
        text = buildAnnotatedString {
            append(content)
            append(" ")
            appendInlineContent("label", "[label]")
        },
        color = MixinAppTheme.colors.textPrimary,
        fontSize = 14.sp,
        inlineContent = mapOf(
            "label" to InlineTextContent(
                placeholder = Placeholder(
                    width = with(density) {
                        (measureTextWidth(label, (14 * 0.8).sp) + 12.dp).toSp()
                    },
                    height = with(density) {
                        (16.dp).toSp()
                    },
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = accentColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .wrapContentSize(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = (14 * 0.8).sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .widthIn(max = 100.dp)
                    )
                }
            }
        ),
        modifier = modifier
    )
}

@Composable
private fun measureTextWidth(text: String, fontSize: TextUnit): Dp {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val paint = android.graphics.Paint().apply {
        textSize = fontSizePx
        isAntiAlias = true
    }
    val textWidth = paint.measureText(text)
    return with(density) { textWidth.toDp() }
}