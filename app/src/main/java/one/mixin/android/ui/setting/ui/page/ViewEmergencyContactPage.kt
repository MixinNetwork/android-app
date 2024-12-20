package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.HighlightStarLinkText
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.UserAvatarImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.vo.User
import one.mixin.android.vo.createSystemUser

@Composable
fun ViewEmergencyContactPage(user: User) {
    SettingPageScaffold(
        title = stringResource(id = R.string.Emergency_Contact),
        titleBarActions = {
            val context = LocalContext.current
            IconButton(onClick = {
                context.openUrl(context.getString(R.string.emergency_url))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help_outline),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .background(MixinAppTheme.colors.background)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(42.dp))
            UserAvatarImage(user = user, size = 92.dp)
            Box(modifier = Modifier.height(12.dp))
            Text(
                text = user.fullName ?: "",
                modifier = Modifier.widthIn(0.dp, 280.dp),
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary,
                maxLines = 1,
            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.contact_mixin_id, user.identityNumber),
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                maxLines = 1,
            )
            Box(modifier = Modifier.height(42.dp))
        }

        Box(modifier = Modifier.height(8.dp))

        val context = LocalContext.current
        HighlightStarLinkText(
            modifier = Modifier.padding(horizontal = 16.dp),
            source = stringResource(id = R.string.setting_emergency_desc),
            links = arrayOf(context.getString(R.string.emergency_url)),
            textStyle =
                TextStyle(
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                ),
        ) {
            context.openUrl(context.getString(R.string.emergency_url))
        }

        Box(modifier = Modifier.height(16.dp))
    }
}

@Composable
@Preview
fun PreviewViewEmergencyContactPage() {
    MixinAppTheme {
        ViewEmergencyContactPage(
            user = createSystemUser(),
        )
    }
}
