package one.mixin.android.ui.conversation.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.base.CompressedList
import one.mixin.android.ui.conversation.holder.TextHolder
import one.mixin.android.vo.MessageItem

class MessageAdapter(val data: CompressedList<MessageItem>,val onItemListener:ConversationAdapter.OnItemListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    fun getItem(position: Int): MessageItem? {
        if (position >=0 && position < data.size){
            return data[position]
        }else{
            return null
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}