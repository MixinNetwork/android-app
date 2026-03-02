package one.mixin.android.ui.home.web3.trade

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import java.math.BigDecimal

class OpenPositionAdapter(
    private val onItemClick: ((PerpsPositionItem) -> Unit)? = null
) : ListAdapter<PerpsPositionItem, OpenPositionAdapter.ViewHolder>(DiffCallback()) {

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
        holder.bind(
            position = getItem(position),
            positionInList = position,
            listSize = itemCount
        )
    }

    class ViewHolder(
        private val binding: ItemClosedPositionListBinding,
        private val onItemClick: ((PerpsPositionItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: PerpsPositionItem, positionInList: Int, listSize: Int) {
            binding.apply {
                val context = binding.root.context

                root.setBackgroundResource(
                    when {
                        listSize <= 1 -> R.drawable.bg_card
                        positionInList == 0 -> R.drawable.bg_card_top
                        positionInList == listSize - 1 -> R.drawable.bg_card_bottom
                        else -> R.drawable.bg_card_middle
                    }
                )

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

                val pnl = (position.unrealizedPnl ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                pnlTv.text = String.format("$%.2f", pnl.abs())
                pnlTv.setTextColor(
                    if (pnl >= BigDecimal.ZERO) {
                        context.getColor(R.color.wallet_green)
                    } else {
                        context.getColor(R.color.wallet_red)
                    }
                )

                val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val markPrice = (position.markPrice ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                val priceChange = if (entryPrice > BigDecimal.ZERO) {
                    ((markPrice - entryPrice) / entryPrice * BigDecimal(100))
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

    private class DiffCallback : DiffUtil.ItemCallback<PerpsPositionItem>() {
        override fun areItemsTheSame(
            oldItem: PerpsPositionItem,
            newItem: PerpsPositionItem
        ): Boolean {
            return oldItem.positionId == newItem.positionId
        }

        override fun areContentsTheSame(
            oldItem: PerpsPositionItem,
            newItem: PerpsPositionItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
