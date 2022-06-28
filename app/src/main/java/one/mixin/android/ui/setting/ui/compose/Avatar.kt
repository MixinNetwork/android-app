package one.mixin.android.ui.setting.ui.compose

import GlideImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.getColorCode
import one.mixin.android.vo.User
import one.mixin.android.widget.AvatarView

@Composable
fun UserAvatarImage(user: User, size: Dp) {
    if (user.avatarUrl != null && user.avatarUrl.isNotEmpty()) {
        GlideImage(
            data = user.avatarUrl,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder)
        )
    } else {
        val avatarArray = integerArrayResource(id = R.array.avatar_colors)
        val backgroundColor = user.userId.getColorCode(CodeType.Avatar(avatarArray.size))
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(backgroundColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = AvatarView.checkEmoji(user.fullName),
                color = Color.White,
                fontSize = 22.sp
            )
        }
    }
}