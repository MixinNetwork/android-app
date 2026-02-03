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
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.AddPhoneBeforeFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Account
import java.time.Instant

@AndroidEntryPoint
class VerifyMobileReminderBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG: String = "VerifyMobileReminderBottomSheetDialogFragment"
        private const val PREF_VERIFY_MOBILE_REMINDER_SNOOZE: String = "pref_verify_mobile_reminder_snooze"
        private const val ARGS_SUBTITLE_RES_ID: String = "args_subtitle_res_id"
        private const val ARGS_ENABLE_SNOOZE: String = "args_enable_snooze"

        fun newInstance(subtitleResId: Int = R.string.Verify_Mobile_Number_Desc): VerifyMobileReminderBottomSheetDialogFragment {
            return VerifyMobileReminderBottomSheetDialogFragment().apply {
                arguments = android.os.Bundle().apply {
                    putInt(ARGS_SUBTITLE_RES_ID, subtitleResId)
                    putBoolean(ARGS_ENABLE_SNOOZE, true)
                }
            }
        }

        fun newInstance(
            subtitleResId: Int = R.string.Verify_Mobile_Number_Desc,
            enableSnooze: Boolean,
        ): VerifyMobileReminderBottomSheetDialogFragment {
            return VerifyMobileReminderBottomSheetDialogFragment().apply {
                arguments = android.os.Bundle().apply {
                    putInt(ARGS_SUBTITLE_RES_ID, subtitleResId)
                    putBoolean(ARGS_ENABLE_SNOOZE, enableSnooze)
                }
            }
        }

        fun shouldShow(context: Context): Boolean {
            val account = Session.getAccount() ?: return false
            val lastSnoozeTimeMillis: Long = context.defaultSharedPreferences.getLong(PREF_VERIFY_MOBILE_REMINDER_SNOOZE, 0)
            if (System.currentTimeMillis() - lastSnoozeTimeMillis < Constants.INTERVAL_7_DAYS) return false
            return shouldShowWithoutSnooze(account)
        }

        fun shouldShowForBuy(context: Context): Boolean {
            val account = Session.getAccount() ?: return false
            if (account.phone.isBlank()) return true
            return shouldShowWithoutSnooze(account)
        }

        private fun shouldShowWithoutSnooze(account: Account): Boolean {
            if (account.phone.isBlank()) return true
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
        val subtitleResId: Int = arguments?.getInt(ARGS_SUBTITLE_RES_ID, R.string.Verify_Mobile_Number_Desc)
            ?: R.string.Verify_Mobile_Number_Desc
        val enableSnooze: Boolean = arguments?.getBoolean(ARGS_ENABLE_SNOOZE, true) ?: true
        val phoneNumber: String? = if (Session.hasPhone()) Session.getAccount()?.phone else null
        MixinAppTheme {
            ReminderPage(
                R.drawable.bg_reminder_verify_mobile,
                R.string.Verify_Mobile_Number,
                subtitleResId,
                R.string.Verify_Now,
                action = {
                    dismissAllowingStateLoss()
                    if (phoneNumber.isNullOrBlank()) {
                        navTo(AddPhoneBeforeFragment.newInstance(), AddPhoneBeforeFragment.TAG)
                    } else {
                        navTo(VerifyFragment.newInstance(VerifyFragment.FROM_PHONE, phoneNumber), VerifyFragment.TAG)
                    }
                },
                dismiss = {
                    if (enableSnooze) {
                        requireContext().defaultSharedPreferences.putLong(
                            PREF_VERIFY_MOBILE_REMINDER_SNOOZE,
                            System.currentTimeMillis(),
                        )
                    }
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
