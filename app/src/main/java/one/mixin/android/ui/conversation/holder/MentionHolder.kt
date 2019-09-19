package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_chat_mention.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.vo.App

class MentionHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    @SuppressLint("SetTextI18n")
    fun bind(app: App, keyword: String?, listener: MentionAdapter.OnUserClickListener) {
        itemView.name.text = app.name
        itemView.id_tv.text = "@${app.appNumber}"
        itemView.id_tv.highLight(keyword)
        itemView.icon_iv.loadImage(app.icon_url)
        itemView.setOnClickListener { listener.onUserClick(app.appNumber) }
    }
}
