package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemChainCardBinding
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.Dapp

class WalletAdapter(val onDappClick:(Dapp)->Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var connections: List<Dapp> = emptyList()

    var title: String = ""
    var subTitle: String = ""
    var action: Int? = null
    var icon: Int = R.drawable.ic_ethereum
    var onClickListener: View.OnClickListener = View.OnClickListener { }

    @SuppressLint("NotifyDataSetChanged")
    fun setContent(
        title: String,
        subTitle: String,
        @DrawableRes icon: Int,
        onClickListener: View.OnClickListener,
        action: Int? = null
    ) {
        this.title = title
        this.subTitle = subTitle
        this.icon = icon
        this.onClickListener = onClickListener
        this.action = action
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            Web3Holder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            Web3CardHolder(ItemChainCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return connections.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (0 == position) return 0
        return 1
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (getItemViewType(position) == 1) {
            (holder as Web3Holder).bind(connections[position - 1], onDappClick)
        } else {
            if (action == null) {
                (holder as Web3CardHolder).bind(title, subTitle, icon, onClickListener)
            } else {
                (holder as Web3CardHolder).bind(title, subTitle, action!!, icon, onClickListener)
            }
        }
    }
}

class Web3CardHolder(val binding: ItemChainCardBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        title: String,
        subTitle: String,
        @DrawableRes icon: Int,
        onClickListener: View.OnClickListener
    ) {
        binding.root.setContent(title, subTitle, icon, onClickListener)
    }

    fun bind(
        title: String,
        address: String,
        @StringRes action: Int,
        @DrawableRes icon: Int,
        onClickListener: View.OnClickListener
    ) {
        binding.root.setContent(title, address, action, icon, onClickListener)
    }
}

class Web3Holder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(web3: Dapp, onDappClick: (Dapp) -> Unit) {
        binding.apply {
            avatar.loadUrl(web3.iconUrl)
            name.text = web3.name
            mixinIdTv.text = web3.homeUrl
            verifiedIv.isVisible = false
            root.setOnClickListener {
                onDappClick.invoke(web3)
            }
        }
    }
}
