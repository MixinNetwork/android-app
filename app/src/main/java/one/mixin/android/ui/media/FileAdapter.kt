package one.mixin.android.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.Locale
import kotlinx.android.synthetic.main.item_file.view.*
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem

class FileAdapter(private val onClickListener: (MessageItem) -> Unit) :
    SharedMediaHeaderAdapter<FileHolder>() {
    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        FileHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_file,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, onClickListener)
        }
    }

    override fun getHeaderTextMargin() = 20f
}

class FileHolder(itemView: View) : NormalHolder(itemView) {
    fun bind(item: MessageItem, onClickListener: (MessageItem) -> Unit) {
        itemView.name_tv.text = item.mediaName
        itemView.size_tv.text = item.mediaSize?.fileSize()
        var type = item.mediaName
            ?.substringAfterLast(".", "")
            ?.toUpperCase(Locale.getDefault())
        if (type != null && type.length > 3) {
            type = type.substring(0, 3)
        }
        itemView.type_tv.text = type
        itemView.setOnClickListener {
            item.let(onClickListener)
        }
    }
}
