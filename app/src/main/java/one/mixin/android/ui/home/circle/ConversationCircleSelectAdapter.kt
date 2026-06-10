package one.mixin.android.ui.home.circle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupSelectBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroupConversation

class ConversationCircleSelectAdapter(
    val removeItem: (Any) -> Unit,
) : RecyclerView.Adapter<ConversationCircleSelectAdapter.SelectViewHolder>() {
    var checkedItems = mutableListOf<Any>()

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_circle_select, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedItems.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(
        holder: SelectViewHolder,
        position: Int,
    ) {
        val item = checkedItems[position]
        val binding = ItemGroupSelectBinding.bind(holder.itemView)
        if (item is User) {
            binding.avatarView.setInfo(item.fullName, item.avatarUrl, item.userId)
            binding.nameTv.setName(item)
        } else if (item is ConversationMinimal) {
            holder.itemView.apply {
                if (item.isGroupConversation()) {
                    binding.avatarView.setGroup(item.groupIconUrl)
                } else {
                    binding.avatarView.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
                }
                binding.nameTv.setName(item)
            }
        }
        holder.itemView.setOnClickListener {
            removeItem(item)
        }
    }
}
