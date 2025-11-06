package one.mixin.android.ui.wallet.holder

import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemLimitOrderBinding
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.route.OrderItem
import one.mixin.android.ui.home.web3.trade.LimitOrderState

class OrderHolder(private val binding: ItemLimitOrderBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(order: OrderItem) {
        binding.iconA.loadImage(order.assetIconUrl, R.drawable.ic_avatar_place_holder)
        binding.iconB.loadImage(order.receiveAssetIconUrl, R.drawable.ic_avatar_place_holder)
        val left = order.assetSymbol ?: ""
        val right = order.receiveAssetSymbol ?: ""

        // Line 1: symbol -> symbol | time
        binding.line1Left.text = "$left → $right"
        binding.line1Right.text = order.createdAt.fullDate()

        // Line 2: -xx symbol (red) | Limit or Swap
        val payAmountText = order.payAmount.ifEmpty { "0" }.numberFormat()
        binding.line2Left.setTextColor(itemView.context.getColor(R.color.wallet_pink))
        binding.line2Left.text = "-${payAmountText} ${left}"
        binding.line2Right.text = when (order.type.lowercase()) {
            "swap" -> itemView.context.getString(R.string.order_type_swap)
            "limit" -> itemView.context.getString(R.string.order_type_limit)
            else -> order.type
        }

        // Line 3: +xx symbol (color by status) | state with color
        val receiveAmountText = (order.receiveAmount ?: "0").ifEmpty { "0" }.numberFormat()
        binding.line3Left.text = "+${receiveAmountText} ${right}"
        val stateLower = order.state.lowercase()
        val leftColor = when (stateLower) {
            "settled" -> itemView.context.getColor(R.color.wallet_green)
            "failed", "cancelled", "canceled", "expired" -> itemView.context.getColor(R.color.wallet_pink)
            else -> resolveAttrColor(R.attr.text_primary)
        }
        binding.line3Left.setTextColor(leftColor)

        binding.line3Right.text = LimitOrderState.from(order.state).format(itemView.context)
        binding.line3Right.setTextColor(getStatusColor(order.state))
    }

    private fun resolveAttrColor(attr: Int): Int {
        val typedArray = itemView.context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    private fun getStatusColor(state: String?): Int {
        state ?: return resolveAttrColor(R.attr.text_assist)
        return when (state.lowercase()) {
            "settled" -> itemView.context.getColor(R.color.wallet_green)
            "failed", "cancelled", "canceled", "expired" -> itemView.context.getColor(R.color.wallet_pink)
            "created", "pricing", "quoting" -> resolveAttrColor(R.attr.text_assist)
            else -> resolveAttrColor(R.attr.text_assist)
        }
    }
}
