package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_transaction.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import javax.inject.Inject

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

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelable<SnapshotItem>(ARGS_SNAPSHOT) }
    private val asset: AssetItem? by lazy { requireArguments().getParcelable<AssetItem>(ARGS_ASSET) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transaction, container, false).apply {
            isClickable = true
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        initView(this, container, lifecycleScope, walletViewModel, assetId, snapshotId, asset, snapshot)
    }
}
