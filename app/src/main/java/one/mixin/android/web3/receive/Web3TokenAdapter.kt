package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.setQuoteText
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class Web3TokenAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = tokens.isEmpty()

    var tokens: ArrayList<Web3TokenItem> = ArrayList(0)
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var address: String? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    private var onClickListener: ((Web3TokenItem) -> Unit)? = null

    fun setOnClickListener(onClickListener: (Web3TokenItem) -> Unit) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return Web3Holder(ItemWeb3TokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return tokens.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        (holder as Web3Holder).bind(tokens[position])
        holder.itemView.setOnClickListener {
            onClickListener?.invoke(tokens[position])
        }
    }
}

class Web3Holder(val binding: ItemWeb3TokenBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(token: Web3TokenItem) {
        binding.apply {
            avatar.bg.loadImage(token.iconUrl, holder = R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chainIcon ?: "", holder = R.drawable.ic_avatar_place_holder)

            balance.text =
                try {
                    if (token.balance.numberFormat().toFloat() == 0f) {
                        "0.00"
                    } else {
                        token.balance.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    token.balance.numberFormat()
                }
            symbolTv.text = token.symbol
            balanceAs.text = "≈ ${Fiats.getSymbol()}${token.fiat().numberFormat2()}"
            val changePercent =
                if (token.changeUsd.isBlank()) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(token.changeUsd)
                }
            changeTv.setQuoteText("${changePercent.numberFormat2()}%", changePercent >= BigDecimal.ZERO)
            if (token.priceUsd == "0") {
                priceTv.setText(R.string.NA)
                changeTv.visibility = View.GONE
            } else {
                priceTv.text = "${Fiats.getSymbol()}${token.priceFiat().numberFormat2()}"
                changeTv.visibility = View.VISIBLE
            }
        }
    }
}
