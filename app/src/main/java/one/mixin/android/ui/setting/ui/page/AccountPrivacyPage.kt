package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.booleanValueAsState
import one.mixin.android.compose.intValueAsState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.supportsOreo
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.SettingViewModel

@Composable
fun AccountPrivacyPage() {
    SettingPageScaffold(title = stringResource(R.string.Privacy)) {
        val viewModel = hiltViewModel<SettingViewModel>()
        val navController = LocalSettingNav.current

        val blockedUsers by viewModel.countBlockingUsers().observeAsState()

        SettingTile(
            title = stringResource(R.string.Blocked),
            trailing = {
                if (blockedUsers.isNullOrEmpty()) {
                    Text(text = stringResource(id = R.string.None))
                } else {
                    Text(text = "${blockedUsers!!.size}")
                }
            },
        ) {
            navController.navigation(SettingDestination.Blocked)
        }

        SettingTile(
            title = stringResource(R.string.Conversation),
            description = stringResource(R.string.setting_privacy_tip),
        ) {
            navController.navigation(SettingDestination.Conversation)
        }

        Box(modifier = Modifier.height(16.dp))

        SettingTile(
            title = stringResource(R.string.Phone_Number),
        ) {
            navController.navigation(SettingDestination.PhoneNumber)
        }

        SettingTile(
            title = stringResource(R.string.Phone_Contact),
        ) {
            navController.navigation(SettingDestination.MobileContact)
        }

        supportsOreo {
            var incognitoEnable by LocalContext.current.defaultSharedPreferences
                .booleanValueAsState(
                    key = Constants.Account.PREF_INCOGNITO_KEYBOARD,
                    defaultValue = false,
                )
            Box(modifier = Modifier.height(16.dp))
            SettingTile(
                title = stringResource(R.string.Incognito_Keyboard),
                description = stringResource(R.string.setting_incognito_prompt),
                trailing = {
                    Switch(
                        checked = incognitoEnable,
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MixinAppTheme.colors.accent,
                                uncheckedThumbColor = MixinAppTheme.colors.unchecked,
                                checkedTrackColor = MixinAppTheme.colors.accent,
                                uncheckedTrackColor = MixinAppTheme.colors.unchecked,
                            ),
                        onCheckedChange = null,
                    )
                },
            ) {
                incognitoEnable = !incognitoEnable
            }
        }

        Box(modifier = Modifier.height(16.dp))

        SettingTile(
            title = stringResource(R.string.fingerprint_lock),
            trailing = {
                val fingerprintLock by LocalContext.current.defaultSharedPreferences
                    .intValueAsState(
                        key = Constants.Account.PREF_APP_AUTH,
                        defaultValue = -1,
                    )
                Text(
                    text =
                        stringResource(
                            id =
                                when (fingerprintLock) {
                                    0 -> R.string.Enable_immediately
                                    1 -> R.string.Enable_after_1_minute
                                    2 -> R.string.Enable_after_30_minutes
                                    else -> R.string.Disabled
                                },
                        ),
                )
            },
        ) {
            navController.navigation(SettingDestination.AppAuthSetting)
        }
    }
}
