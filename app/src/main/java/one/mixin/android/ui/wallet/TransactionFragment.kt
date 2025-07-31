package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class TransactionFragment : BaseFragment(R.layout.fragment_transaction), TransactionInterface {
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
        ) = TransactionFragment().withArgs {
            putParcelable(ARGS_SNAPSHOT, snapshotItem)
            putParcelable(ARGS_ASSET, asset)
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val binding by viewBinding(FragmentTransactionBinding::bind)

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelableCompat(ARGS_SNAPSHOT, SnapshotItem::class.java) }
    private val asset: TokenItem? by lazy { requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightIb.setOnClickListener {
            lifecycleScope.launch {
                val userTeamMixin = walletViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                if (userTeamMixin == null) {
                    toast(R.string.Data_error)
                } else {
                    ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                }
            }
        }
        binding.root.isClickable = true
        initView(this, binding, lifecycleScope, walletViewModel, assetId, snapshotId, asset, snapshot)
    }
}
