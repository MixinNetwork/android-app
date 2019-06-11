package one.mixin.android.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_emergency_contact_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.inTransaction
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.Session
import one.mixin.android.widget.BottomSheet

class EmergencyContactTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "EmergencyContactTipBottomSheetDialogFragment"

        fun newInstance() = EmergencyContactTipBottomSheetDialogFragment()
    }

    override fun setupDialog(dialog: Dialog?, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_emergency_contact_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.continue_tv.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                activity?.supportFragmentManager?.inTransaction {
                    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                        .addToBackStack(null)
                }
            } else {
                WalletActivity.show(requireActivity())
            }
            dismiss()
        }

        val url = getString(R.string.setting_emergency_url)
        val target = getString(R.string.setting_emergency)
        val desc = getString(R.string.setting_emergency_desc)
        contentView.desc_tv.highlightLinkText(desc, arrayOf(target), arrayOf(url))
    }
}