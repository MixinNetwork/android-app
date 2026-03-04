package one.mixin.android.ui.home.web3.trade

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class OpenPositionAdapter(
    private val isQuoteColorReversed: Boolean = false,
    private val onItemClick: ((PerpsPositionItem) -> Unit)? = null
) : SafePagedListAdapter<PerpsPositionItem, OpenPositionAdapter.ViewHolder>(DiffCallback()) {

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
                leverageTv.text = context.getString(R.string.Perpetual_Leverage_Format, position.leverage)
                leverageTv.setTextColor(sideColor)
                leverageTv.setBackgroundResource(
                    if (isLong) {
                        if (isQuoteColorReversed) R.drawable.bg_perps_leverage_short else R.drawable.bg_perps_leverage_long
                    } else {
                        if (isQuoteColorReversed) R.drawable.bg_perps_leverage_long else R.drawable.bg_perps_leverage_short
                    }
                )

                val quantity = position.quantity.toBigDecimalOrNull()
                val quantityStr = position.quantity
                quantityTv.text = "$quantityStr ${position.tokenSymbol ?: ""}"

                val markPrice = (position.markPrice ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                val positionValue = (quantity ?: BigDecimal.ZERO).abs().multiply(markPrice)
                rightTopValueTv.text = formatUsd(positionValue)
                rightTopValueTv.setTextColor(resolveAttrColor(root, R.attr.text_primary))

                rightBottomValueTv.isVisible = true
                val pnl = (position.unrealizedPnl ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                rightBottomValueTv.text = formatSignedUsd(pnl)
                rightBottomValueTv.setTextColor(
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
            }
        }

        private fun formatUsd(amount: BigDecimal): String {
            val fiatRate = BigDecimal(Fiats.getRate())
            val fiatSymbol = Fiats.getSymbol()
            return "$fiatSymbol${amount.multiply(fiatRate).priceFormat()}"
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
