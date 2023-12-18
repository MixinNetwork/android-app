package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentWarningBottomSheetBinding
import one.mixin.android.databinding.FragmentWatingBottomSheetBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RestoreTransactionJob
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject
@AndroidEntryPoint
class WaitingBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WaitingBottomSheetDialogFragment"

        fun newInstance() = WaitingBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentWatingBottomSheetBinding::inflate)

    @Inject
    lateinit var jobManager: MixinJobManager

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            continueTv.setOnClickListener {
                dismiss()
            }
        }

        jobManager.addJobInBackground(RestoreTransactionJob())
    }
}
