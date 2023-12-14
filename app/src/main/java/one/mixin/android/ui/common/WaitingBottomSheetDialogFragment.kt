package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.lifecycle.lifecycleScope
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

class WaitingBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WaitingBottomSheetDialogFragment"
        private const val ARGS_ASSET_ID = "asset_id"
        private const val ARGS_SNAPSHOT_ID = "snapshot_id"

        fun newInstance(assetId: String, snapshotId: String) = WaitingBottomSheetDialogFragment().withArgs {
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    private val binding by viewBinding(FragmentWatingBottomSheetBinding::inflate)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val assetId: String by lazy {
        requireNotNull(requireArguments().getString(ARGS_ASSET_ID))
    }

    private val snapshotId: String by lazy {
        requireNotNull(requireArguments().getString(ARGS_SNAPSHOT_ID))
    }

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
            activity?.addFragment(
                this@WaitingBottomSheetDialogFragment,
                TransactionFragment.newInstance(
                    assetId = assetId,
                    snapshotId = snapshotId,
                ),
                TransactionFragment.TAG,
            )
        }

        jobManager.addJobInBackground(RestoreTransactionJob())
    }
}
