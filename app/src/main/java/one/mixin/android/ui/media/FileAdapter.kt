package one.mixin.android.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.ItemFileBinding
import one.mixin.android.extension.fileSize
import one.mixin.android.session.Session
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import java.util.Locale

class FileAdapter(private val onClickListener: (MessageItem) -> Unit, private val onLongClickListener: (String) -> Unit) :
    SharedMediaHeaderAdapter<FileHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) =
        FileHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_file,
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: FileHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it, onClickListener, onLongClickListener)
        }
    }

    override fun getHeaderTextMargin() = 20f
}

class FileHolder(itemView: View) : NormalHolder(itemView) {
    private val binding = ItemFileBinding.bind(itemView)

    fun bind(
        item: MessageItem,
        onClickListener: (MessageItem) -> Unit,
        onLongClickListener: (String) -> Unit,
    ) {
        binding.nameTv.text = item.mediaName
        binding.sizeTv.text = item.mediaSize?.fileSize()
        var type =
            item.mediaName
                ?.substringAfterLast(".", "")
                ?.uppercase(Locale.getDefault())
        if (type != null && type.length > 3) {
            type = type.substring(0, 3)
        }
        binding.typeTv.text = type
        item.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.fileExpired.visibility = VISIBLE
                    binding.fileProgress.visibility = GONE
                    binding.typeTv.visibility = GONE
                }
                MediaStatus.PENDING.name -> {
                    binding.fileExpired.visibility = GONE
                    binding.fileProgress.visibility = VISIBLE
                    binding.typeTv.visibility = GONE
                    binding.fileProgress.enableLoading()
                    binding.fileProgress.setBindId(item.messageId)
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.fileExpired.visibility = GONE
                    binding.fileProgress.visibility = GONE
                    binding.typeTv.visibility = VISIBLE
                    binding.fileProgress.setDone()
                    binding.fileProgress.setBindId(null)
                }
                MediaStatus.CANCELED.name -> {
                    binding.fileExpired.visibility = GONE
                    binding.fileProgress.visibility = VISIBLE
                    binding.typeTv.visibility = GONE
                    if (Session.getAccountId() == item.userId && item.mediaUrl != null) {
                        binding.fileProgress.enableUpload()
                    } else {
                        binding.fileProgress.enableDownload()
                    }
                    binding.fileProgress.setBindId(item.messageId)
                    binding.fileProgress.setProgress(-1)
                }
            }
        }
        itemView.setOnClickListener {
            item.let(onClickListener)
        }
        itemView.setOnLongClickListener {
            item.messageId.let(onLongClickListener)
            true
        }
    }
}
