package one.mixin.android.ui.wallet

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_all_transactions.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancelChildren
import kotlinx.coroutines.experimental.launch
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.SnapshotAdapter
import one.mixin.android.vo.SnapshotItem
import org.jetbrains.anko.doAsync
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class AllTransactionsFragment : BaseFragment(), SnapshotAdapter.TransactionsListener {
    companion object {
        const val TAG = "AllTransactionsFragment"

        fun newInstance() = AllTransactionsFragment()
    }

    private lateinit var snapshotContext: CoroutineContext

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    @Inject
    lateinit var jobManager: MixinJobManager

    private val adapter = SnapshotAdapter().apply { setListener(this@AllTransactionsFragment) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_all_transactions, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        snapshotContext = Job()
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        transaction_rv.adapter = adapter
        transaction_rv.addItemDecoration(SpaceItemDecoration())
        walletViewModel.allSnapshots().observe(this, Observer {
            it?.let {
                adapter.submitList(it)

                doAsync {
                    for (s in it) {
                        s?.opponentId?.let {
                            val u = walletViewModel.getUserById(it)
                            if (u == null) {
                                jobManager.addJobInBackground(RefreshUserJob(arrayListOf(it)))
                            }
                        }
                    }
                }
            }
        })
        transaction_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if ((recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() == adapter.itemCount - 1) {
                    getMore(adapter.getLastTime())
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        jobManager.addJobInBackground(RefreshSnapshotsJob())
    }

    override fun onDestroy() {
        super.onDestroy()
        snapshotContext.cancelChildren()
    }

    private var hasMore = true
    private var job: Job? = null

    @Synchronized
    private fun getMore(offset: String) {
        if (hasMore && (job == null || job?.isActive == false)) {
            job = GlobalScope.launch(snapshotContext) {
                val response = walletViewModel.getSnapshotsByOffset(offset).await()
                hasMore = if (response.isSuccess && response.data != null && response.data?.isEmpty() != true) {
                    walletViewModel.insertSnapshots(response.data!!)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onItemClick(snapshot: SnapshotItem) {
        doAsync {
            val a = walletViewModel.simpleAssetItem(snapshot.assetId)
            a?.let {
                val fragment = TransactionFragment.newInstance(snapshot, it)
                activity?.addFragment(this@AllTransactionsFragment, fragment, TransactionFragment.TAG)
            }
        }
    }
}