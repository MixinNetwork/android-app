package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentAllTransactionsBinding
import one.mixin.android.extension.navigate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment.Companion.ARGS_SNAPSHOT
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.toSnapshot

@AndroidEntryPoint
class AllTransactionsFragment : BaseTransactionsFragment<PagedList<SnapshotItem>>(R.layout.fragment_all_transactions), OnSnapshotListener {
    companion object {
        const val TAG = "AllTransactionsFragment"
    }

    private val binding by viewBinding(FragmentAllTransactionsBinding::bind)

    private val adapter = SnapshotPagedAdapter()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        adapter.listener = this
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightAnimator.setOnClickListener { showFiltersSheet() }
            }
            transactionsRv.itemAnimator = null
            transactionsRv.adapter = adapter
            val layoutManager = LinearLayoutManager(requireContext())
            transactionsRv.layoutManager = layoutManager
            transactionsRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
            adapter.registerAdapterDataObserver(
                object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(
                        positionStart: Int,
                        itemCount: Int,
                    ) {
                        val firstPos = layoutManager.findFirstVisibleItemPosition()
                        if (firstPos == 0) {
                            layoutManager.scrollToPosition(0)
                        }
                    }
                },
            )
        }
        dataObserver =
            Observer { pagedList ->
                if (pagedList.isNotEmpty()) {
                    showEmpty(false)
                    val opponentIds =
                        pagedList.filter {
                            !it?.opponentId.isNullOrBlank()
                        }.map {
                            it.opponentId
                        }
                    walletViewModel.checkAndRefreshUsers(opponentIds)
                } else {
                    showEmpty(true)
                }
                adapter.submitList(pagedList)
            }
        bindLiveData()

        refreshAllPendingDeposit()
    }

    override fun <T> onNormalItemClick(item: T) {
        lifecycleScope.launch {
            val snapshot = item as SnapshotItem
            val a =
                withContext(Dispatchers.IO) {
                    walletViewModel.simpleAssetItem(snapshot.assetId)
                }
            a?.let {
                if (viewDestroyed()) return@launch

                view?.navigate(
                    R.id.action_all_transactions_fragment_to_transaction_fragment,
                    Bundle().apply {
                        putParcelable(ARGS_SNAPSHOT, snapshot)
                        putParcelable(ARGS_ASSET, it)
                    },
                )
            }
        }
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch {
            val user =
                withContext(Dispatchers.IO) {
                    walletViewModel.getUser(userId)
                } ?: return@launch
            if (user.notMessengerUser()) {
                NonMessengerUserBottomSheetDialogFragment.newInstance(user)
                    .showNow(parentFragmentManager, NonMessengerUserBottomSheetDialogFragment.TAG)
            } else {
                val f = UserBottomSheetDialogFragment.newInstance(user)
                f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onApplyClick() {
        initialLoadKey = null
        bindLiveData()
        filtersSheet.dismiss()
    }

    private fun refreshAllPendingDeposit() =
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = { walletViewModel.allPendingDeposit() },
                successBlock = {
                    walletViewModel.clearAllPendingDeposits()

                    val pendingDeposits = it.data
                    if (pendingDeposits.isNullOrEmpty()) {
                        return@handleMixinResponse
                    }
                    val destinationTags = walletViewModel.findDepositEntryDestinations()
                    pendingDeposits
                        .filter { pd ->
                            destinationTags.any { dt ->
                                dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                            }
                        }
                        .chunked(100) { chunk ->
                            lifecycleScope.launch {
                                chunk.map { pd ->
                                    pd.toSnapshot()
                                }.let { list ->
                                    walletViewModel.insertPendingDeposit(list)
                                }
                            }
                        }
                },
            )
        }

    private fun bindLiveData() {
        val orderByAmount = currentOrder == R.id.sort_amount
        when (currentType) {
            R.id.filters_radio_all -> {
                bindLiveData(walletViewModel.allSnapshots(initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
            }
            R.id.filters_radio_transfer -> {
                bindLiveData(
                    walletViewModel.allSnapshots(
                        SafeSnapshotType.snapshot.name,
                        SafeSnapshotType.pending.name,
                        initialLoadKey = initialLoadKey,
                        orderByAmount = orderByAmount,
                    ),
                )
            }
            R.id.filters_radio_deposit -> {
                bindLiveData(walletViewModel.allSnapshots(SafeSnapshotType.deposit.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
            }
            R.id.filters_radio_withdrawal -> {
                bindLiveData(walletViewModel.allSnapshots(SafeSnapshotType.withdrawal.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
            }
        }
    }

    private fun showEmpty(show: Boolean) {
        binding.apply {
            if (show) {
                if (empty.root.visibility == GONE) {
                    empty.root.visibility = VISIBLE
                }
                if (transactionsRv.visibility == VISIBLE) {
                    transactionsRv.visibility = GONE
                }
            } else {
                if (empty.root.visibility == VISIBLE) {
                    empty.root.visibility = GONE
                }
                if (transactionsRv.visibility == GONE) {
                    transactionsRv.visibility = VISIBLE
                }
            }
        }
    }
}
