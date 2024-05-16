package one.mixin.android.web3.dapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.vo.Dapp

class SearchDappAdapter(val onUrlClick: (String) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var query: String = ""
    var url: String? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var userList: List<Dapp>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        userList = null
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (position == 0 && showTip()) {
            (holder as TipHolder).bind(url, onUrlClick)
        } else {
            userList?.get(
                if (showTip()) {
                    position - 1
                } else {
                    position
                },
            )?.let {
                (holder as DappHolder).bind(it, onUrlClick)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (showTip() && position == 0) {
            return 1
        }
        return 0
    }

    override fun getItemCount(): Int = (userList?.size ?: 0) + (if (showTip()) 1 else 0)

    private fun showTip() = !url.isNullOrBlank()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            TipHolder(ItemSearchTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            DappHolder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }
}
