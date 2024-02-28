package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransactionBottomSheetDialogFragment : MixinBottomSheetDialogFragment(), TransactionInterface {
    companion object {
        const val TAG = "TransactionFragment"
        const val ARGS_SNAPSHOT = "args_snapshot"
        const val ARGS_ASSET_ID = "args_asset_id"
        const val ARGS_SNAPSHOT_ID = "args_snapshot_id"

        fun newInstance(
            snapshotItem: SnapshotItem? = null,
            asset: TokenItem? = null,
            assetId: String? = null,
            snapshotId: String? = null,
        ) = TransactionBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_SNAPSHOT, snapshotItem)
            putParcelable(ARGS_ASSET, asset)
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val binding by viewBinding(FragmentTransactionBinding::inflate)

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelableCompat(ARGS_SNAPSHOT, SnapshotItem::class.java) }
    private val asset: TokenItem? by lazy { requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        binding.titleView.leftIb.setOnClickListener { dismiss() }
        binding.titleView.rightAnimator.visibility = View.GONE
        initView(this, binding, lifecycleScope, walletViewModel, assetId, snapshotId, asset, snapshot)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
    }
}
