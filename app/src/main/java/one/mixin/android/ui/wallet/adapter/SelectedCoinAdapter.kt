package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectedCoinBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.wallet.alert.vo.CoinItem

class SelectedCoinAdapter(val removeCoinItem: (CoinItem) -> Unit) : RecyclerView.Adapter<SelectedCoinAdapter.SelectViewHolder>() {
    var checkedCoinItems: List<CoinItem>? = null

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_token, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedCoinItems.notNullWithElse({ it.size }, 0)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: SelectViewHolder,
        position: Int,
    ) {
        val binding = ItemSelectedCoinBinding.bind(holder.itemView)
        checkedCoinItems?.let { list ->
            val token = list[position]
            binding.avatarView.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
            binding.nameTv.text = token.symbol
        }
        holder.itemView.setOnClickListener {
            checkedCoinItems?.let { list ->
                val user = list[position]
                removeCoinItem(user)
            }
        }
    }
}