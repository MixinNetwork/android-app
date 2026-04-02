package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
        private const val PREF_VERIFY_MOBILE_REMINDER_SNOOZE = "pref_verify_mobile_reminder_snooze"
        private const val ARGS_ENABLE_SNOOZE = "args_enable_snooze"

        @Volatile
        private var isShowing = false

        fun showSafely(
            fragmentManager: androidx.fragment.app.FragmentManager,
            enableSnooze: Boolean = true,
        ): Boolean {
            if (isShowing) return false

            val fragment = VerifyMobileReminderBottomSheetDialogFragment().apply {
                arguments = android.os.Bundle().apply {
                    putBoolean(ARGS_ENABLE_SNOOZE, enableSnooze)
                }
            }

            return try {
                if (fragmentManager.isStateSaved) {
                    false
                } else {
                    isShowing = true
                    fragment.show(fragmentManager, TAG)
                    true
                }
            } catch (_: Exception) {
                isShowing = false
                false
            }
        }

        fun shouldShow(context: Context): Boolean {
            val account = Session.getAccount() ?: return false
            if (Session.hasPhone().not()) return false
            val lastSnoozeTimeMillis = context.defaultSharedPreferences.getLong(PREF_VERIFY_MOBILE_REMINDER_SNOOZE, 0)
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
            val phoneVerifiedAt = account.phoneVerifiedAt
            if (phoneVerifiedAt.isNullOrBlank()) return true
            val verifiedAtMillis = runCatching {
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
        val enableSnooze = arguments?.getBoolean(ARGS_ENABLE_SNOOZE, true) ?: true
        val phoneNumber = if (Session.hasPhone()) Session.getAccount()?.phone else null
        val hasPhoneNumber = !phoneNumber.isNullOrBlank()
        val titleResId = if (hasPhoneNumber) R.string.Verify_Mobile_Number else R.string.verify_mobile_reminder_title
        val actionResId = if (hasPhoneNumber) R.string.Verify_Now else R.string.verify_mobile_reminder_action
        val subtitleResId = if (hasPhoneNumber) R.string.Verify_Mobile_Number_Desc else R.string.verify_mobile_reminder_desc
        MixinAppTheme {
            ReminderPage(
                contentImage = R.drawable.bg_reminder_verify_mobile,
                title = titleResId,
                actionStr = actionResId,
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
                contentSlot = {
                    Text(
                        text = stringResource(subtitleResId),
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
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
