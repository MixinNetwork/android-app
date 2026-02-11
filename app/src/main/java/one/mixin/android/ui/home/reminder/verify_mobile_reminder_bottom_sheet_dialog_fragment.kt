package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.SystemUIManager
import java.time.Instant

@AndroidEntryPoint
class VerifyMobileReminderBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG: String = "VerifyMobileReminderBottomSheetDialogFragment"
        private const val PREF_VERIFY_MOBILE_REMINDER_SNOOZE: String = "pref_verify_mobile_reminder_snooze"

        fun shouldShow(context: Context): Boolean {
            if (!Session.hasPhone()) return false
            val account = Session.getAccount() ?: return false
            val lastSnoozeTimeMillis: Long = context.defaultSharedPreferences.getLong(PREF_VERIFY_MOBILE_REMINDER_SNOOZE, 0)
            if (System.currentTimeMillis() - lastSnoozeTimeMillis < Constants.INTERVAL_7_DAYS) return false
            val phoneVerifiedAt: String? = account.phoneVerifiedAt
            if (phoneVerifiedAt.isNullOrBlank()) return true
            val verifiedAtMillis: Long = runCatching {
                Instant.parse(phoneVerifiedAt).toEpochMilli()
            }.getOrNull() ?: return true
            return System.currentTimeMillis() - verifiedAtMillis > Constants.INTERVAL_60_DAYS
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
            ReminderPage(
                R.drawable.bg_reminder_verify_mobile,
                R.string.Verify_Mobile_Number,
                if(Session.hasPhone()) R.string.Verify_Mobile_Number_Desc else R.string.Verify_Mobile_Number_Security_Desc,
                R.string.Verify_Now,
                action = {
                    dismissAllowingStateLoss()
                    LandingActivity.showVerifyMobile(requireContext())
                },
                dismiss = {
                    requireContext().defaultSharedPreferences.putLong(
                        PREF_VERIFY_MOBILE_REMINDER_SNOOZE,
                        System.currentTimeMillis(),
                    )
                    dismissAllowingStateLoss()
                },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }
}
