package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemChatMentionBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class MentionHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    private val binding by lazy {
        ItemChatMentionBinding.bind(containerView)
    }
    @SuppressLint("SetTextI18n")
    fun bind(user: User, keyword: String?, listener: MentionAdapter.OnUserClickListener) {
        binding.name.text = user.fullName
        binding.name.highLight(keyword)
        binding.idTv.text = "@${user.identityNumber}"
        if (!keyword.isNullOrEmpty()) {
            binding.idTv.highLight("@$keyword")
        }
        binding.iconIv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
        itemView.setOnClickListener {
            listener.onUserClick(user)
        }
    }
}
