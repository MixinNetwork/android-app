package one.mixin.android.web3.swap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.ItemWeb3TokenBinding
import one.mixin.android.extension.loadImage

class SwapTokenAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    fun isEmpty() = tokens.isEmpty()

    var tokens: ArrayList<SwapToken> = ArrayList(0)
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
    private var onClickListener: ((SwapToken) -> Unit)? = null

    fun setOnClickListener(onClickListener: (SwapToken) -> Unit) {
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
    fun bind(token: SwapToken) {
        binding.apply {
            avatar.bg.loadImage(token.logoURI, R.drawable.ic_avatar_place_holder)
            avatar.badge.loadImage(token.chainLogoURI, R.drawable.ic_avatar_place_holder)
            symbolTv.text = token.symbol
            balanceAs.text = token.name
        }
    }
}
