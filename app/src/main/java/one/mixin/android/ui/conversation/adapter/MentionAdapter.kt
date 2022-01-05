package one.mixin.android.ui.conversation.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import one.mixin.android.R
import one.mixin.android.ui.conversation.holder.MentionHolder
import one.mixin.android.util.QueryHighlighter
import one.mixin.android.vo.User

class MentionAdapter constructor(private val onClickListener: OnUserClickListener) :
    ListAdapter<User, MentionHolder>(
        object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = false
            override fun areContentsTheSame(oldItem: User, newItem: User) = false
        }
    ) {

    var list: List<User>? = null
    var keyword: String? = null

    private lateinit var queryHighlighter: QueryHighlighter
    private fun getQueryHighlighter(context: Context): QueryHighlighter {
        if (!::queryHighlighter.isInitialized) {
            val color = context.resources.getColor(R.color.wallet_blue_secondary, null)
            val style = TextAppearanceSpan(null, 0, 0, ColorStateList.valueOf(color), null)
            queryHighlighter = QueryHighlighter(style, mode = QueryHighlighter.Mode.WORDS)
        }
        return queryHighlighter
    }

    override fun onBindViewHolder(holder: MentionHolder, position: Int) {
        getItem(position).let {
            holder.bind(it, keyword, getQueryHighlighter(holder.itemView.context), onClickListener)
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
