package one.mixin.android.ui.setting.ui.page

import androidx.biometric.BiometricManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.HighlightStarLinkText
import one.mixin.android.compose.MixinBottomSheetDialog
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.booleanValueAsState
import one.mixin.android.compose.longValueAsState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.setting.BiometricTimeFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil

@Composable
fun PinSettingPage() {
    SettingPageScaffold(title = stringResource(id = R.string.PIN)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(36.dp))
            Image(
                painter = painterResource(R.drawable.ic_emergency),
                contentDescription = null,
                modifier =
                    Modifier
                        .height(83.dp)
                        .width(92.dp),
            )
            Box(modifier = Modifier.height(20.dp))

            val context = LocalContext.current

            HighlightStarLinkText(
                source = stringResource(R.string.wallet_pin_tops_desc),
                links = arrayOf(Constants.HelpLink.TIP),
                textStyle =
                    TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.Center,
                    ),
                onClick = {
                    context.openUrl(it)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Box(modifier = Modifier.height(24.dp))
        }

        Box(modifier = Modifier.height(20.dp))

        val context = LocalContext.current

        var enableBiometrics by remember {
            context.defaultSharedPreferences
        }.booleanValueAsState(
            key = Constants.Account.PREF_BIOMETRICS,
            defaultValue = false,
        )

        val biometricInterval by remember {
            context.defaultSharedPreferences
        }.longValueAsState(Constants.BIOMETRIC_INTERVAL, Constants.BIOMETRIC_INTERVAL_DEFAULT)

        var isSupportWithErrorInfo by remember {
            mutableStateOf<Pair<Boolean, String?>?>(null)
        }

        var randomKeyboardEnabled by LocalContext.current.defaultSharedPreferences
            .booleanValueAsState(
                key = Constants.Account.PREF_RANDOM,
                defaultValue = false,
            )
        Box(modifier = Modifier.height(16.dp))
        SettingTile(
            title = stringResource(R.string.Random_keyboard),
            trailing = {
                Switch(
                    checked = randomKeyboardEnabled,
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
            randomKeyboardEnabled = !randomKeyboardEnabled
        }

        var showBiometricsDialog by remember { mutableStateOf(false) }

        Box(modifier = Modifier.height(16.dp))
        SettingTile(
            title = stringResource(R.string.Pay_with_Biometrics),
            trailing = {
                Switch(
                    checked = enableBiometrics,
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
                BiometricUtil.isSupportWithErrorInfo(
                    context,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG,
                )
            val isSupport = isSupportWithErrorInfo?.first == true
            if (!isSupport) {
                enableBiometrics = false
                BiometricUtil.deleteKey(context)
                return@SettingTile
            }

            if (enableBiometrics) {
                enableBiometrics = false
                BiometricUtil.deleteKey(context)
            } else {
                showBiometricsDialog = true
            }
        }

        if (showBiometricsDialog) {
            MixinBottomSheetDialog(createDialog = {
                PinBiometricsBottomSheetDialogFragment.newInstance(true).apply {
                    setCallback(
                        object : BiometricBottomSheetDialogFragment.Callback() {
                            override fun onDismiss(success: Boolean) {
                                if (success) {
                                    enableBiometrics = true
                                    context.defaultSharedPreferences.putLong(
                                        Constants.BIOMETRIC_PIN_CHECK,
                                        System.currentTimeMillis(),
                                    )
                                } else {
                                    showBiometricsDialog = false
                                }
                            }
                        },
                    )
                }
            })
        }
        val navController = LocalSettingNav.current

        if (enableBiometrics) {
            SettingTile(
                title = stringResource(R.string.Pay_with_PIN_interval),
                trailing = {
                    val hour = biometricInterval / BiometricTimeFragment.X_HOUR.toFloat()
                    if (hour < 1) {
                        Text(
                            text =
                                context.resources.getQuantityString(
                                    R.plurals.Minute,
                                    (hour * 60).toInt(),
                                    (hour * 60).toInt(),
                                ),
                        )
                    } else {
                        Text(
                            text =
                                context.resources.getQuantityString(
                                    R.plurals.Hour,
                                    hour.toInt(),
                                    hour.toInt(),
                                ),
                        )
                    }
                },
            ) {
                navController.navigation(SettingDestination.BiometricTime)
            }
        }

        Box(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.wallet_enable_biometric_pay_prompt),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist,
        )

        Box(modifier = Modifier.height(16.dp))

        SettingTile(title = stringResource(R.string.Change_PIN)) {
            TipActivity.show(context as SettingActivity, TipType.Change)
        }
    }
}
