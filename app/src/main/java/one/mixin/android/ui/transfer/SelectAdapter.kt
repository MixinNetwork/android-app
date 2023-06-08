package one.mixin.android.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.widget.ConversationCheckView

class SelectAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectItem = ArrayList<String>()

    var conversations: List<ConversationMinimal>? = null
    var keyword: CharSequence? = null

    override fun getItemCount(): Int {
        return conversations?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (conversations.isNullOrEmpty()) {
            return
        }
        when (holder) {
            is ConversationViewHolder -> {
                val conversationItem = conversations!![position]
                holder.bind(conversationItem, true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ConversationViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_forward_conversation,
                parent,
                false,
            ),
        )
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ConversationMinimal, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item) { check ->
                }
            }
        }
    }
}
