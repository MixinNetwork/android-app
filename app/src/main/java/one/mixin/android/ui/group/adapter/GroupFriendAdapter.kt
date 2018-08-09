package one.mixin.android.ui.group.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*
import kotlinx.android.synthetic.main.item_group_friend.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.extension.notNullElse
import one.mixin.android.vo.User

class GroupFriendAdapter : RecyclerView.Adapter<GroupFriendAdapter.FriendViewHolder>(),
    StickyRecyclerHeadersAdapter<GroupFriendAdapter.HeaderViewHolder> {

    private var data: List<User>? = null
    private var mShowHeader: Boolean = false
    private var mListener: GroupFriendListener? = null
    private val mCheckedMap: HashMap<String, Boolean> = HashMap()
    var alreadyUserIds: List<String>? = null
    var isAdd: Boolean = true

    fun setData(data: List<User>?, showHeader: Boolean) {
        this.data = data
        mShowHeader = showHeader
        data?.filterNot { mCheckedMap.containsKey(it.identityNumber) }
            ?.forEach { mCheckedMap[it.identityNumber] = false }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = notNullElse(data, { it.size }, 0)

    override fun getHeaderId(position: Int): Long {
        if (!mShowHeader) {
            return -1
        }
        return notNullElse(data, {
            val u = it[position]
            if (u.fullName != null) {
                if (u.fullName.isEmpty()) ' '.toLong() else u.fullName[0].toLong()
            } else {
                -1L
            }
        }, -1L)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user, mListener, mCheckedMap, alreadyUserIds, isAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder =
        FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_friend, parent, false))

    fun setGroupFriendListener(listener: GroupFriendListener) {
        mListener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            user: User,
            listener: GroupFriendListener?,
            checkedMap: HashMap<String, Boolean>,
            alreadyUserIds: List<String>?,
            isAdd: Boolean
        ) {
            itemView.name.text = user.fullName
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
            if (isAdd) {
                alreadyUserIds?.let {
                    if (it.contains(user.userId)) {
                        itemView.cb.setButtonDrawable(R.drawable.ic_round_gray)
                        itemView.isEnabled = false
                        itemView.bot_iv.visibility = if (user.appId != null) VISIBLE else GONE
                        itemView.verified_iv.visibility =
                            if (user.isVerified != null && user.isVerified) VISIBLE else GONE
                        return
                    } else {
                        itemView.cb.setButtonDrawable(R.drawable.cb_add_member)
                        itemView.isEnabled = true
                    }
                }
            }
            if (checkedMap.containsKey(user.identityNumber)) {
                itemView.cb.isChecked = checkedMap[user.identityNumber]!!
            }
            itemView.bot_iv.visibility = if (user.appId != null) VISIBLE else GONE
            itemView.verified_iv.visibility = if (user.isVerified != null && user.isVerified) VISIBLE else GONE
            itemView.cb.isClickable = false
            itemView.setOnClickListener {
                itemView.cb.isChecked = !itemView.cb.isChecked
                checkedMap[user.identityNumber] = itemView.cb.isChecked
                listener?.onItemClick(user, itemView.cb.isChecked)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.header.text = if (user.fullName != null && user.fullName.isNotEmpty())
                user.fullName[0].toString() else ""
        }
    }

    interface GroupFriendListener {
        fun onItemClick(user: User, checked: Boolean)
    }
}