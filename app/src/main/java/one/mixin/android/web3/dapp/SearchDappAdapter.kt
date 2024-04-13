package one.mixin.android.web3.dapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemFavoriteBinding
import one.mixin.android.vo.Dapp

class SearchDappAdapter(val onDappClick:(Dapp)->Unit) : RecyclerView.Adapter<DappHolder>() {
    var query: String = ""
    var userList: List<Dapp>? = null

    fun clear() {
        userList = null
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(
        holder: DappHolder,
        position: Int,
    ) {
        userList?.get(position)?.let {
            holder.bind(it, onDappClick)
        }
    }

    override fun getItemCount(): Int = userList?.size ?: 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DappHolder = DappHolder(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

}
