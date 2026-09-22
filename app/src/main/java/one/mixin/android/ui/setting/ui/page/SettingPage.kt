package one.mixin.android.ui.setting.ui.page

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.openCustomerService
import one.mixin.android.session.Session
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination

@Composable
fun SettingPage() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MixinAppTheme.colors.backgroundWindow,
    ) {
        Scaffold(
            backgroundColor = MixinAppTheme.colors.backgroundWindow,
            topBar = {
                MixinTopAppBar(
                    navigationIcon = {
                        MixinBackButton()
                    },
                    title = {
                        Text(text = stringResource(id = R.string.Settings))
                    },
                )
            },
        ) {
            val settingNavController = LocalSettingNav.current
            Column(
                modifier =
                    Modifier
                        .padding(it)
                        .verticalScroll(rememberScrollState()),
            ) {
                val context = LocalContext.current
                val inviteContent = stringResource(R.string.chat_on_mixin_content, Session.getAccount()?.identityNumber ?: "")
                val share = stringResource(R.string.Share)
                SettingTile(
                    icon = R.drawable.ic_setting_privacy,
                    title = stringResource(id = R.string.Account),
                ) {
                    settingNavController.navigation(SettingDestination.Account)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_notification,
                    title = stringResource(id = R.string.Notification_and_Confirmation),
                ) {
                    settingNavController.navigation(SettingDestination.NotificationAndConfirm)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_backup,
                    title = stringResource(id = R.string.Chat),
                ) {
                    settingNavController.navigation(SettingDestination.MigrateRestore)
                }
                SettingTile(
                    icon = R.drawable.ic_setting_data,
                    title = stringResource(id = R.string.Data_and_Storage_Usage),
                ) {
                    settingNavController.navigation(SettingDestination.DataStorage)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_appearance,
                    title = stringResource(id = R.string.Appearance),
                ) {
                    settingNavController.navigation(SettingDestination.Appearance)
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_desktop,
                    title = stringResource(id = R.string.Mixin_Messenger_Desktop),
                ) {
                    context.findFragmentActivityOrNull()?.supportFragmentManager?.let { fragmentManager ->
                        DeviceFragment.newInstance().showNow(fragmentManager, DeviceFragment.TAG)
                    }
                }
                Box(modifier = Modifier.height(16.dp))

                SettingTile(
                    icon = R.drawable.ic_setting_feedback,
                    title = stringResource(id = R.string.Feedback),
                ) {
                    context.openCustomerService()
                }
                SettingTile(
                    icon = R.drawable.ic_setting_share,
                    title = stringResource(id = R.string.Invite_a_Friend),
                ) {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        inviteContent,
                    )
                    sendIntent.type = "text/plain"
                    context.startActivity(
                        Intent.createChooser(
                            sendIntent,
                            share,
                        ),
                    )
                }
                Box(modifier = Modifier.height(16.dp))
                SettingTile(
                    icon = R.drawable.ic_setting_about,
                    title = stringResource(id = R.string.About),
                ) {
                    settingNavController.navigation(SettingDestination.About)
                }
            }
        }
    }
}
