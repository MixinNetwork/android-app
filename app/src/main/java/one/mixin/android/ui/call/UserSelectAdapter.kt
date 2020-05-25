package one.mixin.android.ui.call

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_user_select.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.User

class UserSelectAdapter(
    val removeItem: (User) -> Unit
) : RecyclerView.Adapter<UserSelectAdapter.SelectViewHolder>() {

    var checkedUsers = mutableListOf<User>()

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_select, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedUsers.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val item = checkedUsers[position]
        holder.itemView.avatar_view.setInfo(item.fullName, item.avatarUrl, item.userId)
        holder.itemView.setOnClickListener {
            removeItem(item)
        }
    }
}
