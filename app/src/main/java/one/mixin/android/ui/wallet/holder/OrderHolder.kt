package one.mixin.android.ui.wallet.holder

import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemLimitOrderBinding
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.route.OrderItem
import one.mixin.android.vo.route.OrderState
import java.math.BigDecimal

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
        val orderState = OrderState.from(order.state)


        // Original logic
        val receiveAmountText = (if (orderState.isPending()) {
            order.expectedReceiveAmount
        } else {
            order.filledReceiveAmount ?: order.receiveAmount ?: order.expectedReceiveAmount
        } ?: "0").ifEmpty { "0" }.numberFormat()
        binding.line3Left.text = "+${receiveAmountText} ${right}"

        val hasReceivedAmount = receiveAmountText != "0"

        // Pending orders without received amount should be gray
        val leftColor = when {
            orderState.isPending() && !hasReceivedAmount -> resolveAttrColor(R.attr.text_assist)
            orderState.isCancel() -> resolveAttrColor(R.attr.text_assist)
            orderState.isDone() -> itemView.context.getColor(R.color.wallet_green)
            else -> itemView.context.getColor(R.color.wallet_pink)
        }
        binding.line3Left.setTextColor(leftColor)

        binding.line3Right.text = orderState.format(itemView.context)
        binding.line3Right.setTextColor(getStatusColor(orderState))
    }

    private fun resolveAttrColor(attr: Int): Int {
        val typedArray = itemView.context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    private fun getStatusColor(state: OrderState): Int {
        return when {
            state.isPending() -> resolveAttrColor(R.attr.text_assist)
            state.isDone() -> itemView.context.getColor(R.color.wallet_green)
            else -> itemView.context.getColor(R.color.wallet_pink)
        }
    }
}
