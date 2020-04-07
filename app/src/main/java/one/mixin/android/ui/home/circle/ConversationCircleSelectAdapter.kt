package one.mixin.android.ui.home.circle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_group_select.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ConversationCircleSelectAdapter(
    val removeItem: (Any) -> Unit
) : RecyclerView.Adapter<ConversationCircleSelectAdapter.SelectViewHolder>() {

    var checkedItems = mutableListOf<Any>()

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_circle_select, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedItems.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val item = checkedItems[position]
        if (item is User) {
            holder.itemView.avatar_view.setInfo(item.fullName, item.avatarUrl, item.userId)
            holder.itemView.name_tv.text = item.fullName
            item.showVerifiedOrBot(holder.itemView.verified_iv, holder.itemView.bot_iv)
        } else if (item is ConversationItem) {
            holder.itemView.apply {
                if (item.isGroup()) {
                    avatar_view.setGroup(item.groupIconUrl)
                    name_tv.text = item.groupName
                } else {
                    avatar_view.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
                    name_tv.text = item.name
                }
                bot_iv.isVisible = false
                verified_iv.isVisible = false
            }
        }
        holder.itemView.setOnClickListener {
            removeItem(item)
        }
    }
}
