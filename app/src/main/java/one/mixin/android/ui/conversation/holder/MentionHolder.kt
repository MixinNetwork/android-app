package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemChatMentionBinding
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.util.QueryHighlighter
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class MentionHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    private val binding by lazy {
        ItemChatMentionBinding.bind(containerView)
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        user: User,
        keyword: String?,
        queryHighlighter: QueryHighlighter,
        listener: MentionAdapter.OnUserClickListener,
    ) {
        binding.name.setName(user)
        queryHighlighter.apply(binding.name, user.fullName, keyword)
        queryHighlighter.apply(binding.idTv, "@${user.identityNumber}", "@$keyword")
        binding.iconIv.setInfo(user.fullName, user.avatarUrl, user.userId)
        itemView.setOnClickListener {
            listener.onUserClick(user)
        }
    }
}
