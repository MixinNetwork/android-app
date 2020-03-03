package one.mixin.android.ui.conversation.location

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class LocationBottomDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "LocationBottomDialogFragment"

        fun newInstance() = LocationBottomDialogFragment().withArgs {
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_location_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}
