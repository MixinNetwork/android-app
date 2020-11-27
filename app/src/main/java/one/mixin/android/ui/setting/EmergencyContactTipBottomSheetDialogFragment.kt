package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentEmergencyContactBottomBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.inTransaction
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class EmergencyContactTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "EmergencyContactTipBottomSheetDialogFragment"

        fun newInstance() = EmergencyContactTipBottomSheetDialogFragment()
    }

    private var _binding: FragmentEmergencyContactBottomBinding? = null
    private val binding get() = requireNotNull(_binding)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentEmergencyContactBottomBinding.bind(View.inflate(context, R.layout.fragment_emergency_contact_bottom, null))
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            continueTv.setOnClickListener {
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

            scrollView.post {
                val childHeight = scrollContent.height
                val isScrollable = scrollView.height <
                    childHeight + scrollView.paddingTop + scrollView.paddingBottom
                if (isScrollable) {
                    imageView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = context?.dpToPx(12f) ?: 0
                        bottomMargin = context?.dpToPx(12f) ?: 0
                    }
                    continueTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = context?.dpToPx(20f) ?: 0
                        bottomMargin = context?.dpToPx(20f) ?: 0
                    }
                }
            }
        }
    }
}
