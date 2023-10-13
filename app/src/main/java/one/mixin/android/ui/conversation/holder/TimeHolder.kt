package one.mixin.android.ui.conversation.holder

import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.extension.timeAgoDate

class TimeHolder constructor(val binding: ItemChatTimeBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(time: String) {
        binding.chatTime.timeAgoDate(time)
    }
}
