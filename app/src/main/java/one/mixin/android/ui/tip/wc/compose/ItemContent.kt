package one.mixin.android.ui.tip.wc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.UserBadge
import one.mixin.android.vo.User

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemWalletContent(
    title: String,
    @DrawableRes iconRes: Int = R.drawable.ic_wallet_privacy,
    fontSize: TextUnit = 16.sp,
    padding: Dp = 20.dp,
    walletId: String? = null,
    walletName: String? = null,
    isWalletOwner: Boolean? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = padding),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (walletId != null && walletName != null) walletName else stringResource(R.string.Privacy_Wallet),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = fontSize,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            if (isWalletOwner != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(if (isWalletOwner) R.string.Wallet_Owner else R.string.Wallet_Member),
                    color = if (isWalletOwner) Color.White else MixinAppTheme.colors.textRemarks,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            color = if (isWalletOwner) MixinAppTheme.colors.walletOrange.copy(0.6f) else MixinAppTheme.colors.backgroundGrayLight,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp)
                )
            }
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
    padding: Dp = 20.dp,
    subTitleFontWeight: FontWeight = FontWeight.Normal,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = padding),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textRemarks,
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
                fontSize = 16.sp,
                fontWeight = subTitleFontWeight,
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
fun ItemContent(
    title: String,
    subTitle: String,
    toUser: User,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoilImage(
                model = toUser.avatarUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(4.dp))
            UserBadge(toUser)
        }
        Box(modifier = Modifier.height(5.dp))
        Text(
            text = subTitle,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
        )
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
        fontSize = 16.sp,
        inlineContent = mapOf(
            "label" to InlineTextContent(
                placeholder = Placeholder(
                    width = with(density) {
                        (measureTextWidth(label, (16 * 0.8).sp) + 12.dp).toSp()
                    },
                    height = with(density) {
                        16.5.sp
                    },
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        fontSize = (16 * 0.8).sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
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