package one.mixin.android.ui.group.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_group_info.view.*
import one.mixin.android.R
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter : ListAdapter<User, GroupInfoAdapter.ItemHolder>(User.DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_info, parent, false))

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.bind(getItem(position), listener, self, participantsMap)
    }

    private var listener: GroupInfoListener? = null
    private var conversation: Conversation? = null
    var self: User? = null
    var participantsMap: ArrayMap<String, Participant>? = null

    fun setConversation(conversation: Conversation) {
        this.conversation = conversation
    }

    fun getConversation(): Conversation? {
        return conversation
    }

    fun setGroupInfoListener(listener: GroupInfoListener) {
        this.listener = listener
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            user: User,
            listener: GroupInfoListener?,
            self: User?,
            participantsMap: ArrayMap<String, Participant>?
        ) {
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
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
        fun onClick(name: View, user: User)
        fun onLongClick(name: View, user: User): Boolean
    }
}