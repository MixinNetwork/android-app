package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem

@AndroidEntryPoint
class TransactionFragment : BaseFragment(), TransactionInterface {
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
        ) = TransactionFragment().withArgs {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTransactionBinding.inflate(layoutInflater, container, false).apply {
            root.isClickable = true
        }
        _titleBinding = ViewTitleBinding.bind(binding.titleView)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.leftIb.setOnClickListener { activity?.onBackPressed() }
        initView(this, binding, titleBinding, lifecycleScope, walletViewModel, assetId, snapshotId, asset, snapshot)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _titleBinding = null
    }
}
