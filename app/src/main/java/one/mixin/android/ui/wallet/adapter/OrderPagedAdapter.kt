package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemLimitOrderBinding
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.ui.wallet.holder.OrderHolder
import one.mixin.android.vo.route.OrderItem
import kotlin.math.abs

class OrderPagedAdapter :
    SafePagedListAdapter<OrderItem, OrderHolder>(DIFF),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

    var onItemClick: ((OrderItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHolder {
        val binding = ItemLimitOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderHolder(binding).also { binding.root.tag = it }
    }

    override fun onBindViewHolder(holder: OrderHolder, position: Int) {
        getItem(position)?.let { order ->
            holder.bind(order)
            holder.itemView.setOnClickListener { onItemClick?.invoke(order) }
        }
    }

    override fun getHeaderId(pos: Int): Long {
        val item = getItem(pos)
        return if (item == null) -1 else abs(item.createdAt.hashForDate())
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): SnapshotHeaderViewHolder =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(vh: SnapshotHeaderViewHolder, pos: Int) {
        getItem(pos)?.let { vh.bind(it.createdAt) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderItem>() {
            override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean = oldItem.orderId == newItem.orderId
            override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean = oldItem == newItem
        }
    }
}
