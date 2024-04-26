package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.Web3Account
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.ItemChainCardBinding
import one.mixin.android.databinding.ItemWeb3HeaderBinding
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.textColorResource
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class Web3WalletAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var account: Web3Account? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun isEmpty() = tokens.isEmpty()
    private var onClickAction: ((Int) -> Unit)? = null
    private var onWeb3ClickListener: ((Web3Token) -> Unit)? = null

    fun setOnClickAction(onClickListener: (Int) -> Unit) {
        this.onClickAction = onClickListener
    }

    fun setOnWeb3Click(onWeb3ClickListener: (Web3Token) -> Unit) {
        this.onWeb3ClickListener = onWeb3ClickListener
    }


    val tokens: List<Web3Token>
        get() {
            return account?.tokens ?: emptyList()
        }

    var address: String? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var title: String = ""
    var subTitle: String = ""
    var icon: Int = R.drawable.ic_ethereum
    var onClickListener: OnClickListener = OnClickListener { }

    @SuppressLint("NotifyDataSetChanged")
    fun setContent(
        title: String,
        subTitle: String,
        @DrawableRes icon: Int,
        onClickListener: OnClickListener,
    ) {
        this.title = title
        this.subTitle = subTitle
        this.icon = icon
        this.onClickListener = onClickListener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            Web3CardHolder(ItemChainCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else if (viewType == 1) {
            Web3HeaderHolder(ItemWeb3HeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            Web3Holder(ItemWeb3TokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return tokens.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (0 == position) {
            return if (account == null && address.isNullOrEmpty()) {
                0
            } else {
                1
            }
        }
        return 2
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val type = getItemViewType(position)
        when (type) {
            0 -> {
                (holder as Web3CardHolder).bind(title, subTitle, icon, onClickListener)
            }

            1 -> {
                account?.let { account ->
                    (holder as Web3HeaderHolder).bind(account.balance, onClickAction)
                }
            }

            2 -> (holder as Web3Holder).bind(tokens[position - 1], onWeb3ClickListener)
        }
    }
}

class Web3HeaderHolder(val binding: ItemWeb3HeaderBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(balance: String, onClickListener: ((Int) -> Unit)?) {
        binding.header.setText(balance)
        binding.header.setOnClickAction(onClickListener)
    }
}

class Web3CardHolder(val binding: ItemChainCardBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        title: String,
        subTitle: String,
        @DrawableRes icon: Int,
        onClickListener: OnClickListener,
    ) {
        binding.root.setContent(title, subTitle, icon, onClickListener)
    }
}

class Web3Holder(val binding: ItemWeb3TokenBinding) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(token: Web3Token, onWeb3ClickListener: ((Web3Token) -> Unit)?) {
        binding.apply {
            root.setOnClickListener{ onWeb3ClickListener?.invoke(token) }
            avatar.bg.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chainIconUrl, R.drawable.ic_avatar_place_holder)
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
            val changePercent = BigDecimal(token.changePercent)
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
