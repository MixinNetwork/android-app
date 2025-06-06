package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectedTokenBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse

class SelectedWeb3TokenAdapter(val removeTokenItem: (Web3TokenItem) -> Unit) : RecyclerView.Adapter<SelectedWeb3TokenAdapter.SelectViewHolder>() {
    var checkedTokenItems: List<Web3TokenItem>? = null

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_token, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedTokenItems.notNullWithElse({ it.size }, 0)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: SelectViewHolder,
        position: Int,
    ) {
        val binding = ItemSelectedTokenBinding.bind(holder.itemView)
        checkedTokenItems?.let { list ->
            val token = list[position]
            binding.avatarView.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
            binding.nameTv.text = token.symbol
        }
        holder.itemView.setOnClickListener {
            checkedTokenItems?.let { list ->
                val token = list[position]
                removeTokenItem(token)
            }
        }
    }
}
