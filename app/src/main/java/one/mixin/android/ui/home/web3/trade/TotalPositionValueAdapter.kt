package one.mixin.android.ui.home.web3.trade

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import java.math.BigDecimal

class TotalPositionValueAdapter : RecyclerView.Adapter<TotalPositionValueAdapter.ViewHolder>() {
    private var totalValue: BigDecimal = BigDecimal.ZERO
    private var subValue: BigDecimal = BigDecimal.ZERO
    private var subPercent: BigDecimal = BigDecimal.ZERO
    @StringRes
    private var titleResId: Int = R.string.Total_Position_Value

    fun submitTotal(value: BigDecimal) {
        totalValue = value
        notifyItemChanged(0)
    }

    fun submitSubtitle(value: BigDecimal, percent: BigDecimal) {
        subValue = value
        subPercent = percent
        notifyItemChanged(0)
    }

    fun submitTitle(@StringRes titleResId: Int) {
        this.titleResId = titleResId
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_position_value, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(totalValue, subValue, subPercent, titleResId)
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        private val valueTv: TextView = itemView.findViewById(R.id.value_tv)
        private val subtitleTv: TextView = itemView.findViewById(R.id.subtitle_tv)

        fun bind(
            total: BigDecimal,
            subtitleValue: BigDecimal,
            subtitlePercent: BigDecimal,
            @StringRes titleResId: Int
        ) {
            val context = itemView.context
            titleTv.text = context.getString(titleResId)
            valueTv.text = context.getString(R.string.Perpetual_Usd_Amount, total.toDouble())
            valueTv.setTextColor(resolveAttrColor(itemView, R.attr.text_primary))
            subtitleTv.text = context.getString(
                R.string.Perpetual_Amount_Percent_Format,
                formatSignedUsd(context, subtitleValue),
                subtitlePercent.toDouble()
            )
            subtitleTv.setTextColor(resolveAttrColor(itemView, R.attr.text_assist))
        }

        private fun formatSignedUsd(context: android.content.Context, amount: BigDecimal): String {
            val sign = when {
                amount < BigDecimal.ZERO -> "-"
                else -> ""
            }
            return context.getString(R.string.Perpetual_Usd_Amount_Signed, sign, amount.abs().toDouble())
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
