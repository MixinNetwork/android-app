package one.mixin.android.ui.media

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.item_media.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadBase64ImageCenterCrop
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import one.mixin.android.widget.gallery.MimeType

class MediaAdapter(private val onClickListener: (imageView: View, messageId: String) -> Unit) :
    SharedMediaHeaderAdapter<MediaHolder>() {
    var size: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MediaHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_media,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, size, onClickListener)
        }
    }

    override fun getHeaderTextMargin() = 5f
}

class MediaHolder(itemView: View) : NormalHolder(itemView) {
    fun bind(
        item: MessageItem,
        size: Int,
        onClickListener: (imageView: View, messageId: String) -> Unit
    ) {
        val params = itemView.layoutParams
        params.width = size
        params.height = size
        itemView.layoutParams = params
        val imageView = itemView.thumbnail_iv
        imageView.updateLayoutParams<ViewGroup.LayoutParams> {
            width = size
            height = size
        }
        if (item.mediaUrl == null && item.thumbImage != null) {
            val imageData = Base64.decode(item.thumbImage, Base64.DEFAULT)
            imageView.loadBase64ImageCenterCrop(imageData)
        } else {
            if (item.isImage()) {
                val isGif = item.mediaMimeType.equals(MimeType.GIF.toString(), true)
                if (isGif) {
                    imageView.loadGif(
                        item.mediaUrl.toString(),
                        centerCrop = true,
                        holder = R.drawable.ic_giphy_place_holder
                    )
                    itemView.gif_tv.isVisible = true
                } else {
                    imageView.loadImageCenterCrop(item.mediaUrl, R.drawable.image_holder)
                    itemView.gif_tv.isVisible = false
                }
                itemView.video_iv.isVisible = false
                itemView.duration_tv.isVisible = false
            } else {
                itemView.gif_tv.isVisible = false
                if (item.isVideo()) {
                    itemView.video_iv.isVisible = true
                    itemView.duration_tv.isVisible = true
                    itemView.duration_tv.text = try {
                        item.mediaDuration?.toLong()?.formatMillis()
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    itemView.video_iv.isVisible = false
                    itemView.duration_tv.isVisible = false
                }
                imageView.loadImageCenterCrop(item.mediaUrl, R.drawable.image_holder)
            }
        }
        itemView.setOnClickListener {
            onClickListener(itemView.thumbnail_iv, item.messageId)
        }
    }
}
