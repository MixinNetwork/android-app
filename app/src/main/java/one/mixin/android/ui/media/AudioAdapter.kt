package one.mixin.android.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem

class AudioAdapter : SharedMediaHeaderAdapter<AudioHolder>() {
    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        AudioHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_audio,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}

class AudioHolder(itemView: View) : NormalHolder(itemView) {

    fun bind(item: MessageItem) {
    }
}
