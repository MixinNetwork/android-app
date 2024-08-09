package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinBottomSheetDialog
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.longValueAsState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import timber.log.Timber

private val presetDurations = floatArrayOf(.25f, .5f, 1f, 2f, 6f, 12f, 24f)
private const val X_HOUR = 1000 * 60 * 60

@Composable
fun BiometricTimePage() {
    SettingPageScaffold(title = stringResource(id = R.string.Pay_with_PIN_interval)) {
        Box(modifier = Modifier.height(32.dp))

        val context = LocalContext.current

        val values: ArrayList<String> =
            remember {
                val strings = arrayListOf<String>()
                presetDurations.forEach { v ->
                    if (v < 1) {
                        strings.add(
                            context.resources.getQuantityString(
                                R.plurals.Minute,
                                (v * 60).toInt(),
                                (v * 60).toInt(),
                            ),
                        )
                    } else {
                        strings.add(context.resources.getQuantityString(R.plurals.Hour, v.toInt(), v.toInt()))
                    }
                }
                strings
            }

        var biometricInterval by remember {
            context.defaultSharedPreferences
        }.longValueAsState(
            Constants.BIOMETRIC_INTERVAL,
            Constants.BIOMETRIC_INTERVAL_DEFAULT,
        )

        val selectedIndex =
            remember(biometricInterval) {
                val intervalHour = biometricInterval.toFloat() / X_HOUR
                presetDurations.indexOfFirst { it == intervalHour }
            }

        values.forEachWithIndex { index, value ->

            var showDialog by remember {
                mutableStateOf(false)
            }

            SettingTile(
                title = value,
                trailing = {
                    if (selectedIndex == index) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_check_black_24dp),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .padding(4.dp),
                        )
                    }
                },
            ) {
                if (index >= presetDurations.size || selectedIndex == index) {
                    return@SettingTile
                }
                showDialog = true
            }

            if (showDialog) {
                MixinBottomSheetDialog(
                    createDialog = {
                        PinBiometricsBottomSheetDialogFragment.newInstance(false).apply {
                            setCallback(
                                object : BiometricBottomSheetDialogFragment.Callback() {
                                    override fun onDismiss(success: Boolean) {
                                        if (success) {
                                            val intervalMillis = (presetDurations[index] * X_HOUR).toLong()
                                            biometricInterval = intervalMillis
                                            Timber.d("set biometric interval $intervalMillis")
                                        } else {
                                            showDialog = false
                                        }
                                    }
                                },
                            )
                        }
                    },
                    tag = PinBiometricsBottomSheetDialogFragment.TAG,
                )
            }
        }

        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            text = stringResource(id = R.string.wallet_pin_pay_interval_tips),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
        )
    }
}
