package one.mixin.android.ui.wallet

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
import one.mixin.android.databinding.FragmentErrorBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.withArgs
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import timber.log.Timber
@AndroidEntryPoint
class ErrorFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ErrorFragment"
        const val MESSAGE = "message"

        fun newInstance(message: String) = ErrorFragment().withArgs {
            putString(MESSAGE, message)
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val binding by viewBinding(FragmentErrorBinding::inflate)

    private lateinit var contentView: View

    private val message: String by lazy { requireArguments().getString(MESSAGE)!! }

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
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        dialog.setContentView(contentView)
        val behavior = ((contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, requireContext().dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
        binding.apply {
            errorInfo.text = message
            closeIv.setOnClickListener {
                dismiss()
            }
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
