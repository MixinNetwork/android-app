package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_transactions_user.*
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.date
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserSnapshotsJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource
import javax.inject.Inject

class UserTransactionsFragment : BaseFragment() {

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
        TransactionsAdapter(snapshotClick)
    }

    private val userId by lazy {
        arguments!!.getString(ARGS_ID)!!
    }

    @SuppressLint("CheckResult")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recycler_view.addItemDecoration(SpaceItemDecoration())
        title_view.right_animator.visibility = View.GONE
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        jobManager.addJobInBackground(RefreshUserSnapshotsJob(userId))
        walletViewModel.snapshotsByUserId(userId)
            .observe(this, Observer {
                if (recycler_view.adapter == null) {
                    recycler_view.adapter = adapter
                }
                adapter.list = it
                adapter.notifyDataSetChanged()
            })
    }

    private val snapshotClick: (SnapshotItem) -> Unit = { snapshot ->
        walletViewModel.getAssetItem(snapshot.assetId).autoDisposable(scopeProvider).subscribe({
            it.let {
                val fragment = TransactionFragment.newInstance(snapshot, it)
                activity?.addFragment( fragment, TransactionFragment.TAG)
            }
        }, {})
    }

    class TransactionsAdapter(val action: (SnapshotItem) -> Unit) : RecyclerView.Adapter<TransactionHolder>() {
        var list: List<SnapshotItem>? = null

        override fun onCreateViewHolder(vg: ViewGroup, position: Int): TransactionHolder {
            return TransactionHolder(LayoutInflater.from(vg.context).inflate(R.layout.item_wallet_transactions, vg, false))
        }

        override fun getItemCount(): Int = notNullElse(list, { it.size }, 0)

        override fun onBindViewHolder(holder: TransactionHolder, position: Int) {
            list?.let {
                holder.bind(it[position], action)
            }
        }
    }

    class TransactionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(snapshot: SnapshotItem, action: (SnapshotItem) -> Unit) {
            val isPositive = snapshot.amount.toFloat() > 0
            itemView.date.text = snapshot.createdAt.date()
            when {
                snapshot.type == SnapshotType.deposit.name -> {
                    snapshot.transactionHash?.let {
                        if (it.length > 10) {
                            val start = it.substring(0, 6)
                            val end = it.substring(it.length - 4, it.length)
                            itemView.name.text = itemView.context.getString(R.string.wallet_transactions_hash, start, end)
                        } else {
                            itemView.name.text = it
                        }
                    }
                }
                snapshot.type == SnapshotType.transfer.name -> itemView.name.text = if (isPositive) {
                    itemView.context.getString(R.string.transfer_from, snapshot.opponentFullName)
                } else {
                    itemView.context.getString(R.string.transfer_to, snapshot.opponentFullName)
                }
                else -> itemView.name.text = snapshot.receiver!!.formatPublicKey()
            }
            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}"
            else "${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}"
            itemView.value.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
            itemView.setOnClickListener { action(snapshot) }
        }
    }
}
