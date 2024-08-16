package one.mixin.android.ui.group.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.databinding.ItemGroupFriendBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class GroupFriendAdapter :
    RecyclerView.Adapter<GroupFriendAdapter.FriendViewHolder>(),
    StickyRecyclerHeadersAdapter<GroupFriendAdapter.HeaderViewHolder> {
    private var data: List<User>? = null
    private var mShowHeader: Boolean = false
    private var mListener: GroupFriendListener? = null
    private val mCheckedMap: HashMap<String, Boolean> = HashMap()
    var alreadyUserIds: List<String>? = null
    var isAdd: Boolean = true

    @SuppressLint("NotifyDataSetChanged")
    fun setData(
        data: List<User>?,
        showHeader: Boolean,
    ) {
        this.data = data
        mShowHeader = showHeader
        data?.filterNot { mCheckedMap.containsKey(it.identityNumber) }
            ?.forEach { mCheckedMap[it.identityNumber] = false }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearUser(user: User) {
        mCheckedMap[user.identityNumber] = false
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun getHeaderId(position: Int): Long {
        if (!mShowHeader) {
            return -1
        }
        return data.notNullWithElse(
            {
                val u = it[position]
                if (u.fullName != null) {
                    if (u.fullName.isEmpty()) ' '.code.toLong() else u.fullName[0].code.toLong()
                } else {
                    -1L
                }
            },
            -1L,
        )
    }

    override fun onBindHeaderViewHolder(
        holder: HeaderViewHolder,
        position: Int,
    ) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: FriendViewHolder,
        position: Int,
    ) {
        if (data == null || data!!.isEmpty()) {
            return
        }
        val user = data!![position]
        holder.bind(user, mListener, mCheckedMap, alreadyUserIds, isAdd)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): FriendViewHolder =
        FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_friend, parent, false))

    fun setGroupFriendListener(listener: GroupFriendListener) {
        mListener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemGroupFriendBinding.bind(itemView)

        fun bind(
            user: User,
            listener: GroupFriendListener?,
            checkedMap: HashMap<String, Boolean>,
            alreadyUserIds: List<String>?,
            isAdd: Boolean,
        ) {
            binding.normal.setName(user)
            binding.mixinIdTv.text = user.identityNumber
            binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            if (isAdd) {
                alreadyUserIds?.let {
                    if (it.contains(user.userId)) {
                        binding.cb.setButtonDrawable(R.drawable.ic_round_gray)
                        itemView.isEnabled = false
                        return
                    } else {
                        binding.cb.setButtonDrawable(R.drawable.cb_add_member)
                        itemView.isEnabled = true
                    }
                }
            }
            if (checkedMap.containsKey(user.identityNumber)) {
                binding.cb.isChecked = checkedMap[user.identityNumber]!!
            }
            binding.cb.isClickable = false
            itemView.setOnClickListener {
                binding.cb.isChecked = !binding.cb.isChecked
                checkedMap[user.identityNumber] = binding.cb.isChecked
                listener?.onItemClick(user, binding.cb.isChecked)
            }
        }
    }

    class HeaderViewHolder(val binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.header.text =
                if (!user.fullName.isNullOrEmpty()) {
                    user.fullName[0].toString()
                } else {
                    ""
                }
        }
    }

    interface GroupFriendListener {
        fun onItemClick(
            user: User,
            checked: Boolean,
        )
    }
}
