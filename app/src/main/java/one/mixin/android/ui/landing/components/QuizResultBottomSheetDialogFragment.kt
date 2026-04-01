package one.mixin.android.ui.landing.components

import android.annotation.SuppressLint
import android.app.Dialog
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
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class QuizResultBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QuizResultBottomSheetDialogFragment"
        private const val ARGS_IS_CORRECT = "args_is_correct"

        fun newInstance(isCorrect: Boolean) = QuizResultBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_CORRECT, isCorrect)
        }
    }

    private val isCorrect by lazy { requireArguments().getBoolean(ARGS_IS_CORRECT) }

    var onCorrectAction: (() -> Unit)? = null
    var onWrongAction: (() -> Unit)? = null

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
        MixinAppTheme {
            QuizResultBottomSheetContent(
                isCorrect = isCorrect,
                onCorrectAction = {
                    dismiss()
                    onCorrectAction?.invoke()
                },
                onWrongAction = {
                    dismiss()
                    onWrongAction?.invoke()
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
