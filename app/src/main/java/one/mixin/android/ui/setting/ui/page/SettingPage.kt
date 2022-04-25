package one.mixin.android.ui.setting.ui.page

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
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
                MixinTopAppBar(
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
                val context = LocalContext.current
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

                val scope = rememberCoroutineScope()
                val viewModel = hiltViewModel<SettingViewModel>()

                SettingTile(
                    icon = R.drawable.ic_setting_feedback,
                    title = stringResource(id = R.string.setting_feedback)
                ) {
                    scope.launch {
                        val userTeamMixin = viewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                        if (userTeamMixin == null) {
                            toast(R.string.error_data)
                        } else {
                            ConversationActivity.show(context, recipientId = Constants.TEAM_MIXIN_USER_ID)
                        }
                    }
                }
                SettingTile(
                    icon = R.drawable.ic_setting_share,
                    title = stringResource(id = R.string.setting_share)
                ) {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(R.string.setting_share_text, Session.getAccount()?.identityNumber)
                    )
                    sendIntent.type = "text/plain"
                    context.startActivity(
                        Intent.createChooser(
                            sendIntent,
                            context.resources.getText(R.string.setting_share)
                        )
                    )
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
        SettingTile(
            icon = R.drawable.ic_setting_about,
            title = stringResource(id = R.string.about)
        ) {
        }
    }
}
