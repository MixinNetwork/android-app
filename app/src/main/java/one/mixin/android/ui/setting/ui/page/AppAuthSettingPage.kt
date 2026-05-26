package one.mixin.android.ui.setting.ui.page

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.intValueAsState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.ui.auth.showAppAuthPrompt
import one.mixin.android.util.BiometricUtil

private const val FINGERPRINT_DISABLED = -1
private const val FINGERPRINT_ENABLED_IMMEDIATELY = 0
private const val FINGERPRINT_ENABLED_AFTER_1_MINUTES = 1
private const val FINGERPRINT_ENABLED_AFTER_30_MINUTES = 2

@Composable
fun AppAuthSettingPage() {
    val context = LocalContext.current
    var fingerPrintEnabled by remember {
        context.defaultSharedPreferences
    }.intValueAsState(
        key = Constants.Account.PREF_APP_AUTH,
        defaultValue = FINGERPRINT_DISABLED,
    )

    AppAuthSettingPageContent(
        fingerPrintEnabled = fingerPrintEnabled,
        onFingerPrintEnabledChange = { fingerPrintEnabled = it }
    )
}

@Composable
fun AppAuthSettingPageContent(
    fingerPrintEnabled: Int,
    onFingerPrintEnabledChange: (Int) -> Unit,
) {
    val isInPreview = LocalInspectionMode.current
    SettingPageScaffold(title = stringResource(id = R.string.fingerprint_lock)) {
        var isSupportWithErrorInfo by remember {
            mutableStateOf<Pair<Boolean, String?>?>(null)
        }

        val context = LocalContext.current
        val confirmFingerprint = stringResource(R.string.Confirm_fingerprint)
        val cancel = stringResource(R.string.Cancel)
        val unlockWithFingerprint = stringResource(R.string.Unlock_with_fingerprint)

        val authCallback =
            remember {
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                            errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                        ) {
                            onFingerPrintEnabledChange(FINGERPRINT_DISABLED)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        onFingerPrintEnabledChange(FINGERPRINT_DISABLED)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onFingerPrintEnabledChange(FINGERPRINT_ENABLED_IMMEDIATELY)
                    }
                }
            }

        SettingTile(
            title = stringResource(id = R.string.fingerprint_lock),
            description = stringResource(id = R.string.unlock_with_fingerprint_desc),
            trailing = {
                Switch(
                    checked = fingerPrintEnabled != FINGERPRINT_DISABLED,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MixinAppTheme.colors.accent,
                            checkedTrackColor = MixinAppTheme.colors.accent.copy(alpha = 0.5f),
                        ),
                    onCheckedChange = {
                        if (it) {
                            if (isInPreview) {
                                onFingerPrintEnabledChange(FINGERPRINT_ENABLED_IMMEDIATELY)
                                return@Switch
                            }
                            val activity = context.findFragmentActivityOrNull()
                            if (activity != null) {
                                if (isSupportWithErrorInfo == null) {
                                    isSupportWithErrorInfo = BiometricUtil.isSupportWithErrorInfo(context)
                                }
                                val result = isSupportWithErrorInfo!!
                                if (result.first) {
                                    showAppAuthPrompt(
                                        activity,
                                        confirmFingerprint,
                                        cancel,
                                        unlockWithFingerprint,
                                        authCallback,
                                    )
                                } else {
                                    fingerPrintEnabled // trigger recompose?
                                }
                            }
                        } else {
                            onFingerPrintEnabledChange(FINGERPRINT_DISABLED)
                        }
                    },
                )
            },
        )

        if (fingerPrintEnabled != FINGERPRINT_DISABLED) {
            FingerprintRadioButton(
                checked = fingerPrintEnabled == FINGERPRINT_ENABLED_IMMEDIATELY,
                title = stringResource(id = R.string.Immediately),
            ) {
                onFingerPrintEnabledChange(FINGERPRINT_ENABLED_IMMEDIATELY)
            }

            FingerprintRadioButton(
                checked = fingerPrintEnabled == FINGERPRINT_ENABLED_AFTER_1_MINUTES,
                title = stringResource(id = R.string.Minutes, 1),
            ) {
                onFingerPrintEnabledChange(FINGERPRINT_ENABLED_AFTER_1_MINUTES)
            }

            FingerprintRadioButton(
                checked = fingerPrintEnabled == FINGERPRINT_ENABLED_AFTER_30_MINUTES,
                title = stringResource(id = R.string.Minutes, 30),
            ) {
                onFingerPrintEnabledChange(FINGERPRINT_ENABLED_AFTER_30_MINUTES)
            }
        }
    }
}

@Composable
private fun FingerprintRadioButton(
    checked: Boolean,
    title: String,
    onCheckedChange: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable {
                    onCheckedChange()
                }
                .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = checked,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MixinAppTheme.colors.accent,
                    unselectedColor = MixinAppTheme.colors.unchecked,
                ),
        )

        Box(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )

        Box(modifier = Modifier.width(16.dp))
    }
}

@Composable
@Preview
fun AppAuthSettingPagePreview() {
    MixinAppTheme {
        AppAuthSettingPageContent(
            fingerPrintEnabled = FINGERPRINT_ENABLED_IMMEDIATELY,
            onFingerPrintEnabledChange = {}
        )
    }
}

@Composable
@Preview
fun FingerprintRadioButtonPreview() {
    MixinAppTheme {
        Surface(
            color = MixinAppTheme.colors.backgroundWindow,
        ) {
            FingerprintRadioButton(
                checked = true,
                title = "Fingerprint",
            ) {
            }
        }
    }
}
