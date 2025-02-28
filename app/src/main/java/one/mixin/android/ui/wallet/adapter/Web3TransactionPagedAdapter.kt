package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.databinding.ItemWeb3TransactionBinding
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.web3.details.Web3TransactionHolder
import kotlin.math.abs

class Web3TransactionPagedAdapter :
    SafePagedListAdapter<Web3Transaction, Web3TransactionHolder>(Web3Transaction.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

    override fun getHeaderId(pos: Int): Long {
        val snapshot = getItem(pos)
        return if (snapshot == null) {
            -1
        } else {
            abs(snapshot.createdAt.hashForDate())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(
        vh: SnapshotHeaderViewHolder,
        pos: Int,
    ) {
        getItem(pos)?.let {
            vh.bind(it.createdAt)
        }
    }

    override fun onBindViewHolder(
        holder: Web3TransactionHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            // todo listener
            holder.bind(it)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Web3TransactionHolder {
        return Web3TransactionHolder(
            ItemWeb3TransactionBinding.inflate(LayoutInflater.from(parent.context))
        )
    }
}
