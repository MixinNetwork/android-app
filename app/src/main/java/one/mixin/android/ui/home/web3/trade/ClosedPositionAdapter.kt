package one.mixin.android.ui.home.web3.trade

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PositionHistoryView
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import java.math.BigDecimal

class ClosedPositionAdapter(
    private val onItemClick: ((PositionHistoryView) -> Unit)? = null
) : ListAdapter<PositionHistoryView, ClosedPositionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemClosedPositionListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemClosedPositionListBinding,
        private val onItemClick: ((PositionHistoryView) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: PositionHistoryView) {
            binding.apply {
                val context = binding.root.context
                
                root.setOnClickListener {
                    onItemClick?.invoke(position)
                }
                
                iconIv.loadImage(position.iconUrl, R.drawable.ic_avatar_place_holder)
                
                val displaySymbol = position.displaySymbol ?: position.tokenSymbol ?: "Unknown"
                symbolTv.text = displaySymbol
                
                sideTv.text = if (position.side.lowercase() == "long") {
                    context.getString(R.string.Long)
                } else {
                    context.getString(R.string.Short)
                }
                sideTv.setTextColor(
                    if (position.side.lowercase() == "long") {
                        context.getColor(R.color.wallet_green)
                    } else {
                        context.getColor(R.color.wallet_red)
                    }
                )

                leverageTv.text = "${position.leverage}x"

                val quantity = position.quantity.toBigDecimalOrNull()
                val quantityStr = if (quantity != null) {
                    String.format("%.4f", quantity)
                } else {
                    position.quantity
                }
                quantityTv.text = "$quantityStr ${position.tokenSymbol ?: ""}"

                val pnl = position.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
                pnlTv.text = String.format("$%.2f", pnl.abs())
                pnlTv.setTextColor(
                    if (pnl >= BigDecimal.ZERO) {
                        context.getColor(R.color.wallet_green)
                    } else {
                        context.getColor(R.color.wallet_red)
                    }
                )

                val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val closePrice = position.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val priceChange = if (entryPrice > BigDecimal.ZERO) {
                    ((closePrice - entryPrice) / entryPrice * BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }
                
                priceChangeTv.text = String.format("%s%.1f%%", if (priceChange >= BigDecimal.ZERO) "+" else "", priceChange)
                priceChangeTv.setTextColor(
                    if (priceChange >= BigDecimal.ZERO) {
                        context.getColor(R.color.wallet_green)
                    } else {
                        context.getColor(R.color.wallet_red)
                    }
                )
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PositionHistoryView>() {
        override fun areItemsTheSame(
            oldItem: PositionHistoryView,
            newItem: PositionHistoryView
        ): Boolean {
            return oldItem.historyId == newItem.historyId
        }

        override fun areContentsTheSame(
            oldItem: PositionHistoryView,
            newItem: PositionHistoryView
        ): Boolean {
            return oldItem == newItem
        }
    }
}
