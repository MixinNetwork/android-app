package one.mixin.android.ui.group.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupInfoBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter(private val self: User) : PagedHeaderAdapter<ParticipantItem>(ParticipantItem.DIFF_CALLBACK) {

    private var listener: GroupInfoListener? = null

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_info, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder) {
            getItem(getPos(position))?.let {
                holder.bind(it, listener, self)
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
            participant: ParticipantItem,
            listener: GroupInfoListener?,
            self: User?,
        ) {
            binding.avatar.setInfo(participant.fullName, participant.avatarUrl, participant.userId)
            binding.normal.text = participant.fullName
            binding.botIv.visibility = if (participant.appId != null) VISIBLE else GONE
            binding.verifyIv.visibility = if (participant.isVerified != null && participant.isVerified) VISIBLE else GONE
            when (participant.role) {
                ParticipantRole.OWNER.name -> {
                    binding.desc.setText(R.string.owner)
                    binding.desc.isVisible = true
                }
                ParticipantRole.ADMIN.name -> {
                    binding.desc.setText(R.string.Admin)
                    binding.desc.isVisible = true
                }
                else -> {
                    binding.desc.isVisible = false
                }
            }
            itemView.setOnClickListener {
                if (self?.userId != participant.userId) {
                    listener?.onClick(binding.normal, participant)
                }
            }
            itemView.setOnLongClickListener {
                if (self?.userId == participant.userId) return@setOnLongClickListener false
                return@setOnLongClickListener listener?.onLongClick(binding.normal, participant) ?: false
            }
        }
    }

    interface GroupInfoListener {
        fun onClick(name: View, participant: ParticipantItem)
        fun onLongClick(name: View, participant: ParticipantItem): Boolean
    }
}
