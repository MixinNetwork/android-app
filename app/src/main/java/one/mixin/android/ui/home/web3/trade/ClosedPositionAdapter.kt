package one.mixin.android.ui.home.web3.trade

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.home.web3.trade.perps.calculateClosedRoe
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsSignedFiatDecimal
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsSignedPercent
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class ClosedPositionAdapter(
    private val isQuoteColorReversed: Boolean = false,
    private val onItemClick: ((PerpsPositionHistoryItem) -> Unit)? = null
) : PagingDataAdapter<PerpsPositionHistoryItem, ClosedPositionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemClosedPositionListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            isQuoteColorReversed,
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(position = item, positionInList = position, listSize = itemCount)
    }

    class ViewHolder(
        private val binding: ItemClosedPositionListBinding,
        private val isQuoteColorReversed: Boolean,
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
                val sideColor = context.getColor(
                    if (isLong) {
                        if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green
                    } else {
                        if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red
                    }
                )
                val displaySymbol = position.tokenSymbol ?: context.getString(R.string.Unknown)
                val title = context.getString(R.string.Perpetual_Side_Symbol_Title, sideText, displaySymbol)
                titleTv.text = title

                leverageTv.isVisible = true
                leverageTv.text = "${position.leverage}x"
                leverageTv.setTextColor(sideColor)
                leverageTv.setBackgroundResource(
                    if (isLong) {
                        if (isQuoteColorReversed) R.drawable.bg_perps_leverage_short else R.drawable.bg_perps_leverage_long
                    } else {
                        if (isQuoteColorReversed) R.drawable.bg_perps_leverage_long else R.drawable.bg_perps_leverage_short
                    }
                )

                val quantity = position.quantity
                    .toBigDecimalOrNull()
                    ?.abs()
                    ?.stripTrailingZeros()
                    ?.toPlainString()
                    ?: position.quantity.removePrefix("-")
                quantityTv.text = "$quantity ${position.tokenSymbol ?: ""}"

                val pnl = position.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val pnlPercent = calculateClosedRoe(
                    entryPrice = position.entryPrice,
                    closePrice = position.closePrice,
                    side = position.side,
                    leverage = position.leverage,
                )
                rightTopValueTv.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    14f
                )
                rightTopValueTv.text = "${formatSignedUsd(pnl)} (${formatPerpsSignedPercent(pnlPercent)})"
                rightTopValueTv.setTextColor(
                    when {
                        pnl > BigDecimal.ZERO -> {
                            context.getColor(if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green)
                        }

                        pnl < BigDecimal.ZERO -> {
                            context.getColor(if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red)
                        }

                        else -> {
                            resolveAttrColor(root, R.attr.text_primary)
                        }
                    }
                )
                rightBottomValueTv.isVisible = false
            }
        }

        private fun formatSignedUsd(amount: BigDecimal): String {
            val fiatRate = BigDecimal(Fiats.getRate())
            val fiatSymbol = Fiats.getSymbol()
            return formatPerpsSignedFiatDecimal(amount.multiply(fiatRate), fiatSymbol)
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
