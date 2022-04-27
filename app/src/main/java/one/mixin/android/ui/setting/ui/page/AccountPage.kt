package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun AccountPage() {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_account))
                },
                navigationIcon = {
                    MixinBackButton()
                }
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            val navController = LocalSettingNav.current
            AccountTile(stringResource(R.string.setting_privacy)) {
                navController.navigation(SettingDestination.AccountPrivacy)
            }
            AccountTile(stringResource(R.string.setting_security)) {
                navController.navigation(SettingDestination.AccountSecurity)
            }
            ChangeNumberButton()
            Box(modifier = Modifier.height(16.dp))
            AccountTile(stringResource(R.string.setting_delete_account)) {

            }
        }
    }
}

@Composable
private fun ChangeNumberButton() {
    val openDialog = remember {
        mutableStateOf(false)
    }
    if (openDialog.value) {
        AlertDialog(
            text = {
                Text(stringResource(R.string.profile_modify_number))
            },
            confirmButton = {
                val context = LocalContext.current
                TextButton(onClick = {
                    openDialog.value = false

                    val activity = context.findFragmentActivityOrNull()

                    if (Session.getAccount()?.hasPin == true) {
                        activity?.supportFragmentManager?.inTransaction {
                            setCustomAnimations(
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_bottom,
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_bottom
                            )
                                .add(
                                    R.id.container,
                                    VerifyFragment.newInstance(VerifyFragment.FROM_PHONE)
                                )
                                .addToBackStack(null)
                        }
                    } else {
                        activity?.supportFragmentManager?.inTransaction {
                            setCustomAnimations(
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_bottom,
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_bottom
                            )
                                .add(
                                    R.id.container,
                                    WalletPasswordFragment.newInstance(),
                                    WalletPasswordFragment.TAG
                                )
                                .addToBackStack(null)
                        }
                    }


                }) {
                    Text(stringResource(R.string.profile_phone))
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismissRequest = {
                openDialog.value = false
            },
        )
    }
    AccountTile(stringResource(R.string.profile_phone)) {
        openDialog.value = true
    }
}

@Composable
private fun AccountTile(
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
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary
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
