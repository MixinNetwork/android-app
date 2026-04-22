package one.mixin.android.ui.wallet.alert

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
import one.mixin.android.ui.wallet.alert.components.AlertFrequencyBottom
import one.mixin.android.ui.wallet.alert.components.AlertTypeBottom
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency
import one.mixin.android.ui.wallet.alert.vo.AlertType
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class AlertSelectionBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AlertSelectionBottomSheetDialogFragment"
        private const val ARGS_MODE = "args_mode"
        private const val ARGS_SELECTED_TYPE = "args_selected_type"
        private const val ARGS_SELECTED_FREQUENCY = "args_selected_frequency"

        private const val MODE_TYPE = "type"
        private const val MODE_FREQUENCY = "frequency"

        fun newTypeInstance(selectedType: AlertType) = AlertSelectionBottomSheetDialogFragment().withArgs {
            putString(ARGS_MODE, MODE_TYPE)
            putString(ARGS_SELECTED_TYPE, selectedType.value)
        }

        fun newFrequencyInstance(selectedFrequency: AlertFrequency) = AlertSelectionBottomSheetDialogFragment().withArgs {
            putString(ARGS_MODE, MODE_FREQUENCY)
            putString(ARGS_SELECTED_FREQUENCY, selectedFrequency.value)
        }
    }

    private val mode by lazy { requireArguments().getString(ARGS_MODE).orEmpty() }
    private val selectedType by lazy { AlertType.values().first { it.value == requireArguments().getString(ARGS_SELECTED_TYPE) } }
    private val selectedFrequency by lazy { AlertFrequency.values().first { it.value == requireArguments().getString(ARGS_SELECTED_FREQUENCY) } }

    var onTypeSelected: ((AlertType) -> Unit)? = null
    var onFrequencySelected: ((AlertFrequency) -> Unit)? = null

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
            if (mode == MODE_TYPE) {
                AlertTypeBottom(selectedType, { type ->
                    dismiss()
                    onTypeSelected?.invoke(type)
                }, {
                    dismiss()
                })
            } else {
                AlertFrequencyBottom(selectedFrequency, { frequency ->
                    dismiss()
                    onFrequencySelected?.invoke(frequency)
                }, {
                    dismiss()
                })
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }
}
