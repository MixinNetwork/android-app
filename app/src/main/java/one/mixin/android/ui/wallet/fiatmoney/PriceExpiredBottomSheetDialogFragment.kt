package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPriceExpiredBottomSheetBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
@AndroidEntryPoint
class PriceExpiredBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PriceExpiredBottomSheetDialogFragment"

        fun newInstance() = PriceExpiredBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentPriceExpiredBottomSheetBinding::inflate)

    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.apply {
            refreshTv.setOnClickListener {
                // TODO refresh
                dismiss()
            }
            cancelTv.setOnClickListener { dismiss() }

            oldPrice.text = "1 USDC = 1.005 USD"
            newPrice.text = "1 USDC = 1.006 USD"
        }
    }

    private val mBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                try {
                    dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }
}
