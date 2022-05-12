package one.mixin.android.ui.setting.ui.page

import androidx.biometric.BiometricManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.setting.BiometricTimeFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.ui.compose.*
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil

@Composable
fun PinSettingPage() {
    SettingPageScaffold(title = stringResource(id = R.string.pin)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(36.dp))
            Image(
                painter = painterResource(id = MixinAppTheme.drawables.emergencyAvatar),
                contentDescription = null,
                modifier = Modifier
                    .height(83.dp)
                    .width(92.dp),
            )
            Box(modifier = Modifier.height(20.dp))


            val context = LocalContext.current

            HighlightLinkText(
                source = stringResource(R.string.wallet_pin_tops_desc),
                texts = arrayOf(stringResource(R.string.wallet_pin_tops)),
                links = arrayOf(Constants.HelpLink.TIP),
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textSubtitle,
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
            defaultValue = false
        )

        val biometricInterval by remember {
            context.defaultSharedPreferences
        }.longValueAsState(Constants.BIOMETRIC_INTERVAL, Constants.BIOMETRIC_INTERVAL_DEFAULT)

        var isSupportWithErrorInfo by remember {
            mutableStateOf<Pair<Boolean, String?>?>(null)
        }

        var showBiometricsDialog by remember { mutableStateOf(false) }

        SettingTile(
            title = stringResource(R.string.wallet_pay_with_biometrics),
            trailing = {
                Switch(checked = enableBiometrics, onCheckedChange = null)
            },
        ) {
            isSupportWithErrorInfo =
                BiometricUtil.isSupportWithErrorInfo(context, BiometricManager.Authenticators.BIOMETRIC_STRONG)
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
                    callback = object : BiometricBottomSheetDialogFragment.Callback() {
                        override fun onSuccess() {
                            enableBiometrics = true
                            context.defaultSharedPreferences.putLong(
                                Constants.BIOMETRIC_PIN_CHECK,
                                System.currentTimeMillis()
                            )
                        }

                        override fun onDismiss() {
                            showBiometricsDialog = false
                        }
                    }
                }
            })
        }

        if (enableBiometrics) {
            val navController = LocalSettingNav.current
            SettingTile(
                title = stringResource(R.string.wallet_pin_pay_interval),
                trailing = {
                    val hour = biometricInterval / BiometricTimeFragment.X_HOUR.toFloat()
                    if (hour < 1) {
                        Text(
                            text = context.resources.getQuantityString(
                                R.plurals.wallet_pin_pay_interval_minutes,
                                (hour * 60).toInt(),
                                (hour * 60).toInt(),
                            )
                        )
                    } else {
                        Text(
                            text = context.resources.getQuantityString(
                                R.plurals.wallet_pin_pay_interval_hours,
                                hour.toInt(),
                                hour.toInt()
                            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textSubtitle
        )

        Box(modifier = Modifier.height(16.dp))

        SettingTile(title = stringResource(R.string.wallet_password_change)) {

        }

    }
}
