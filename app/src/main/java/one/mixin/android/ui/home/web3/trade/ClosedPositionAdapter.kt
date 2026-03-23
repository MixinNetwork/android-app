package one.mixin.android.ui.home.web3.trade

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
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.math.RoundingMode

class ClosedPositionAdapter(
    private val isQuoteColorReversed: Boolean = false,
    private val onItemClick: ((PerpsPositionHistoryItem) -> Unit)? = null
) : SafePagedListAdapter<PerpsPositionHistoryItem, ClosedPositionAdapter.ViewHolder>(DiffCallback()) {

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
                titleTv.text = context.getString(R.string.Perpetual_Side_Symbol_Title, sideText, displaySymbol)

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

                val quantity = position.quantity.toBigDecimalOrNull()
                quantityTv.text = "${formatDisplayDecimal(quantity?.abs())} ${position.tokenSymbol ?: ""}"

                val pnl = position.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val pnlPercent = calculateClosedPercent(
                    entryPrice = position.entryPrice,
                    closePrice = position.closePrice,
                    side = position.side,
                    leverage = position.leverage,
                )
                rightTopValueTv.text = formatSignedUsd(pnl)
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
                rightBottomValueTv.isVisible = true
                rightBottomValueTv.text = formatSignedPercent(pnlPercent)
                rightBottomValueTv.setTextColor(rightTopValueTv.currentTextColor)
            }
        }

        private fun formatSignedUsd(amount: BigDecimal): String {
            val fiatRate = BigDecimal(Fiats.getRate())
            val fiatSymbol = Fiats.getSymbol()
            val fiatAmount = amount.abs().multiply(fiatRate).priceFormat()
            return when {
                amount > BigDecimal.ZERO -> "+$fiatSymbol$fiatAmount"
                amount < BigDecimal.ZERO -> "-$fiatSymbol$fiatAmount"
                else -> "$fiatSymbol${BigDecimal.ZERO.multiply(fiatRate).priceFormat()}"
            }
        }

        private fun calculateClosedPercent(
            entryPrice: String?,
            closePrice: String?,
            side: String,
            leverage: Int,
        ): BigDecimal {
            val entry = entryPrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
            val close = closePrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
            if (entry <= BigDecimal.ZERO || leverage <= 0) {
                return BigDecimal.ZERO
            }

            val direction = if (side.equals("short", ignoreCase = true)) BigDecimal(-1) else BigDecimal.ONE
            return close
                .subtract(entry)
                .divide(entry, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal(leverage))
                .multiply(BigDecimal(100))
                .multiply(direction)
        }

        private fun formatDisplayDecimal(value: BigDecimal?): String {
            val safeValue = value ?: BigDecimal.ZERO
            val absValue = safeValue.abs()
            if (absValue > BigDecimal.ZERO && absValue < BigDecimal("0.01")) {
                return "<0.01"
            }
            return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString()
        }

        private fun formatSignedPercent(value: BigDecimal): String {
            val sign = when {
                value > BigDecimal.ZERO -> "+"
                value < BigDecimal.ZERO -> "-"
                else -> ""
            }
            return "$sign${formatDisplayDecimal(value.abs())}%"
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
