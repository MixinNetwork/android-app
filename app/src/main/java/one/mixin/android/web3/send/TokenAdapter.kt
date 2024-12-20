package one.mixin.android.web3.send

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

class TokenAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = tokens.isEmpty()

    var tokens: ArrayList<TokenItem> = ArrayList(0)
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
    private var onClickListener: ((TokenItem) -> Unit)? = null

    fun setOnClickListener(onClickListener: (TokenItem) -> Unit) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return TokenHolder(ItemWeb3TokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return tokens.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        (holder as TokenHolder).bind(tokens[position])
        holder.itemView.setOnClickListener {
            onClickListener?.invoke(tokens[position])
        }
    }
}

class TokenHolder(val binding: ItemWeb3TokenBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(token: TokenItem) {
        binding.apply {
            avatar.loadToken(token)
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
            symbolTv.text = token.symbol
            balanceAs.text = "≈ ${Fiats.getSymbol()}${token.fiat().numberFormat2()}"
            if (token.priceUsd == "0") {
                priceTv.setText(R.string.NA)
                changeTv.visibility = View.GONE
            } else {
                changeTv.visibility = View.VISIBLE
                priceTv.text = "${Fiats.getSymbol()}${token.priceFiat().priceFormat()}"
                if (token.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(token.changeUsd)
                    val isRising = changeUsd >= BigDecimal.ZERO
                    changeTv.setQuoteText("${(changeUsd * BigDecimal(100)).numberFormat2()}%", isRising)
                }
            }
        }
    }
}
