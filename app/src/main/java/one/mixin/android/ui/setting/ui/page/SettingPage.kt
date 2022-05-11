package one.mixin.android.ui.setting.ui.page

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.compose.SettingTile

@Composable
fun SettingPage() {
    SettingPageScaffold(title = stringResource(R.string.setting_title)) {
        val context = LocalContext.current

        val settingNavController = LocalSettingNav.current

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
            settingNavController.navigation(SettingDestination.BackUp)
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
            context.findFragmentActivityOrNull()?.supportFragmentManager?.let { fragmentManager ->
                DeviceFragment.newInstance().showNow(fragmentManager, DeviceFragment.TAG)
            }
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

