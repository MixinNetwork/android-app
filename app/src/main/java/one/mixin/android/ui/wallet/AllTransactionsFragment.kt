package one.mixin.android.ui.wallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_all_transactions.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.TransactionsAdapter
import one.mixin.android.vo.SnapshotItem
import org.jetbrains.anko.doAsync
import javax.inject.Inject

class AllTransactionsFragment : BaseFragment(), TransactionsAdapter.TransactionsListener {
    companion object {
        const val TAG = "AllTransactionsFragment"

        fun newInstance() = AllTransactionsFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    @Inject
    lateinit var jobManager: MixinJobManager

    private val adapter = TransactionsAdapter().apply { setListener(this@AllTransactionsFragment) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_all_transactions, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        transaction_rv.adapter = adapter
        transaction_rv.addItemDecoration(SpaceItemDecoration())
        walletViewModel.allSnapshots().observe(this, Observer {
            it?.let {
                adapter.submitList(it)

                doAsync {
                    for (s in it) {
                        s?.counterUserId?.let {
                            val u = walletViewModel.getUserById(it)
                            if (u == null) {
                                jobManager.addJobInBackground(RefreshUserJob(arrayListOf(it)))
                            }
                        }
                    }
                }
            }
        })
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