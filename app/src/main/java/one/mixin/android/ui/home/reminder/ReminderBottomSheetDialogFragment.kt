package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Account.PREF_BATTERY_OPTIMIZE
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.compose.theme.languageBasedImage
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isBatteryOptimizationRestricted
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openBatteryOptimizationSetting
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class ReminderBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ReminderBottomSheetDialogFragment"
        private const val PREF_NOTIFICATION_ON = "pref_notification_on"
        const val PREF_NEW_VERSION = "pref_new_version"
        const val ARGS_POPUP_TYPE = "args_popup_type"

        fun newInstance(
            popupType: PopupType,
        ) =
            ReminderBottomSheetDialogFragment().withArgs {
                putString(ARGS_POPUP_TYPE, popupType::class.java.simpleName)
            }

        fun getType(context: Context): PopupType? {
            val sharedPreferences = context.defaultSharedPreferences
            val account = Session.getAccount()
            val appVersion = account?.system?.messenger

            if (appVersion != null) {
                val lastNewVersionReminderTime = sharedPreferences.getLong(PREF_NEW_VERSION, 0)
                if (System.currentTimeMillis() - lastNewVersionReminderTime > INTERVAL_24_HOURS &&
                    compareVersions(appVersion.version.replace("v", "")) > 0
                ) {
                    return PopupType.NewVersionReminder
                }
            }

            val lastNotificationReminderTime = sharedPreferences.getLong(PREF_NOTIFICATION_ON, 0)
            if (System.currentTimeMillis() - lastNotificationReminderTime > INTERVAL_48_HOURS &&
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                return PopupType.NotificationPermissionReminder
            }

            val lastBatteryOptimizationReminderTime = sharedPreferences.getLong(PREF_BATTERY_OPTIMIZE, 0)
            if (System.currentTimeMillis() - lastBatteryOptimizationReminderTime > INTERVAL_24_HOURS &&
                context.isBatteryOptimizationRestricted()
            ) {
                return PopupType.BatteryOptimizationReminder
            }

            return null
        }

        private fun compareVersions(remoteVersion: String): Int {
            val remoteVersionParts = remoteVersion.split(".")
            val localVersionParts = BuildConfig.VERSION_NAME.split(".")

            val maxLength = maxOf(remoteVersionParts.size, localVersionParts.size)

            for (i in 0 until maxLength) {
                val v1 = remoteVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
                val v2 = localVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (v1 != v2) {
                    return v1.compareTo(v2)
                }
            }
            return 0
        }

        @StringRes
        private fun getBatteryOptimizationContentResId(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                R.string.setting_battery_optimize_title_one_ui_above_s
            } else {
                R.string.setting_battery_optimize_title_one_ui_below_s
            }
        }
    }

    private val popupType by lazy {
        val typeName = requireArguments().getString(ARGS_POPUP_TYPE)
        when (typeName) {
            PopupType.NewVersionReminder::class.java.simpleName -> PopupType.NewVersionReminder
            PopupType.NotificationPermissionReminder::class.java.simpleName -> PopupType.NotificationPermissionReminder
            PopupType.BatteryOptimizationReminder::class.java.simpleName -> PopupType.BatteryOptimizationReminder
            else -> throw IllegalArgumentException("Unknown PopupType")
        }
    }

    fun isForType(type: PopupType): Boolean = popupType::class == type::class

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            when (popupType) {
                is PopupType.NewVersionReminder -> {
                    ReminderPage(
                        contentImage = R.drawable.bg_reminber_version,
                        title = R.string.New_Update_Available,
                        actionStr = R.string.Update_Now,
                        action = {
                            Session.getAccount()?.system?.messenger?.let { it ->
                                (requireActivity() as? MainActivity)?.showUpdate(it.releaseUrl)
                            }
                            dismissAllowingStateLoss()
                        },
                        dismiss = {
                            requireContext().defaultSharedPreferences.putLong(
                                PREF_NEW_VERSION,
                                System.currentTimeMillis(),
                            )
                            dismissAllowingStateLoss()
                        },
                        contentSlot = {
                            Text(
                                text = stringResource(R.string.New_Update_Available_desc),
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                }

                is PopupType.NotificationPermissionReminder -> {
                    ReminderPage(
                        contentImage = languageBasedImage(
                            R.drawable.bg_reminder_notifaction,
                            R.drawable.bg_reminder_notifaction_cn,
                        ),
                        title = R.string.Turn_On_Notifications,
                        actionStr = R.string.Enable_Notifications,
                        action = {
                            requireContext().openNotificationSetting()
                            dismissAllowingStateLoss()
                        },
                        dismiss = {
                            requireContext().defaultSharedPreferences.putLong(
                                PREF_NOTIFICATION_ON,
                                System.currentTimeMillis(),
                            )
                            dismissAllowingStateLoss()
                        },
                        contentSlot = {
                            Text(
                                text = stringResource(R.string.notification_content),
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                }

                is PopupType.BatteryOptimizationReminder -> {
                    ReminderPage(
                        contentImage = R.drawable.bg_reminder_battery_optimization,
                        title = R.string.Battery_Optimization,
                        actionStr = R.string.Go_settings,
                        action = {
                            requireContext().defaultSharedPreferences.putLong(
                                PREF_BATTERY_OPTIMIZE,
                                System.currentTimeMillis(),
                            )
                            requireContext().openBatteryOptimizationSetting()
                            dismissAllowingStateLoss()
                        },
                        dismiss = {
                            requireContext().defaultSharedPreferences.putLong(
                                PREF_BATTERY_OPTIMIZE,
                                System.currentTimeMillis(),
                            )
                            dismissAllowingStateLoss()
                        },
                        contentSlot = {
                            Text(
                                text = batteryOptimizationContent(),
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    private fun batteryOptimizationContent(): String {
        return getString(getBatteryOptimizationContentResId())
            .replace("<b>", "")
            .replace("</b>", "")
            .replace("**", "")
    }

    sealed class PopupType {
        object NewVersionReminder : PopupType()
        object NotificationPermissionReminder : PopupType()
        object BatteryOptimizationReminder : PopupType()
    }
}
