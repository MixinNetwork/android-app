package one.mixin.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.getColorCode
import one.mixin.android.vo.App
import one.mixin.android.vo.User
import one.mixin.android.widget.AvatarView

@Composable
fun UserAvatarImage(
    user: User,
    size: Dp,
) {
    AvatarImage(
        name = user.fullName ?: "",
        imageUrl = user.avatarUrl,
        identityNumber = user.identityNumber,
        size = size,
    )
}

@Composable
fun AppAvatarImage(
    app: App,
    size: Dp,
) {
    AvatarImage(
        name = app.name,
        imageUrl = app.iconUrl,
        identityNumber = app.appNumber,
        size = size,
    )
}

@Composable
private fun AvatarImage(
    name: String,
    imageUrl: String?,
    identityNumber: String,
    size: Dp,
) {
    if (!imageUrl.isNullOrEmpty()) {
        CoilImage(
            model = imageUrl,
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape),
            placeholder = R.drawable.ic_avatar_place_holder,
        )
    } else {
        val avatarArray = integerArrayResource(id = R.array.avatar_colors)
        val code = identityNumber.getColorCode(CodeType.Avatar(avatarArray.size))
        val color = avatarArray.getOrElse(code) { avatarArray[0] }
        val fontSize = with(LocalDensity.current) { (size * (22f / 48f)).toSp() }
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(color)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = AvatarView.checkEmoji(name),
                modifier = Modifier.sizeIn(maxWidth = size * 0.8f, maxHeight = size * 0.8f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = fontSize * 0.1f,
                    maxFontSize = fontSize,
                    stepSize = fontSize * 0.05f,
                ),
            )
        }
    }
}

@Composable
@Preview
fun AvatarImagePreview() {
    MixinAppTheme {
        AvatarImage(
            name = "Test",
            imageUrl = null,
            identityNumber = "1234124",
            size = 100.dp,
        )
    }
}
