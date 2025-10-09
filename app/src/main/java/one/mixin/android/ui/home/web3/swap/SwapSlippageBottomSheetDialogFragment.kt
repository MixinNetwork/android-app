package one.mixin.android.ui.home.web3.swap

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class SwapSlippageBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "SwapSlippageBottomSheetDialogFragment"

        const val ARGS_SLIPPAGE_BPS = "args_slippage_bps"

        fun newInstance(
            slippageBps: Int,
        ) =
            SwapSlippageBottomSheetDialogFragment().withArgs {
                putInt(ARGS_SLIPPAGE_BPS, slippageBps)
            }
    }

    private val slippageBps by lazy { requireArguments().getInt(ARGS_SLIPPAGE_BPS) }


    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        SwapSlippagePage(slippageBps, {
            dismiss()
        }) { bps ->
            onSlippage?.invoke(bps)
            dismiss()
        }
    }


    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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

    override fun onDetach() {
        super.onDetach()
        if (activity is WalletConnectActivity || activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private var onSlippage: ((Int) -> Unit)? = null

    fun setOnSlippage(callback: (Int) -> Unit): SwapSlippageBottomSheetDialogFragment {
        onSlippage = callback
        return this
    }
}
