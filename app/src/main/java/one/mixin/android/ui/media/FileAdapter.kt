package one.mixin.android.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_file.view.*
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.Session
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import java.util.Locale

class FileAdapter(private val onClickListener: (MessageItem) -> Unit) :
    SharedMediaHeaderAdapter<FileHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
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
        item.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.file_expired.visibility = VISIBLE
                    itemView.file_progress.visibility = GONE
                    itemView.type_tv.visibility = GONE
                }
                MediaStatus.PENDING.name -> {
                    itemView.file_expired.visibility = GONE
                    itemView.file_progress.visibility = VISIBLE
                    itemView.type_tv.visibility = GONE
                    itemView.file_progress.enableLoading()
                    itemView.file_progress.setBindId(item.messageId)
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    itemView.file_expired.visibility = GONE
                    itemView.file_progress.visibility = GONE
                    itemView.type_tv.visibility = VISIBLE
                    itemView.file_progress.setDone()
                    itemView.file_progress.setBindId(null)
                }
                MediaStatus.CANCELED.name -> {
                    itemView.file_expired.visibility = GONE
                    itemView.file_progress.visibility = VISIBLE
                    itemView.type_tv.visibility = GONE
                    if (Session.getAccountId() == item.userId && item.mediaUrl != null) {
                        itemView.file_progress.enableUpload()
                    } else {
                        itemView.file_progress.enableDownload()
                    }
                    itemView.file_progress.setBindId(item.messageId)
                    itemView.file_progress.setProgress(-1)
                }
            }
        }
        itemView.setOnClickListener {
            item.let(onClickListener)
        }
    }
}
