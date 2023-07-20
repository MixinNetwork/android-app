package one.mixin.android.ui.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.base.AsyncAdapter
import one.mixin.android.ui.conversation.base.CompressedList
import one.mixin.android.ui.conversation.base.DataFetcher
import one.mixin.android.ui.conversation.holder.TextHolder
import one.mixin.android.vo.MessageItem

class ShadowAdapter(private val onItemListener: ConversationAdapter.OnItemListener) :
    AsyncAdapter<MessageItem, RecyclerView.ViewHolder>(object : DataFetcher<MessageItem>(){
        override fun initData(): CompressedList<MessageItem> {
            TODO("Not yet implemented")
        }

        override fun loadRange(): List<MessageItem> {
            TODO("Not yet implemented")
        }
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return TextHolder(
            ItemChatTextBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            (holder as TextHolder).bind(it, null, false, false, false, false, false, onItemListener)
        }
    }
}