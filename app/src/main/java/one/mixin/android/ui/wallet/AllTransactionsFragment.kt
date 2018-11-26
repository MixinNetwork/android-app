package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.paging.PagedList
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_all_transactions.*
import kotlinx.android.synthetic.main.fragment_transaction_filters.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment.Companion.ARGS_SNAPSHOT
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotAdapter
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.widget.RadioGroup

class AllTransactionsFragment : BaseTransactionsFragment<PagedList<SnapshotItem>>(), OnSnapshotListener {

    companion object {
        const val TAG = "AllTransactionsFragment"

        fun newInstance() = AllTransactionsFragment()
    }

    private val adapter = SnapshotAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_all_transactions, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            showFiltersSheet()
        }
        transaction_rv.adapter = adapter
        adapter.listener = this
        transaction_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        dataObserver = Observer { pagedList ->
            if (pagedList != null && pagedList.isNotEmpty()) {
                showEmpty(false)
                adapter.submitList(pagedList)
                GlobalScope.launch(Dispatchers.IO) {
                    for (s in pagedList) {
                        s?.opponentId?.let {
                            val u = walletViewModel.getUserById(it)
                            if (u == null) {
                                jobManager.addJobInBackground(RefreshUserJob(arrayListOf(it)))
                            }
                        }
                    }
                }
            } else {
                showEmpty(true)
            }
        }
        bindLiveData(walletViewModel.allSnapshots())
        jobManager.addJobInBackground(RefreshSnapshotsJob())
    }

    override fun <T> onNormalItemClick(item: T) {
        GlobalScope.launch(Dispatchers.IO) {
            val snapshot = item as SnapshotItem
            val a = walletViewModel.simpleAssetItem(snapshot.assetId)
            a?.let {
                if (!isAdded) return@launch

                view!!.findNavController().navigate(R.id.action_all_transactions_fragment_to_transaction_fragment,
                    Bundle().apply {
                        putParcelable(ARGS_SNAPSHOT, snapshot)
                        putParcelable(ARGS_ASSET, it)
                    })
            }
        }
    }

    override fun onUserClick(userId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            walletViewModel.getUser(userId)?.let {
                UserBottomSheetDialogFragment.newInstance(it).show(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun setRadioGroupListener(view: View) {
        view.filters_radio_group.setOnCheckedListener(object : RadioGroup.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentType = id
                when (currentType) {
                    R.id.filters_radio_all -> {
                        bindLiveData(walletViewModel.allSnapshots())
                    }
                    R.id.filters_radio_transfer -> {
                        bindLiveData(walletViewModel.allSnapshots(SnapshotType.transfer.name, SnapshotType.pending.name))
                    }
                    R.id.filters_radio_deposit -> {
                        bindLiveData(walletViewModel.allSnapshots(SnapshotType.deposit.name))
                    }
                    R.id.filters_radio_withdrawal -> {
                        bindLiveData(walletViewModel.allSnapshots(SnapshotType.withdrawal.name))
                    }
                    R.id.filters_radio_fee -> {
                        bindLiveData(walletViewModel.allSnapshots(SnapshotType.fee.name))
                    }
                    R.id.filters_radio_rebate -> {
                        bindLiveData(walletViewModel.allSnapshots(SnapshotType.rebate.name))
                    }
                }
                filtersSheet.dismiss()
            }
        })
    }

    private fun showEmpty(show: Boolean) {
        empty_rl.visibility = if (show) VISIBLE else GONE
        transaction_rv.visibility = if (show) GONE else VISIBLE
    }
}