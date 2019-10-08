package one.mixin.android.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem

class LinkAdapter : SharedMediaHeaderAdapter<LinkHolder>() {
    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        MediaHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_shared_media_link,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: LinkHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}

class LinkHolder(itemView: View) : NormalHolder(itemView) {

    fun bind(item: MessageItem) {
    }
}
