package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_emergency_contact_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.inTransaction
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.Session
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class EmergencyContactTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "EmergencyContactTipBottomSheetDialogFragment"

        fun newInstance() = EmergencyContactTipBottomSheetDialogFragment()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_emergency_contact_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.continue_tv.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                activity?.supportFragmentManager?.inTransaction {
                    setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                    )
                        .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                        .addToBackStack(null)
                }
            } else {
                parentFragmentManager.inTransaction {
                    setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R
                            .anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                    )
                        .add(R.id.container, WalletPasswordFragment.newInstance(), WalletPasswordFragment.TAG)
                        .addToBackStack(null)
                }
            }
            dismiss()
        }

        contentView.scroll_view.post {
            val childHeight = contentView.scroll_content.height
            val isScrollable = contentView.scroll_view.height <
                childHeight + contentView.scroll_view.paddingTop + contentView.scroll_view.paddingBottom
            if (isScrollable) {
                contentView.image_view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = context?.dpToPx(12f) ?: 0
                    bottomMargin = context?.dpToPx(12f) ?: 0
                }
                contentView.continue_tv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = context?.dpToPx(20f) ?: 0
                    bottomMargin = context?.dpToPx(20f) ?: 0
                }
            }
        }
    }
}
