package one.mixin.android.ui.call

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemUserSelectBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.User

class UserSelectAdapter(
    val removeItem: (User) -> Unit
) : RecyclerView.Adapter<UserSelectAdapter.SelectViewHolder>() {

    var checkedUsers = mutableListOf<User>()

    class SelectViewHolder(val binding: ItemUserSelectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val binding = ItemUserSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return checkedUsers.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val item = checkedUsers[position]
        holder.binding.avatarView.setInfo(item.fullName, item.avatarUrl, item.userId)
        holder.itemView.setOnClickListener {
            removeItem(item)
        }
    }
}
