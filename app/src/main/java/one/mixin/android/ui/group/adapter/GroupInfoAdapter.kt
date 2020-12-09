package one.mixin.android.ui.group.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupInfoBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter(private val self: User) : PagedHeaderAdapter<User>(User.DIFF_CALLBACK) {

    private var listener: GroupInfoListener? = null
    var participantsMap: ArrayMap<String, Participant>? = null

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_info, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder) {
            getItem(getPos(position))?.let {
                holder.bind(it, listener, self, participantsMap)
            }
        }
    }

    fun setGroupInfoListener(listener: GroupInfoListener) {
        this.listener = listener
    }

    class ItemHolder(itemView: View) : NormalHolder(itemView) {
        private val binding by lazy {
            ItemGroupInfoBinding.bind(itemView)
        }
        fun bind(
            user: User,
            listener: GroupInfoListener?,
            self: User?,
            participantsMap: ArrayMap<String, Participant>?
        ) {
            binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            binding.normal.text = user.fullName
            binding.botIv.visibility = if (user.appId != null) VISIBLE else GONE
            binding.verifyIv.visibility = if (user.isVerified != null && user.isVerified) VISIBLE else GONE
            participantsMap?.let {
                val p = it[user.userId]
                p?.let { partition ->
                    when (partition.role) {
                        ParticipantRole.OWNER.name -> {
                            binding.desc.setText(R.string.owner)
                            binding.desc.isVisible = true
                        }
                        ParticipantRole.ADMIN.name -> {
                            binding.desc.setText(R.string.admin)
                            binding.desc.isVisible = true
                        }
                        else -> {
                            binding.desc.isVisible = false
                        }
                    }
                }
            }
            itemView.setOnClickListener {
                if (self?.userId != user.userId) {
                    listener?.onClick(binding.normal, user)
                }
            }
            itemView.setOnLongClickListener {
                if (self?.userId == user.userId) return@setOnLongClickListener false
                return@setOnLongClickListener listener?.onLongClick(binding.normal, user) ?: false
            }
        }
    }

    interface GroupInfoListener {
        fun onClick(name: View, user: User)
        fun onLongClick(name: View, user: User): Boolean
    }
}
