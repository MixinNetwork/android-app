package one.mixin.android.ui.home.web3.trade.perps

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
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.ItemClosedPositionListBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class OpenPositionAdapter(
    private val isQuoteColorReversed: Boolean = false,
    private val onItemClick: ((PerpsPositionItem) -> Unit)? = null
) : PagingDataAdapter<PerpsPositionItem, OpenPositionAdapter.ViewHolder>(DiffCallback()) {

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = getItem(position) ?: return
            holder.bindContent(item)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class ViewHolder(
        private val binding: ItemClosedPositionListBinding,
        private val isQuoteColorReversed: Boolean,
        private val onItemClick: ((PerpsPositionItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: PerpsPositionItem, positionInList: Int, listSize: Int) {
            binding.root.setBackgroundResource(
                when {
                    listSize <= 1 -> R.drawable.bg_card
                    positionInList == 0 -> R.drawable.bg_card_top
                    positionInList == listSize - 1 -> R.drawable.bg_card_bottom
                    else -> R.drawable.bg_card_middle
                }
            )
            val dp8 = (8 * binding.root.context.resources.displayMetrics.density).toInt()
            val dp16 = (16 * binding.root.context.resources.displayMetrics.density).toInt()
            val topPadding = if (positionInList == 0) dp16 else dp8
            val bottomPadding = if (positionInList == listSize - 1) dp16 else dp8
            binding.root.setPadding(dp16, topPadding, dp16, bottomPadding)
            binding.root.setOnClickListener { onItemClick?.invoke(position) }
            binding.iconIv.loadImage(position.iconUrl, R.drawable.ic_avatar_place_holder)
            bindContent(position)
        }

        fun bindContent(position: PerpsPositionItem) {
            binding.apply {
                val context = binding.root.context
                val isLong = position.side.equals("long", ignoreCase = true)
                val sideText = if (isLong) context.getString(R.string.Long) else context.getString(R.string.Short)
                val sideColor = context.getColor(
                    if (isLong) {
                        if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green
                    } else {
                        if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red
                    }
                )
                val isOpening = position.state.equals("opening", ignoreCase = true)
                val displaySymbol = position.tokenSymbol ?: context.getString(R.string.Unknown)
                titleTv.text = context.getString(R.string.Perpetual_Side_Symbol_Title, sideText, displaySymbol)
                leverageTv.isVisible = true
                leverageTv.text = "${position.leverage}x"
                leverageTv.setTextColor(if (isOpening) resolveAttrColor(root, R.attr.text_assist) else sideColor)
                leverageTv.setBackgroundResource(
                    if (isOpening) {
                        R.drawable.bg_round_window_4dp
                    } else if (isLong) {
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

                if (isOpening) {
                    rightTopValueTv.text = context.getString(R.string.Pending)
                    rightTopValueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    rightTopValueTv.setTextColor(resolveAttrColor(root, R.attr.text_assist))
                    rightBottomValueTv.isVisible = false
                } else {
                    val margin = position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    rightTopValueTv.text = formatUsd(margin)
                    rightTopValueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    rightTopValueTv.setTextColor(resolveAttrColor(root, R.attr.text_primary))

                    rightBottomValueTv.isVisible = true
                    val pnl = (position.unrealizedPnl ?: "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val roe = (position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
                    rightBottomValueTv.text = "${formatSignedUsd(pnl)} (${formatPerpsSignedPercent(roe, withSign = false)})"
                    rightBottomValueTv.setTextColor(
                        when {
                            pnl > BigDecimal.ZERO -> context.getColor(if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green)
                            pnl < BigDecimal.ZERO -> context.getColor(if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red)
                            else -> resolveAttrColor(root, R.attr.text_primary)
                        }
                    )
                }
            }
        }

        private fun formatUsd(amount: BigDecimal): String {
            return formatPerpsUsdDecimal(amount)
        }

        private fun formatSignedUsd(amount: BigDecimal): String {
            return formatPerpsSignedRawUsdDecimal(amount)
        }

        private fun resolveAttrColor(view: View, @AttrRes attr: Int): Int {
            val typedValue = TypedValue()
            view.context.theme.resolveAttribute(attr, typedValue, true)
            return if (typedValue.resourceId != 0) {
                view.context.getColor(typedValue.resourceId)
            } else {
                typedValue.data
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PerpsPositionItem>() {
        override fun areItemsTheSame(oldItem: PerpsPositionItem, newItem: PerpsPositionItem) =
            oldItem.positionId == newItem.positionId

        override fun areContentsTheSame(oldItem: PerpsPositionItem, newItem: PerpsPositionItem) =
            oldItem == newItem

        override fun getChangePayload(oldItem: PerpsPositionItem, newItem: PerpsPositionItem): Any = Unit
    }
}
