package one.mixin.android.ui.home.web3.trade

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import java.math.BigDecimal

class ClosedPositionAdapter(
    private val onItemClick: ((PerpsPositionHistoryItem) -> Unit)? = null
) : SafePagedListAdapter<PerpsPositionHistoryItem, ClosedPositionAdapter.ViewHolder>(DiffCallback()) {

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
        val item = getItem(position) ?: return
        holder.bind(position = item, positionInList = position, listSize = itemCount)
    }

    class ViewHolder(
        private val binding: ItemClosedPositionListBinding,
        private val onItemClick: ((PerpsPositionHistoryItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: PerpsPositionHistoryItem, positionInList: Int, listSize: Int) {
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

                val isLong = position.side.equals("long", ignoreCase = true)
                val sideText = if (isLong) {
                    context.getString(R.string.Long)
                } else {
                    context.getString(R.string.Short)
                }
                val sideColor = if (isLong) {
                    context.getColor(R.color.wallet_green)
                } else {
                    context.getColor(R.color.wallet_red)
                }
                val displaySymbol = position.tokenSymbol ?: context.getString(R.string.Unknown)
                titleTv.text = context.getString(R.string.Perpetual_Side_Symbol_Title, sideText, displaySymbol)

                leverageTv.isVisible = false

                val quantityStr = position.quantity
                quantityTv.text = "$quantityStr ${position.tokenSymbol ?: ""}"

                val pnl = position.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
                rightTopValueTv.text = formatSignedUsd(context, pnl)
                rightTopValueTv.setTextColor(
                    when {
                        pnl > BigDecimal.ZERO -> {
                            context.getColor(R.color.wallet_green)
                        }

                        pnl < BigDecimal.ZERO -> {
                            context.getColor(R.color.wallet_red)
                        }

                        else -> {
                            resolveAttrColor(root, R.attr.text_primary)
                        }
                    }
                )
                rightBottomValueTv.isVisible = false
            }
        }

        private fun formatSignedUsd(context: Context, amount: BigDecimal): String {
            return when {
                amount > BigDecimal.ZERO -> context.getString(
                    R.string.Perpetual_Usd_Amount_Signed,
                    "+",
                    amount.abs().toDouble()
                )
                amount < BigDecimal.ZERO -> context.getString(
                    R.string.Perpetual_Usd_Amount_Signed,
                    "-",
                    amount.abs().toDouble()
                )
                else -> context.getString(R.string.Perpetual_Usd_Amount, 0.0)
            }
        }

        private fun resolveAttrColor(view: View, @AttrRes attr: Int): Int {
            val typedValue = android.util.TypedValue()
            view.context.theme.resolveAttribute(attr, typedValue, true)
            return if (typedValue.resourceId != 0) {
                view.context.getColor(typedValue.resourceId)
            } else {
                typedValue.data
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PerpsPositionHistoryItem>() {
        override fun areItemsTheSame(
            oldItem: PerpsPositionHistoryItem,
            newItem: PerpsPositionHistoryItem
        ): Boolean {
            return oldItem.historyId == newItem.historyId
        }

        override fun areContentsTheSame(
            oldItem: PerpsPositionHistoryItem,
            newItem: PerpsPositionHistoryItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
