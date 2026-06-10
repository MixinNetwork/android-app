package one.mixin.android.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.widget.ConversationSelectView

class SelectedAdapter(private val selectListener: ((Boolean, String) -> Unit)?) :
    RecyclerView.Adapter<SelectedAdapter.ConversationViewHolder>() {
    private var selectItem = ArraySet<String>()

    var conversations: List<ConversationMinimal>? = null
        set(value) {
            field = value
            if (value != null) {
                selectItem.addAll(
                    value.map {
                        it.conversationId
                    },
                )
            }
        }

    override fun getItemCount(): Int {
        return conversations?.size ?: 0
    }

    override fun onBindViewHolder(
        holder: ConversationViewHolder,
        position: Int,
    ) {
        if (conversations.isNullOrEmpty()) {
            return
        }

        val conversationItem = conversations!![position]
        holder.bind(
            conversationItem,
            selectItem.contains(conversationItem.conversationId),
        ) { check ->
            selectListener?.invoke(check, conversationItem.conversationId)
            if (check) {
                selectItem.add(conversationItem.conversationId)
            } else {
                selectItem.remove(conversationItem.conversationId)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ConversationViewHolder {
        return ConversationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_select_conversation,
                parent,
                false,
            ),
        )
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            item: ConversationMinimal,
            isCheck: Boolean,
            listener: (Boolean) -> Unit,
        ) {
            (itemView as ConversationSelectView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }
}
