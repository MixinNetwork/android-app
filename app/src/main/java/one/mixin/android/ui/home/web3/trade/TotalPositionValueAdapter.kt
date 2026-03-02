package one.mixin.android.ui.home.web3.trade

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import java.math.BigDecimal

class TotalPositionValueAdapter : RecyclerView.Adapter<TotalPositionValueAdapter.ViewHolder>() {
    private var totalValue: BigDecimal = BigDecimal.ZERO

    fun submitTotal(value: BigDecimal) {
        totalValue = value
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_position_value, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(totalValue)
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueTv: TextView = itemView.findViewById(R.id.value_tv)

        fun bind(total: BigDecimal) {
            val context = itemView.context
            valueTv.text = String.format("$%.2f", total)
            valueTv.setTextColor(
                when {
                    total > BigDecimal.ZERO -> context.getColor(R.color.wallet_green)
                    total < BigDecimal.ZERO -> context.getColor(R.color.wallet_red)
                    else -> resolveAttrColor(itemView, R.attr.text_primary)
                }
            )
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
}
