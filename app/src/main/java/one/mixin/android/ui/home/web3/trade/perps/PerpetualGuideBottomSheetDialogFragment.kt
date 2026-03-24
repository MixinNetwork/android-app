package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class PerpetualGuideBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpetualGuideBottomSheetDialogFragment"
        private const val ARGS_INITIAL_TAB = "args_initial_tab"

        const val TAB_OVERVIEW = 0
        const val TAB_LONG = 1
        const val TAB_SHORT = 2
        const val TAB_LEVERAGE = 3
        const val TAB_POSITION = 4

        fun newInstance(initialTab: Int = TAB_OVERVIEW) = PerpetualGuideBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_INITIAL_TAB, initialTab)
            }
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
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
        val initialTab = arguments?.getInt(ARGS_INITIAL_TAB, TAB_OVERVIEW) ?: TAB_OVERVIEW
        MixinAppTheme {
            PerpetualGuidePage(
                initialTab = initialTab,
                pop = {
                    dismiss()
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }
}
