package one.mixin.android.ui.group.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_group_info.view.*
import kotlinx.android.synthetic.main.view_group_info_header.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.HeaderFilterAdapter
import one.mixin.android.ui.common.recyclerview.HeaderListUpdateCallback
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.group.InviteActivity
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter(private val rv: RecyclerView) : HeaderFilterAdapter<User>() {

    override var data: List<User>? = null
        set(value) {
            if (field == null) {
                field = value
                notifyDataSetChanged()
            } else {
                val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val old = field?.get(oldItemPosition)
                        val new = value?.get(newItemPosition)
                        return old?.userId == new?.userId && old?.relationship == new?.relationship
                    }

                    override fun getOldListSize() = field?.size ?: 0

                    override fun getNewListSize() = value?.size ?: 0

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val old = field?.get(oldItemPosition)
                        val new = value?.get(newItemPosition)
                        return old == new
                    }
                })
                field = value
                val recyclerViewState = rv.layoutManager?.onSaveInstanceState()
                if (headerView != null) {
                    diffResult.dispatchUpdatesTo(HeaderListUpdateCallback(this))
                } else {
                    diffResult.dispatchUpdatesTo(this)
                }
                rv.layoutManager?.onRestoreInstanceState(recyclerViewState)
            }
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
            conversation?.let { c ->
                itemView.add_rl.setOnClickListener { listener?.onAdd() }
                itemView.invite_item.setOnClickListener {
                    InviteActivity.show(itemView.context, c.conversationId)
                }
                if (isAdmin) {
                    itemView.add_rl.visibility = VISIBLE
                    itemView.invite_item.visibility = VISIBLE
                } else {
                    itemView.add_rl.visibility = GONE
                    itemView.invite_item.visibility = GONE
                }
                itemView.group_info_not_in.visibility = if (inGroup) GONE else VISIBLE
            }
        }
    }

    class ItemHolder(itemView: View) : NormalHolder(itemView) {
        fun bind(
            user: User,
            listener: GroupInfoListener?,
            self: User?,
            participantsMap: ArrayMap<String, Participant>?
        ) {
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            itemView.normal.text = user.fullName
            itemView.bot_iv.visibility = if (user.appId != null) VISIBLE else GONE
            itemView.verify_iv.visibility = if (user.isVerified != null && user.isVerified) VISIBLE else GONE
            participantsMap?.let {
                val p = it[user.userId]
                p?.let {
                    val role = it.role
                    itemView.desc.visibility = if (role == ParticipantRole.OWNER.name ||
                        role == ParticipantRole.ADMIN.name) View.VISIBLE else View.GONE
                }
            }
            itemView.setOnClickListener {
                if (self?.userId != user.userId) {
                    listener?.onClick(itemView.normal, user)
                }
            }
            itemView.setOnLongClickListener {
                if (self?.userId == user.userId) return@setOnLongClickListener false
                return@setOnLongClickListener listener?.onLongClick(itemView.normal, user) ?: false
            }
        }
    }

    interface GroupInfoListener {
        fun onAdd()
        fun onClick(name: View, user: User)
        fun onLongClick(name: View, user: User): Boolean
    }
}