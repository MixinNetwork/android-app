package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
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
            asset: AssetItem? = null,
            assetId: String? = null,
            snapshotId: String? = null
        ) = TransactionBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_SNAPSHOT, snapshotItem)
            putParcelable(ARGS_ASSET, asset)
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _titleBinding: ViewTitleBinding? = null
    private val titleBinding get() = requireNotNull(_titleBinding)

    private val walletViewModel by viewModels<WalletViewModel>()

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelable(ARGS_SNAPSHOT) }
    private val asset: AssetItem? by lazy { requireArguments().getParcelable(ARGS_ASSET) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentTransactionBinding.bind(View.inflate(context, R.layout.fragment_transaction, null).apply { isClickable = true })
        _titleBinding = ViewTitleBinding.bind(binding.titleView)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        titleBinding.leftIb.setOnClickListener { dismiss() }
        initView(this, binding, titleBinding, lifecycleScope, walletViewModel, assetId, snapshotId, asset, snapshot)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _titleBinding = null
    }
}
