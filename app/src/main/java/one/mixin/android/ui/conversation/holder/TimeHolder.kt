package one.mixin.android.ui.conversation.holder

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_chat_time.view.*
import one.mixin.android.extension.timeAgoDate

class TimeHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    fun bind(time: String) {
        itemView.chat_time.timeAgoDate(time)
    }
}