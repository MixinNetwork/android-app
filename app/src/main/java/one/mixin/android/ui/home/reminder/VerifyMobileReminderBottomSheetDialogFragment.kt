package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
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
        private const val PREF_VERIFY_MOBILE_REMINDER_DEBUG_ALLOW_ONCE = "pref_verify_mobile_reminder_debug_allow_once"
        private const val ARGS_SUBTITLE_RES_ID = "args_subtitle_res_id"
        private const val ARGS_ENABLE_SNOOZE = "args_enable_snooze"

        @Volatile
        private var isShowing = false

        fun showSafely(
            fragmentManager: FragmentManager,
            subtitleResId: Int = R.string.verify_mobile_reminder_desc,
            enableSnooze: Boolean = true,
        ): Boolean {
            if (isShowing) return false

            val fragment = VerifyMobileReminderBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARGS_SUBTITLE_RES_ID, subtitleResId)
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

        fun allowDebugShowOnce(context: Context) {
            context.defaultSharedPreferences.putBoolean(PREF_VERIFY_MOBILE_REMINDER_DEBUG_ALLOW_ONCE, true)
        }

        fun shouldShow(context: Context): Boolean {
            if (consumeDebugShowOnce(context)) return true
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

        private fun consumeDebugShowOnce(context: Context): Boolean {
            val shouldShow = context.defaultSharedPreferences.getBoolean(PREF_VERIFY_MOBILE_REMINDER_DEBUG_ALLOW_ONCE, false)
            if (!shouldShow) return false
            context.defaultSharedPreferences.putBoolean(PREF_VERIFY_MOBILE_REMINDER_DEBUG_ALLOW_ONCE, false)
            return true
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
        val subtitleResId = if (hasPhoneNumber) {
            R.string.Verify_Mobile_Number_Desc
        } else {
            arguments?.getInt(ARGS_SUBTITLE_RES_ID, R.string.verify_mobile_reminder_desc)
                ?: R.string.verify_mobile_reminder_desc
        }
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
                    Box(modifier = Modifier.padding(bottom = 44.dp)) {
                        Text(
                            text = stringResource(subtitleResId),
                            color = MixinAppTheme.colors.textAssist,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
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
}
