package one.mixin.android.ui.setting.ui.page

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun SettingPage() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.surface
    ) {
        Scaffold(
            backgroundColor = MixinAppTheme.colors.backgroundWindow,
            topBar = {
                val settingNavController = LocalSettingNav.current
                TopAppBar(
                    contentColor = MixinAppTheme.colors.textPrimary,
                    backgroundColor = MixinAppTheme.colors.background,
                    elevation = 0.dp,
                    navigationIcon = {
                        MixinBackButton()
                    },
                    title = {
                        Text(text = stringResource(id = R.string.setting_title))
                    },
                )
            }
        ) {
            val settingNavController = LocalSettingNav.current
            Column(modifier = Modifier.padding(it)) {
                SettingTile(
                    icon = R.drawable.ic_setting_privacy,
                    title = stringResource(id = R.string.setting_account)
                ) {
                    settingNavController.navigation(SettingDestination.Account)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_notification,
                    title = stringResource(id = R.string.setting_notification_confirmation)
                ) {
                    settingNavController.navigation(SettingDestination.NotificationAndConfirm)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_backup,
                    title = stringResource(id = R.string.setting_backup)
                ) {
                    settingNavController.navigation(SettingDestination.Backup)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_data,
                    title = stringResource(id = R.string.setting_data_storage)
                ) {
                    settingNavController.navigation(SettingDestination.Storage)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_appearance,
                    title = stringResource(id = R.string.setting_appearance)
                ) {
                    settingNavController.navigation(SettingDestination.Appearance)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_desktop,
                    title = stringResource(id = R.string.setting_desktop)
                ) {
                    settingNavController.navigation(SettingDestination.Desktop)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_feedback,
                    title = stringResource(id = R.string.setting_feedback)
                ) {
                    settingNavController.navigation(SettingDestination.Feedback)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_share,
                    title = stringResource(id = R.string.setting_share)
                ) {
                    settingNavController.navigation(SettingDestination.Share)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_about,
                    title = stringResource(id = R.string.about)
                ) {
                    settingNavController.navigation(SettingDestination.About)
                }
            }
        }
    }
}

@Composable
fun SettingTile(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(60.dp)
            .background(MixinAppTheme.colors.background)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MixinAppTheme.colors.icon
        )
        Box(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary
        )
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MixinAppTheme {
        SettingPage()
    }
}

@Preview(showBackground = true)
@Composable
fun DarkThemePreview() {
    MixinAppTheme(darkTheme = true) {
        SettingPage()
    }
}