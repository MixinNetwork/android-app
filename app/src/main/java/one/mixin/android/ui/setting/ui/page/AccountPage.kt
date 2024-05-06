package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.inTransaction
import one.mixin.android.session.Session
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType

@Composable
fun AccountPage() {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(stringResource(R.string.Account))
                },
                navigationIcon = {
                    MixinBackButton()
                },
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState()),
        ) {
            val navController = LocalSettingNav.current
            AccountTile(stringResource(R.string.Privacy)) {
                navController.navigation(SettingDestination.AccountPrivacy)
            }
            AccountTile(stringResource(R.string.Security)) {
                navController.navigation(SettingDestination.AccountSecurity)
            }
            ChangeNumberButton()
            Box(modifier = Modifier.height(16.dp))
            AccountTile(stringResource(R.string.Delete_my_account)) {
                navController.navigation(SettingDestination.DeleteAccount)
            }
        }
    }
}

@Composable
private fun ChangeNumberButton() {
    val openDialog =
        remember {
            mutableStateOf(false)
        }
    if (openDialog.value) {
        val context = LocalContext.current

        MixinAlertDialog(
            text = {
                Text(stringResource(R.string.profile_modify_number))
            },
            confirmText = stringResource(R.string.Change_Phone_Number),
            onConfirmClick = {
                openDialog.value = false

                val activity = context.findFragmentActivityOrNull()

                if (Session.getAccount()?.hasPin == true) {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                        )
                            .add(
                                R.id.container,
                                VerifyFragment.newInstance(VerifyFragment.FROM_PHONE),
                            )
                            .addToBackStack(null)
                    }
                } else {
                    activity?.let { TipActivity.show(it, TipType.Create, true) }
                }
            },
            dismissText = stringResource(android.R.string.cancel),
            onDismissRequest = {
                openDialog.value = false
            },
        )
    }
    AccountTile(stringResource(R.string.Change_Phone_Number)) {
        openDialog.value = true
    }
}

@Composable
private fun AccountTile(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(60.dp)
                .background(MixinAppTheme.colors.background)
                .clickable { onClick() }
                .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Preview
@Composable
fun AccountPagePreview() {
    MixinAppTheme {
        AccountPage()
    }
}
