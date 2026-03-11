package one.mixin.android.ui.home.web3.trade

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.priceFormat
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class TotalPositionValueAdapter : RecyclerView.Adapter<TotalPositionValueAdapter.ViewHolder>() {
    private var totalValue: BigDecimal = BigDecimal.ZERO
    private var subValue: BigDecimal = BigDecimal.ZERO
    private var subPercent: BigDecimal? = null
    private var isClosed: Boolean = false
    @StringRes
    private var titleResId: Int = R.string.Total_Position_Value

    fun submitTotal(value: BigDecimal) {
        totalValue = value
        notifyItemChanged(0)
    }

    fun submitSubtitle(value: BigDecimal, percent: BigDecimal?) {
        subValue = value
        subPercent = percent
        notifyItemChanged(0)
    }

    fun submitTitle(@StringRes titleResId: Int) {
        this.titleResId = titleResId
        this.isClosed = (titleResId == R.string.Realized_PnL)
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_position_value, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(totalValue, subValue, subPercent, titleResId, isClosed)
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        private val valueTv: TextView = itemView.findViewById(R.id.value_tv)
        private val subtitleTv: TextView = itemView.findViewById(R.id.subtitle_tv)

        fun bind(
            total: BigDecimal,
            subtitleValue: BigDecimal,
            subtitlePercent: BigDecimal?,
            @StringRes titleResId: Int,
            isClosed: Boolean
        ) {
            val context = itemView.context
            val fiatRate = BigDecimal(Fiats.getRate())
            val fiatSymbol = Fiats.getSymbol()
            titleTv.text = context.getString(titleResId)
            valueTv.text = "${
                if (total >= BigDecimal.ZERO) {
                    "+"
                } else {
                    "-"
                }
            }$fiatSymbol${total.abs().multiply(fiatRate).priceFormat()}"
            val gainColor = context.getColor(R.color.wallet_green)
            val lossColor = context.getColor(R.color.wallet_red)

            if (isClosed) {
                val isProfit = subtitleValue >= BigDecimal.ZERO
                valueTv.setTextColor(
                    if (isProfit) {
                        gainColor
                    } else {
                        lossColor
                    }
                )
                subtitleTv.isGone = true
            } else {
                valueTv.setTextColor(resolveAttrColor(itemView, R.attr.text_primary))
                if (subtitlePercent == null) {
                    subtitleTv.isGone = true
                } else {
                    subtitleTv.isVisible = true
                    subtitleTv.text = context.getString(
                        R.string.Perpetual_Amount_Percent_Format,
                        formatSignedUsd(subtitleValue),
                        subtitlePercent.toDouble()
                    )
                    subtitleTv.setTextColor(
                        when {
                            subtitlePercent > BigDecimal.ZERO -> gainColor
                            subtitlePercent < BigDecimal.ZERO -> lossColor
                            else -> resolveAttrColor(itemView, R.attr.text_assist)
                        }
                    )
                }
            }
        }

        private fun formatSignedUsd(amount: BigDecimal): String {
            val fiatRate = BigDecimal(Fiats.getRate())
            val fiatSymbol = Fiats.getSymbol()
            val fiatAmount = amount.abs().multiply(fiatRate).priceFormat()
            return if (amount < BigDecimal.ZERO) {
                "-$fiatSymbol$fiatAmount"
            } else {
                "$fiatSymbol$fiatAmount"
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
}
