package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.ui.search.holder.BotHolder
import one.mixin.android.vo.User

class SearchBotAdapter : RecyclerView.Adapter<BotHolder>() {
    var onItemClickListener: SearchBotsFragment.UserListener? = null
    var query: String = ""

    var userList: List<User>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        userList = null
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(
        holder: BotHolder,
        position: Int,
    ) {
        userList?.get(position)?.let {
            holder.bind(it, query, onItemClickListener)
        }
    }

    override fun getItemCount(): Int = userList?.size ?: 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BotHolder = BotHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
}
