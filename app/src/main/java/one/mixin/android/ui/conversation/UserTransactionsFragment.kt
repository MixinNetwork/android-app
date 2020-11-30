package one.mixin.android.ui.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.PagedList
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransactionsUserBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.wallet.BaseTransactionsFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SnapshotItem

@AndroidEntryPoint
class UserTransactionsFragment : BaseTransactionsFragment<PagedList<SnapshotItem>>(), OnSnapshotListener {

    companion object {
        const val TAG = "UserTransactionsFragment"

        fun newInstance(userId: String) = UserTransactionsFragment().withArgs {
            putString(ARGS_USER_ID, userId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_transactions_user, container, false).apply {
            isClickable = true
        }

    private val binding by viewBinding(FragmentTransactionsUserBinding::bind)

    private val adapter = SnapshotPagedAdapter()

    private val userId by lazy {
        requireArguments().getString(ARGS_USER_ID)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transactionsRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.titleView.rightAnimator.visibility = View.GONE
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        adapter.listener = this
        binding.transactionsRv.adapter = adapter
        dataObserver = Observer {
            if (it != null && it.isNotEmpty()) {
                showEmpty(false)
            } else {
                showEmpty(true)
            }
            adapter.submitList(it)

            if (!refreshedSnapshots) {
                walletViewModel.refreshSnapshots(opponent = userId)
                refreshedSnapshots = true
            }
        }
        bindLiveData(walletViewModel.snapshotsByUserId(userId, initialLoadKey))
    }

    override fun <T> onNormalItemClick(item: T) {
        val snapshot = item as SnapshotItem
        lifecycleScope.launch {
            val assetItem = walletViewModel.simpleAssetItem(snapshot.assetId)
            assetItem.let {
                try {
                    view?.findNavController()
                        ?.navigate(
                            R.id.action_user_transactions_to_transaction,
                            Bundle().apply {
                                putParcelable(TransactionFragment.ARGS_SNAPSHOT, snapshot)
                                putParcelable(TransactionsFragment.ARGS_ASSET, it)
                            }
                        )
                } catch (e: IllegalStateException) {
                    val fragment = TransactionFragment.newInstance(snapshot, it)
                    activity?.addFragment(
                        this@UserTransactionsFragment,
                        fragment,
                        TransactionFragment.TAG
                    )
                }
            }
        }
    }

    override fun onUserClick(userId: String) {
        // Do nothing, avoid recursively calling this page.
    }

    private fun showEmpty(show: Boolean) {
        if (show) {
            if (binding.empty.emptyRl.visibility == View.GONE) {
                binding.empty.emptyRl.visibility = View.VISIBLE
            }
            if (binding.transactionsRv.visibility == View.VISIBLE) {
                binding.transactionsRv.visibility = View.GONE
            }
        } else {
            if (binding.empty.emptyRl.visibility == View.VISIBLE) {
                binding.empty.emptyRl.visibility = View.GONE
            }
            if (binding.transactionsRv.visibility == View.GONE) {
                binding.transactionsRv.visibility = View.VISIBLE
            }
        }
    }

    override fun refreshSnapshots() {
        walletViewModel.refreshSnapshots(offset = refreshOffset, opponent = userId)
    }

    override fun onApplyClick() {
        // Left empty
    }
}
