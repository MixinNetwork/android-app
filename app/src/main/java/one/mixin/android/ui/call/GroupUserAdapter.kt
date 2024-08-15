package one.mixin.android.ui.call

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupFriendBinding
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment.Companion.GROUP_VOICE_MAX_COUNT
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class GroupUserAdapter : ListAdapter<User, GroupUserViewHolder>(User.DIFF_CALLBACK) {
    var listener: GroupUserListener? = null
    private val mCheckedMap: HashMap<String, Boolean> = HashMap()
    var alreadyUserIds: List<String>? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) =
        GroupUserViewHolder(ItemGroupFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: GroupUserViewHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it, listener, mCheckedMap, alreadyUserIds)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeUser(user: User) {
        mCheckedMap[user.identityNumber] = false
        notifyDataSetChanged()
    }
}

class GroupUserViewHolder(val binding: ItemGroupFriendBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        user: User,
        listener: GroupUserListener?,
        checkedMap: HashMap<String, Boolean>,
        alreadyUserIds: List<String>?,
    ) {
        binding.normal.setName(user)
        binding.mixinIdTv.text = user.identityNumber
        binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        alreadyUserIds?.let {
            if (it.contains(user.userId)) {
                binding.cb.setButtonDrawable(R.drawable.ic_round_gray)
                binding.root.isEnabled = false
                return
            } else {
                binding.cb.setButtonDrawable(R.drawable.cb_add_member)
                binding.root.isEnabled = true
            }
        }
        binding.cb.isChecked = checkedMap[user.identityNumber] == true
        binding.cb.isClickable = false
        binding.root.setOnClickListener {
            if ((alreadyUserIds?.size ?: (0 + checkedMap.size)) >= GROUP_VOICE_MAX_COUNT) {
                listener?.onFull()
                return@setOnClickListener
            }
            binding.cb.isChecked = !binding.cb.isChecked
            checkedMap[user.identityNumber] = binding.cb.isChecked
            listener?.onItemClick(user, binding.cb.isChecked)
        }
    }
}

interface GroupUserListener {
    fun onItemClick(
        user: User,
        checked: Boolean,
    )

    fun onFull()
}
