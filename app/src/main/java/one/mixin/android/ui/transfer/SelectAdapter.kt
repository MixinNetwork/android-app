package one.mixin.android.ui.transfer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.widget.ConversationSelectView

class SelectAdapter(private val allListener: (Boolean) -> Unit, private val sizeChangeListener: (Int) -> Unit) :
    RecyclerView.Adapter<SelectAdapter.ConversationViewHolder>() {

    var selectItem = ArraySet<String>()

    var conversations: List<ConversationMinimal>? = null
        set(value) {
            field = value
            displayConversations = conversations
        }

    private var displayConversations: List<ConversationMinimal>? = null

    fun getSelectedList(): List<ConversationMinimal>? {
        return conversations?.filter {
            selectItem.contains(it.conversationId)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun check(check: Boolean, conversationId: String) {
        if (check) {
            selectItem.add(conversationId)
        } else {
            selectItem.remove(conversationId) }
        notifyDataSetChanged()
    }

    var keyword: CharSequence? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            if (value.isNullOrBlank()) {
                displayConversations = conversations
            } else {
                displayConversations = conversations?.filter {
                    if (it.isGroupConversation()) {
                        it.groupName.equalsIgnoreCase(value)
                    } else {
                        it.name.equalsIgnoreCase(value)
                    }
                }
            }
            notifyDataSetChanged()
        }

    var isAll = false
        private set(value) {
            field = value
            allListener.invoke(value)
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun selectAll() {
        conversations?.let { list ->
            selectItem.addAll(list.map { it.conversationId })
        }
        isAll = true
        sizeChangeListener.invoke(selectItem.size)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deselect() {
        selectItem.clear()
        isAll = false
        sizeChangeListener.invoke(selectItem.size)
        notifyDataSetChanged()
    }

    fun toggle() {
        if (isAll) {
            deselect()
        } else {
            selectAll()
        }
    }

    override fun getItemCount(): Int {
        return displayConversations?.size ?: 0
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        if (displayConversations.isNullOrEmpty()) {
            return
        }

        val conversationItem = displayConversations!![position]
        holder.bind(
            conversationItem,
            selectItem.contains(conversationItem.conversationId),
        ) { check ->
            if (check) {
                selectItem.add(conversationItem.conversationId)
            } else {
                selectItem.remove(conversationItem.conversationId)
            }
            sizeChangeListener.invoke(selectItem.size)
            isAll = selectItem.size == conversations?.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_select_conversation,
                parent,
                false,
            ),
        )
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: ConversationMinimal, isCheck: Boolean, listener: (Boolean) -> Unit) {
            (itemView as ConversationSelectView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }
}
