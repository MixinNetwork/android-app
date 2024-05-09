package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType

@Composable
fun SecurityPage() {
    val navController = LocalSettingNav.current
    val context = LocalContext.current

    SettingPageScaffold(title = stringResource(id = R.string.Security)) {
        SettingTile(title = stringResource(R.string.PIN)) {
            if (Session.getAccount()?.hasPin == true) {
                navController.navigation(SettingDestination.PinSetting)
            } else {
                TipActivity.show(context as SettingActivity, TipType.Create)
            }
        }

        Box(modifier = Modifier.height(16.dp))
        SettingTile(title = stringResource(R.string.Emergency_Contact)) {
            navController.navigation(SettingDestination.EmergencyContact)
        }

        Box(modifier = Modifier.height(16.dp))
        SettingTile(title = stringResource(R.string.Authorizations)) {
            navController.navigation(SettingDestination.Authentications)
        }

        Box(modifier = Modifier.height(16.dp))
        SettingTile(title = stringResource(R.string.Logs)) {
            navController.navigation(SettingDestination.PinLogs)
        }
    }
}

@Composable
@Preview
fun SecurityPagePreview() {
    MixinAppTheme {
        SecurityPage()
    }
}
