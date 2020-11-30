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
import one.mixin.android.databinding.ViewGroupInfoHeaderBinding
import one.mixin.android.ui.common.recyclerview.HeaderFilterAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.group.InviteActivity
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter : HeaderFilterAdapter<User>() {

    override var data: List<User>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var listener: GroupInfoListener? = null
    var conversation: Conversation? = null
    var self: User? = null
    var participantsMap: ArrayMap<String, Participant>? = null

    override fun getHeaderViewHolder(): HeadHolder = HeaderHolder(headerView!!)

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_info, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemHolder) {
            holder.bind(if (filtered() || position == 0) data!![position] else data!![position - 1], listener, self, participantsMap)
        } else {
            holder as HeaderHolder
            var inGroup = true
            val isAdmin = if (participantsMap != null && self != null) {
                val me = participantsMap!![self!!.userId]
                if (me == null) {
                    inGroup = false
                    false
                } else {
                    val role = me.role
                    role == ParticipantRole.OWNER.name || role == ParticipantRole.ADMIN.name
                }
            } else {
                false
            }
            holder.bind(conversation, listener, isAdmin, inGroup)
        }
    }

    override fun filtered() = data?.size != participantsMap?.size

    fun setGroupInfoListener(listener: GroupInfoListener) {
        this.listener = listener
    }

    class HeaderHolder(itemView: View) : HeadHolder(itemView) {
        fun bind(
            conversation: Conversation?,
            listener: GroupInfoListener?,
            isAdmin: Boolean,
            inGroup: Boolean
        ) {
            val binding = ViewGroupInfoHeaderBinding.bind(itemView)
            conversation?.let { c ->
                binding.addRl.setOnClickListener { listener?.onAdd() }
                binding.inviteItem.setOnClickListener {
                    InviteActivity.show(itemView.context, c.conversationId)
                }
                if (isAdmin) {
                    binding.addRl.visibility = VISIBLE
                    binding.inviteItem.visibility = VISIBLE
                } else {
                    binding.addRl.visibility = GONE
                    binding.inviteItem.visibility = GONE
                }
                binding.groupInfoNotIn.visibility = if (inGroup) GONE else VISIBLE
            }
        }
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
        fun onAdd()
        fun onClick(name: View, user: User)
        fun onLongClick(name: View, user: User): Boolean
    }
}
