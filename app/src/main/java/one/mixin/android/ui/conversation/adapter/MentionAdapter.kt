package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import one.mixin.android.R
import one.mixin.android.ui.conversation.holder.MentionHolder
import one.mixin.android.vo.User

class MentionAdapter constructor(private val onClickListener: OnUserClickListener) :
    ListAdapter<User, MentionHolder>(object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = false
        override fun areContentsTheSame(oldItem: User, newItem: User) = false
    }) {

    var list: List<User>? = null
    var keyword: String? = null

    override fun onBindViewHolder(holder: MentionHolder, position: Int) {
        getItem(position).let {
            holder.bind(it, keyword, onClickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentionHolder =
        MentionHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_chat_mention,
                parent,
                false
            )
        )

    interface OnUserClickListener {
        fun onUserClick(user: User)
    }
}
