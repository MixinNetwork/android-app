package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.textColorResource
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class Web3TokenAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = tokens.isEmpty()

    var tokens: ArrayList<Web3Token> = ArrayList(0)
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
    private var onClickListener: ((Web3Token) -> Unit)? = null

    fun setOnClickListener(onClickListener: (Web3Token) -> Unit) {
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
    fun bind(token: Web3Token) {
        binding.apply {
            avatar.bg.loadImage(token.iconUrl, holder = R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chainIconUrl, holder = R.drawable.ic_avatar_place_holder)

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
            balanceAs.text = "≈ ${Fiats.getSymbol()}${BigDecimal(token.price).multiply(BigDecimal(Fiats.getRate())).multiply(BigDecimal(token.balance)).numberFormat2()}"
            val changePercent =
                if (token.changePercent.isBlank()) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(token.changePercent)
                }
            changeTv.text = "${changePercent.numberFormat2()}%"
            changeTv.textColorResource = if (changePercent >= BigDecimal.ZERO) R.color.wallet_green else R.color.wallet_pink
            if (token.price == "0") {
                priceTv.setText(R.string.NA)
                changeTv.visibility = View.GONE
            } else {
                priceTv.text = "${Fiats.getSymbol()}${BigDecimal(token.price).multiply(BigDecimal(Fiats.getRate())).numberFormat2()}"
                changeTv.visibility = View.VISIBLE
            }
        }
    }
}
