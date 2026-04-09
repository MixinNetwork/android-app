package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class RecoveryReminderBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG: String = "RecoveryReminderBottomSheetDialogFragment"
        private const val PREF_RECOVERY_REMINDER_SNOOZE = "pref_recovery_reminder_snooze"
        private const val PREF_RECOVERY_REMINDER_DEBUG_ALLOW_ONCE = "pref_recovery_reminder_debug_allow_once"
        private const val ARGS_ENABLE_SNOOZE = "args_enable_snooze"
        private const val ARGS_CONTINUE_ON_DISMISS = "args_continue_on_dismiss"
        private const val ARGS_DISMISS_TEXT = "args_dismiss_text"

        @Volatile
        private var isShowing = false
        private var pendingContinueAction: (() -> Unit)? = null

        private data class ReminderEntryConfig(
            val dismissTextRes: Int,
            val enableSnooze: Boolean,
            val continueOnDismiss: Boolean,
        ) {
            fun toBundle(): Bundle =
                Bundle().apply {
                    putBoolean(ARGS_ENABLE_SNOOZE, enableSnooze)
                    putBoolean(ARGS_CONTINUE_ON_DISMISS, continueOnDismiss)
                    putInt(ARGS_DISMISS_TEXT, dismissTextRes)
                }
        }

        private data class ReminderDialogArgs(
            val enableSnooze: Boolean,
            val continueOnDismiss: Boolean,
            val dismissTextRes: Int,
        ) {
            companion object {
                fun from(arguments: Bundle?): ReminderDialogArgs {
                    return ReminderDialogArgs(
                        enableSnooze = arguments?.getBoolean(ARGS_ENABLE_SNOOZE, true) ?: true,
                        continueOnDismiss = arguments?.getBoolean(ARGS_CONTINUE_ON_DISMISS, false) ?: false,
                        dismissTextRes = arguments?.getInt(ARGS_DISMISS_TEXT, R.string.Skip) ?: R.string.Skip,
                    )
                }
            }
        }

        private val homeEntryConfig = ReminderEntryConfig(
            dismissTextRes = R.string.Not_Now,
            enableSnooze = true,
            continueOnDismiss = false,
        )

        private val logoutEntryConfig = ReminderEntryConfig(
            dismissTextRes = R.string.Cancel,
            enableSnooze = false,
            continueOnDismiss = false,
        )

        fun showForHome(fragmentManager: FragmentManager): Boolean {
            if (!RecoveryReminderState.shouldShowOnHome()) return false
            return showSafely(fragmentManager, entryConfig = homeEntryConfig)
        }

        fun showForRiskAction(
            fragmentManager: FragmentManager,
            onContinue: (() -> Unit)? = null,
        ): Boolean {
            if (!RecoveryReminderState.shouldShowOnRiskAction()) return false
            val entryConfig = ReminderEntryConfig(
                dismissTextRes = R.string.Skip,
                enableSnooze = false,
                continueOnDismiss = onContinue != null,
            )
            return showSafely(
                fragmentManager,
                entryConfig = entryConfig,
                onContinue = onContinue,
            )
        }

        fun showForLogout(fragmentManager: FragmentManager): Boolean {
            if (!RecoveryReminderState.shouldShowOnLogout()) return false
            return showSafely(
                fragmentManager,
                entryConfig = logoutEntryConfig,
            )
        }

        fun allowDebugShowOnce(
            context: Context,
            allow: Boolean = true,
        ) {
            context.defaultSharedPreferences.putBoolean(PREF_RECOVERY_REMINDER_DEBUG_ALLOW_ONCE, allow)
        }

        private fun snoozeFromNow(context: Context) {
            context.defaultSharedPreferences.putLong(
                PREF_RECOVERY_REMINDER_SNOOZE,
                System.currentTimeMillis(),
            )
        }
        fun shouldShowOnRiskAction(): Boolean = RecoveryReminderState.shouldShowOnRiskAction()

        private fun showSafely(
            fragmentManager: FragmentManager,
            entryConfig: ReminderEntryConfig,
            onContinue: (() -> Unit)? = null,
        ): Boolean {
            if (isShowing) return false
            pendingContinueAction = if (entryConfig.continueOnDismiss) onContinue else null
            val fragment = RecoveryReminderBottomSheetDialogFragment().apply {
                arguments = entryConfig.toBundle()
            }
            return try {
                if (fragmentManager.isStateSaved) {
                    false
                } else {
                    isShowing = true
                    fragment.show(fragmentManager, TAG)
                    true
                }
            } catch (e: Exception) {
                isShowing = false
                pendingContinueAction = null
                false
            }
        }

        object RecoveryReminderState {
            private fun recoveryMethodCount(): Int {
                val hasPhone = Session.hasPhone()
                val hasMnemonic = Session.saltExported()
                val hasRecoveryContact = hasPhone && Session.hasEmergencyContact()
                var count = 0
                if (hasPhone) count++
                if (hasMnemonic) count++
                if (hasRecoveryContact) count++
                return count
            }

            fun shouldShowOnHome(context: Context = MixinApplication.appContext): Boolean {
                if (consumeDebugShowOnce(context)) return true
                if (recoveryMethodCount() <= 1) return false
                return isSnoozeExpired(context)
            }

            fun shouldShowOnRiskAction(context: Context = MixinApplication.appContext): Boolean {
                return recoveryMethodCount() == 0
            }

            fun shouldShowOnLogout(context: Context = MixinApplication.appContext): Boolean {
                return recoveryMethodCount() == 0
            }

            private fun isSnoozeExpired(context: Context): Boolean {
                val lastSnoozeTimeMillis = context.defaultSharedPreferences.getLong(PREF_RECOVERY_REMINDER_SNOOZE, 0)
                return System.currentTimeMillis() - lastSnoozeTimeMillis >= Constants.INTERVAL_7_DAYS
            }

            private fun consumeDebugShowOnce(context: Context): Boolean {
                val shouldShow = context.defaultSharedPreferences.getBoolean(PREF_RECOVERY_REMINDER_DEBUG_ALLOW_ONCE, false)
                if (!shouldShow) return false
                context.defaultSharedPreferences.putBoolean(PREF_RECOVERY_REMINDER_DEBUG_ALLOW_ONCE, false)
                return true
            }
        }
    }

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
        isShowing = true
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        pendingContinueAction = null
    }

    override fun dismiss() {
        isShowing = false
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        isShowing = false
        super.dismissAllowingStateLoss()
    }

    @Composable
    override fun ComposeContent() {
        val dialogArgs = ReminderDialogArgs.from(arguments)
        val context = LocalContext.current
        val recoveryKitHelpUrl = stringResource(R.string.recovery_kit_help_url)
        MixinAppTheme {
            ReminderPage(
                contentImage = R.drawable.bg_recovery_kit,
                title = R.string.Recovery_Kit,
                actionStr = R.string.Set_Up_Now,
                dismissStr = dialogArgs.dismissTextRes,
                action = {
                    openRecoveryKit()
                },
                dismiss = {
                    handleDismiss(dialogArgs)
                },
                contentSlot = {
                    HighlightedTextWithClick(
                        fullText = stringResource(R.string.Recovery_Kit_Alert),
                        modifier = Modifier.fillMaxWidth(),
                        stringResource(R.string.More_Information),
                        onTextClick = { context.openUrl(recoveryKitHelpUrl) }
                    )
                },
                extraContent = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ReminderItem(
                            stringResource(R.string.Mobile_Number),
                            if (Session.hasPhone()) stringResource(R.string.Added) else stringResource(R.string.Not_Added),
                            checked = Session.hasPhone()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ReminderItem(
                            stringResource(R.string.Mnemonic_Phrase),
                            if (Session.saltExported()) stringResource(R.string.Backed_Up) else stringResource(R.string.Backup),
                            checked = Session.saltExported()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ReminderItem(
                            stringResource(R.string.Recovery_Contact),
                            if (Session.hasEmergencyContact()) stringResource(R.string.Added) else stringResource(R.string.Not_Added),
                            checked = Session.hasEmergencyContact()
                        )
                    }
                },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    private fun openRecoveryKit() {
        pendingContinueAction = null
        dismissAllowingStateLoss()
        SettingActivity.showRecoveryKit(requireContext())
    }

    private fun handleDismiss(dialogArgs: ReminderDialogArgs) {
        val continueAction = takePendingContinueAction(continueOnDismiss = dialogArgs.continueOnDismiss)
        if (dialogArgs.enableSnooze) {
            snoozeFromNow(requireContext())
        }
        dismissAllowingStateLoss()
        continueAction?.invoke()
    }

    private fun takePendingContinueAction(continueOnDismiss: Boolean): (() -> Unit)? {
        val continueAction = if (continueOnDismiss) pendingContinueAction else null
        pendingContinueAction = null
        return continueAction
    }
}
