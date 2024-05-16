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
    SettingPageScaffold(title = stringResource(id = R.string.fingerprint_lock)) {
        var isSupportWithErrorInfo by remember {
            mutableStateOf<Pair<Boolean, String?>?>(null)
        }

        val context = LocalContext.current

        var fingerPrintEnabled by remember {
            context.defaultSharedPreferences
        }.intValueAsState(
            key = Constants.Account.PREF_APP_AUTH,
            defaultValue = FINGERPRINT_DISABLED,
        )

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
                            fingerPrintEnabled = FINGERPRINT_DISABLED
                        }
                    }

                    override fun onAuthenticationFailed() {
                        fingerPrintEnabled = FINGERPRINT_DISABLED
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        fingerPrintEnabled = FINGERPRINT_ENABLED_IMMEDIATELY
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
                            uncheckedThumbColor = MixinAppTheme.colors.unchecked,
                            checkedTrackColor = MixinAppTheme.colors.accent,
                            uncheckedTrackColor = MixinAppTheme.colors.unchecked,
                        ),
                    onCheckedChange = null,
                )
            },
        ) {
            isSupportWithErrorInfo =
                BiometricUtil.isSupportWithErrorInfo(context, BiometricManager.Authenticators.BIOMETRIC_WEAK)
            val isSupport = isSupportWithErrorInfo?.first == true
            if (!isSupport) {
                fingerPrintEnabled = FINGERPRINT_DISABLED
                return@SettingTile
            }
            if (fingerPrintEnabled != FINGERPRINT_DISABLED) {
                fingerPrintEnabled = FINGERPRINT_DISABLED
            } else {
                val activity = context.findFragmentActivityOrNull() ?: return@SettingTile
                showAppAuthPrompt(
                    activity,
                    context.getString(R.string.Confirm_fingerprint),
                    context.getString(R.string.Cancel),
                    authCallback,
                )
            }
        }

        if (isSupportWithErrorInfo != null) {
            Text(
                text = isSupportWithErrorInfo?.second ?: "",
                color = MixinAppTheme.colors.red,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            )
        }

        if (fingerPrintEnabled != FINGERPRINT_DISABLED) {
            Text(
                text = stringResource(id = R.string.Auto_Lock),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.accent,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            FingerprintRadioGroup(
                index = fingerPrintEnabled,
                onCheckedChange = {
                    fingerPrintEnabled = it
                },
            )
        }
    }
}

@Composable
private fun FingerprintRadioGroup(
    index: Int,
    onCheckedChange: (Int) -> Unit,
) {
    FingerprintRadioButton(
        checked = index == FINGERPRINT_ENABLED_IMMEDIATELY,
        title = stringResource(id = R.string.Immediately),
    ) {
        onCheckedChange(FINGERPRINT_ENABLED_IMMEDIATELY)
    }

    FingerprintRadioButton(
        checked = index == FINGERPRINT_ENABLED_AFTER_1_MINUTES,
        title = stringResource(id = R.string.After_1_minute),
    ) {
        onCheckedChange(FINGERPRINT_ENABLED_AFTER_1_MINUTES)
    }

    FingerprintRadioButton(
        checked = index == FINGERPRINT_ENABLED_AFTER_30_MINUTES,
        title = stringResource(id = R.string.After_30_minutes),
    ) {
        onCheckedChange(FINGERPRINT_ENABLED_AFTER_30_MINUTES)
    }
}

@Composable
private fun FingerprintRadioButton(
    checked: Boolean,
    title: String,
    onChecked: () -> Unit,
) {
    Row(
        Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clickable {
                if (!checked) {
                    onChecked()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(16.dp))
        RadioButton(
            selected = checked,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MixinAppTheme.colors.accent,
                ),
        )
        Box(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
        Box(modifier = Modifier.width(16.dp))
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
