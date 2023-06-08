package one.mixin.android.ui.transfer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.widget.ConversationCheckView

class SelectAdapter(val allListener: (Boolean)->Unit) : RecyclerView.Adapter<SelectAdapter.ConversationViewHolder>() {

    var selectItem = ArraySet<String>()

    var conversations: List<ConversationMinimal>? = null

    var keyword: CharSequence? = null
    private var isAll = false
        private set(value) {
            field = value
            allListener.invoke(value)
        }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        conversations?.let { list ->
            selectItem.addAll(list.map { it.conversationId })
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun deselect() {
        selectItem.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return conversations?.size ?: 0
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        if (conversations.isNullOrEmpty()) {
            return
        }

        val conversationItem = conversations!![position]
        holder.bind(
            conversationItem,
            selectItem.contains(conversationItem.conversationId)
        ) { check ->
            if (check) {
                selectItem.remove(conversationItem.conversationId)
            } else {
                selectItem.add(conversationItem.conversationId)
            }
            isAll = selectItem.size == conversations?.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_forward_conversation,
                parent,
                false,
            ),
        )
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ConversationMinimal, isCheck: Boolean, listener: (Boolean) -> Unit) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }
}
