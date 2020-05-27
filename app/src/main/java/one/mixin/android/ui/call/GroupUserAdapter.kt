package one.mixin.android.ui.call

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_group_friend.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class GroupUserAdapter : ListAdapter<User, GroupUserViewHolder>(User.DIFF_CALLBACK) {
    private var listener: GroupUserListener? = null
    private val mCheckedMap: HashMap<String, Boolean> = HashMap()
    var alreadyUserIds: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        GroupUserViewHolder(parent.inflate(R.layout.item_group_friend, false))

    override fun onBindViewHolder(holder: GroupUserViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, listener, mCheckedMap, alreadyUserIds)
        }
    }

    fun removeUser(user: User) {
        mCheckedMap[user.identityNumber] = false
        notifyDataSetChanged()
    }
}

class GroupUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(
        user: User,
        listener: GroupUserListener?,
        checkedMap: HashMap<String, Boolean>,
        alreadyUserIds: List<String>?
    ) {
        itemView.name.text = user.fullName
        itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        alreadyUserIds?.let {
            if (it.contains(user.userId)) {
                itemView.cb.setButtonDrawable(R.drawable.ic_round_gray)
                itemView.isEnabled = false
                user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
                return
            } else {
                itemView.cb.setButtonDrawable(R.drawable.cb_add_member)
                itemView.isEnabled = true
            }
        }
        if (checkedMap.containsKey(user.identityNumber)) {
            itemView.cb.isChecked = checkedMap[user.identityNumber]!!
        }
        user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
        itemView.cb.isClickable = false
        itemView.setOnClickListener {
            itemView.cb.isChecked = !itemView.cb.isChecked
            checkedMap[user.identityNumber] = itemView.cb.isChecked
            listener?.onItemClick(user, itemView.cb.isChecked)
        }
    }
}

interface GroupUserListener {
    fun onItemClick(user: User, checked: Boolean)
}
