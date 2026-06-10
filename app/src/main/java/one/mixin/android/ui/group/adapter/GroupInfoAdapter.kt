package one.mixin.android.ui.group.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupInfoBinding
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User

class GroupInfoAdapter(private val self: User) : PagingDataAdapter<ParticipantItem, GroupInfoAdapter.ItemHolder>(ParticipantItem.DIFF_CALLBACK) {
    private var listener: GroupInfoListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemHolder =
        ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group_info, parent, false))

    override fun onBindViewHolder(
        holder: ItemHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it, listener, self)
        }
    }

    fun setGroupInfoListener(listener: GroupInfoListener) {
        this.listener = listener
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding by lazy {
            ItemGroupInfoBinding.bind(itemView)
        }

        fun bind(
            participant: ParticipantItem,
            listener: GroupInfoListener?,
            self: User?,
        ) {
            binding.avatar.setInfo(participant.fullName, participant.avatarUrl, participant.userId)
            binding.normal.setName(participant)
            binding.mixinIdTv.text = participant.identityNumber
            when (participant.role) {
                ParticipantRole.OWNER.name -> {
                    binding.desc.setText(R.string.Owner)
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
        fun onClick(
            name: View,
            participant: ParticipantItem,
        )

        fun onLongClick(
            name: View,
            participant: ParticipantItem,
        ): Boolean
    }
}
