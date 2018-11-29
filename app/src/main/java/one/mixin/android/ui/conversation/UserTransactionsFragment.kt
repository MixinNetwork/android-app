package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_transactions_user.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotListAdapter
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.worker.RefreshUserSnapshotsWorker
import javax.inject.Inject

class UserTransactionsFragment : BaseFragment(), OnSnapshotListener {

    companion object {
        const val TAG = "UserTransactionsFragment"
        private const val ARGS_ID = "args_id"

        fun newInstance(userId: String): UserTransactionsFragment {
            val f = UserTransactionsFragment()
            val b = Bundle()
            b.putString(ARGS_ID, userId)
            f.arguments = b
            return f
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transactions_user, container, false).apply {
            isClickable = true
        }

    private val adapter by lazy {
        SnapshotListAdapter()
    }

    private val userId by lazy {
        arguments!!.getString(ARGS_ID)!!
    }

    @SuppressLint("CheckResult")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        title_view.right_animator.visibility = View.GONE
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserSnapshotsWorker>(
            workDataOf(RefreshUserSnapshotsWorker.USER_ID to userId))
        adapter.listener = this
        recycler_view.adapter = adapter
        walletViewModel.snapshotsByUserId(userId).observe(this, Observer {
            adapter.submitList(it)
        })
    }

    override fun <T> onNormalItemClick(item: T) {
        val snapshot = item as SnapshotItem
        walletViewModel.getAssetItem(snapshot.assetId).autoDisposable(scopeProvider).subscribe({ assetItem ->
            assetItem.let {
                val fragment = TransactionFragment.newInstance(snapshot, it)
                activity?.addFragment(this@UserTransactionsFragment, fragment, TransactionFragment.TAG)
            }
        }, {})
    }

    override fun onUserClick(userId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            walletViewModel.getUser(userId)?.let {
                UserBottomSheetDialogFragment.newInstance(it).show(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
            }
        }
    }
}
