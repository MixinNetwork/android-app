package one.mixin.android.ui.wallet

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import one.mixin.android.R
import one.mixin.android.extension.dp

class LoadingProgressDialogFragment : AppCompatDialogFragment() {
    companion object {
        const val TAG = "LoadingProgressDialogFragment"
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.LoadingProgressDialog)
        dialog.setContentView(R.layout.fragment_loading_progress_dialog)
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = 300.dp
            layoutParams.height = 180.dp
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.bg_round_window_16dp)
        }
        isCancelable = false
        return dialog
    }
}
