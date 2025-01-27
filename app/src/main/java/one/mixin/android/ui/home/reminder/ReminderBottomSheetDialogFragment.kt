package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.Constants.INTERVAL_7_DAYS
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.compose.theme.languageBasedImage
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class ReminderBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ReminderBottomSheetDialogFragment"
        private const val PREF_NOTIFICATION_ON = "pref_notification_on"
        private const val PREF_EMERGENCY_CONTACT = "pref_emergency_contact"
        private const val PREF_BACKUP_MNEMONIC = "pref_backup_mnemonic"
        const val PREF_NEW_VERSION = "pref_new_version"
        const val ARGS_POPUP_TYPE = "args_popup_type"

        fun newInstance(
            popupType: PopupType,
        ) =
            ReminderBottomSheetDialogFragment().withArgs {
                putString(ARGS_POPUP_TYPE, popupType::class.java.simpleName)
            }

        fun getType(context: Context, totalUsd: Int): PopupType? {
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

            val isBackupMnemonicReminderNeeded = !Session.saltExported() && Session.isAnonymous()
            val lastBackupMnemonicReminderTime = sharedPreferences.getLong(PREF_BACKUP_MNEMONIC, 0)
            if (isBackupMnemonicReminderNeeded && System.currentTimeMillis() - lastBackupMnemonicReminderTime > INTERVAL_24_HOURS) {
                return PopupType.BackupMnemonicReminder
            }

            val lastNotificationReminderTime = sharedPreferences.getLong(PREF_NOTIFICATION_ON, 0)
            if (System.currentTimeMillis() - lastNotificationReminderTime > INTERVAL_48_HOURS &&
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                return PopupType.NotificationPermissionReminder
            }

            val lastEmergencyContactReminderTime = sharedPreferences.getLong(PREF_EMERGENCY_CONTACT, 0)
            if (System.currentTimeMillis() - lastEmergencyContactReminderTime > INTERVAL_7_DAYS &&
                totalUsd >= 100 && Session.hasPhone() &&
                (Session.getAccount()?.hasEmergencyContact == true).not()
            ) {
                return PopupType.RestoreContactReminder
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
    }

    private val popupType by lazy {
        val typeName = requireArguments().getString(ARGS_POPUP_TYPE)
        when (typeName) {
            PopupType.NewVersionReminder::class.java.simpleName -> PopupType.NewVersionReminder
            PopupType.BackupMnemonicReminder::class.java.simpleName -> PopupType.BackupMnemonicReminder
            PopupType.NotificationPermissionReminder::class.java.simpleName -> PopupType.NotificationPermissionReminder
            PopupType.RestoreContactReminder::class.java.simpleName -> PopupType.RestoreContactReminder
            else -> throw IllegalArgumentException("Unknown PopupType")
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme {
                    when (popupType) {
                        is PopupType.NewVersionReminder -> {
                            ReminderPage(R.drawable.bg_reminber_version, R.string.New_Update_Available, R.string.New_Update_Available_desc, R.string.Update_Now, action = {
                                Session.getAccount()?.system?.messenger?.let { it -> (requireActivity() as? MainActivity)?.showUpdate(it.releaseUrl) }
                                dismissAllowingStateLoss()
                            }, dismiss = {
                                requireContext().defaultSharedPreferences.putLong(
                                    PREF_NEW_VERSION,
                                    System.currentTimeMillis(),
                                )
                                dismissAllowingStateLoss()
                            })
                        }

                        is PopupType.BackupMnemonicReminder -> {
                            ReminderPage(R.drawable.bg_reminber_mnemonic, R.string.Backup_Mnemonic_Phrase, R.string.Backup_Mnemonic_Phrase_desc, R.string.Backup_Now, action = {
                                SettingActivity.showMnemonicPhrase(context)
                                dismissAllowingStateLoss()
                            }, dismiss = {
                                requireContext().defaultSharedPreferences.putLong(
                                    PREF_BACKUP_MNEMONIC,
                                    System.currentTimeMillis(),
                                )
                                dismissAllowingStateLoss()
                            })
                        }

                        is PopupType.NotificationPermissionReminder -> {
                            ReminderPage(
                                languageBasedImage(
                                    R.drawable.bg_reminder_notifaction,
                                    R.drawable.bg_reminder_notifaction_cn
                                ), R.string.Turn_On_Notifications, R.string.notification_content, R.string.Enable_Notifications, action = {
                                    requireContext().openNotificationSetting()
                                    dismissAllowingStateLoss()
                                }, dismiss = {
                                    requireContext().defaultSharedPreferences.putLong(
                                        PREF_NOTIFICATION_ON,
                                        System.currentTimeMillis(),
                                    )
                                    dismissAllowingStateLoss()
                                })
                        }

                        is PopupType.RestoreContactReminder -> {
                            ReminderPage(R.drawable.bg_reminber_recovery_contact, R.string.Emergency_Contact, R.string.setting_emergency_content, R.string.Continue, action = {
                                SettingActivity.showEmergencyContact(requireContext())
                                dismissAllowingStateLoss()
                            }, dismiss = {
                                requireContext().defaultSharedPreferences.putLong(
                                    PREF_EMERGENCY_CONTACT,
                                    System.currentTimeMillis(),
                                )
                                dismissAllowingStateLoss()
                            })
                        }

                    }
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight = requireContext().screenHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }

    sealed class PopupType {
        object NewVersionReminder : PopupType()
        object BackupMnemonicReminder : PopupType()
        object NotificationPermissionReminder : PopupType()
        object RestoreContactReminder : PopupType()
    }
}