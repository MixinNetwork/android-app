package one.mixin.android.ui.home.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
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

        @Volatile
        private var isShowing = false

        fun showSafely(
            fragmentManager: androidx.fragment.app.FragmentManager,
        ): Boolean {
            if (isShowing) return false

            val fragment = VerifyMobileReminderBottomSheetDialogFragment()

            return try {
                fragment.showNow(fragmentManager, TAG)
                true
            } catch (_: Exception) {
                isShowing = false
                false
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
        val subtitleResId = R.string.verify_mobile_reminder_desc
        val phoneNumber = if (Session.hasPhone()) Session.getAccount()?.phone else null
        MixinAppTheme {
            ReminderPage(
                contentImage = R.drawable.ic_moblie_number,
                title = R.string.verify_mobile_reminder_title,
                actionStr = R.string.verify_mobile_reminder_action,
                action = {
                    dismissAllowingStateLoss()
                    navTo(AddPhoneBeforeFragment.newInstance(), AddPhoneBeforeFragment.TAG)
                },
                dismiss = {
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
