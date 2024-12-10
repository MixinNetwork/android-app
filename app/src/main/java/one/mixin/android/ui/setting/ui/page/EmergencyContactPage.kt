package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.MixinBottomSheetDialog
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.setting.EmergencyViewModel
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.PinEmergencyBottomSheetDialog
import one.mixin.android.vo.Account
import timber.log.Timber

@Composable
fun EmergencyContactPage() {
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
            modifier = Modifier.background(MixinAppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(36.dp))
            Image(
                modifier =
                    Modifier
                        .height(83.dp)
                        .width(92.dp),
                painter = painterResource(R.drawable.ic_emergency),
                contentDescription = null,
            )
            Box(modifier = Modifier.height(28.dp))
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(id = R.string.setting_emergency_content),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Box(modifier = Modifier.height(24.dp))
        }

        Box(modifier = Modifier.height(24.dp))

        var hasEmergencyContact by remember {
            mutableStateOf(Session.hasEmergencyContact())
        }

        OnPageResumeFromBackStack {
            // recheck hasEmergencyContact when page resume from backstack.
            hasEmergencyContact = Session.hasEmergencyContact()
        }

        if (!hasEmergencyContact) {
            var showEnableTip by remember {
                mutableStateOf(false)
            }

            val context = LocalContext.current

            SettingTile(
                title = stringResource(id = R.string.Enable_Emergency_Contact),
                titleColor = MixinAppTheme.colors.accent,
            ) {
                showEnableTip = true
            }
        } else {
            HasEmergencyLayout(
                onEmergencyAccountRemoved = {
                    hasEmergencyContact = false
                },
            )
        }
    }
}

@Composable
private fun HasEmergencyLayout(
    onEmergencyAccountRemoved: () -> Unit,
) {
    ShowEmergencyButton()

    Box(modifier = Modifier.height(24.dp))

    val context = LocalContext.current
    SettingTile(
        title = stringResource(id = R.string.Change_emergency_contact),
    ) {
        val activity = context.findFragmentActivityOrNull()
        activity?.supportFragmentManager?.inTransaction {
            setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom,
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom,
            )
                .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                .addToBackStack(null)
        }
    }

    Box(modifier = Modifier.height(24.dp))

    RemoveEmergencyButton(onEmergencyAccountRemoved)
}

@Composable
private fun RemoveEmergencyButton(
    onEmergencyAccountRemoved: () -> Unit,
) {
    var showConfirmDialog by remember {
        mutableStateOf(false)
    }

    var showPinBottomSheet by remember {
        mutableStateOf(false)
    }

    var showLoading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<EmergencyViewModel>()

    SettingTile(
        title = stringResource(id = R.string.Remove_emergency_contact),
        titleColor = MixinAppTheme.colors.red,
        trailing = {
            if (showLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        },
    ) {
        showConfirmDialog = true
    }

    if (showConfirmDialog) {
        MixinAlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            text = {
                Text(text = stringResource(id = R.string.Remove_emergency_contact_confirmation))
            },
            confirmText = stringResource(id = R.string.Delete),
            onConfirmClick = {
                showConfirmDialog = false
                showPinBottomSheet = true
            },
            dismissText = stringResource(id = R.string.Cancel),
        )
    }

    if (showPinBottomSheet) {
        PinEmergencyBottomSheetDialog(
            onDismissRequest = { showPinBottomSheet = false },
            onConfirm = { pinCode ->
                scope.launch {
                    showLoading = true
                    handleMixinResponse(
                        invokeNetwork = { viewModel.deleteEmergency(pinCode) },
                        successBlock = { response ->
                            val a = response.data as Account
                            Session.storeAccount(a)
                            Session.setHasEmergencyContact(a.hasEmergencyContact)
                            Timber.d("delete emergency contact success: ${a.hasEmergencyContact}")
                            onEmergencyAccountRemoved()
                        },
                        exceptionBlock = {
                            showLoading = false
                            Timber.e(it, "delete emergency contact failed")
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            showLoading = false
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun ShowEmergencyButton() {
    var showPinBottomSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<EmergencyViewModel>()

    val navigator = LocalSettingNav.current

    var loading by remember { mutableStateOf(false) }

    if (showPinBottomSheet) {
        PinEmergencyBottomSheetDialog(
            onDismissRequest = {
                showPinBottomSheet = false
            },
            onConfirm = { pinCode ->
                scope.launch {
                    loading = true
                    handleMixinResponse(
                        invokeNetwork = { viewModel.showEmergency(pinCode) },
                        successBlock = { response ->
                            val user = response.data as one.mixin.android.vo.User
                            navigator.viewEmergencyContact(user)
                        },
                        exceptionBlock = {
                            loading = false
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            loading = false
                        },
                    )
                }
            },
        )
    }

    SettingTile(
        title = stringResource(id = R.string.View_emergency_contact),
        trailing = {
            if (loading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        },
    ) {
        showPinBottomSheet = true
    }
}

@Composable
private fun PinEmergencyBottomSheetDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (pinCode: String) -> Unit,
) {
    MixinBottomSheetDialog(createDialog = {
        PinEmergencyBottomSheetDialog.newInstance().apply {
            pinEmergencyCallback =
                object : PinEmergencyBottomSheetDialog.PinEmergencyCallback() {
                    override fun onSuccess(pinCode: String) {
                        onConfirm(pinCode)
                    }
                }
            setCallback(
                object : BiometricBottomSheetDialogFragment.Callback() {
                    override fun onDismiss(success: Boolean) {
                        if (!success) {
                            onDismissRequest()
                        }
                    }
                },
            )
        }
    })
}

@Composable
private fun OnPageResumeFromBackStack(onResume: () -> Unit) {
    val context = LocalContext.current

    val fragmentManager =
        remember {
            context.findFragmentActivityOrNull()?.supportFragmentManager
        }

    val initialBackStackEntryCount =
        remember {
            fragmentManager?.backStackEntryCount ?: 0
        }

    DisposableEffect(fragmentManager) {
        val backStackChanged = {
            Timber.d("back stack changed $initialBackStackEntryCount ${fragmentManager?.backStackEntryCount}")
            if (fragmentManager?.backStackEntryCount == initialBackStackEntryCount) {
                onResume()
            }
        }
        fragmentManager?.addOnBackStackChangedListener(backStackChanged)
        onDispose {
            fragmentManager?.removeOnBackStackChangedListener(backStackChanged)
        }
    }
}
