package one.mixin.android.ui.media

import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.item_media.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isVideo

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
        // itemView.bg.setBackgroundResource(0)
        // if (item.isGif) {
        //     itemView.gif_tv.isVisible = true
        //     itemView.video_iv.isVisible = false
        //     itemView.duration_tv.isVisible = false
        //     imageView.loadGif(
        //         item.mediaUrl.toString(),
        //         centerCrop = true,
        //         holder = R.drawable.ic_giphy_place_holder
        //     )
        // } else {
            itemView.gif_tv.isVisible = false
            if (item.isVideo()) {
                itemView.video_iv.isVisible = true
                itemView.duration_tv.isVisible = true
                itemView.duration_tv.text =
                    DateUtils.formatElapsedTime(item.mediaDuration?.toLong() ?: 0 / 1000)
            } else {
                itemView.video_iv.isVisible = false
                itemView.duration_tv.isVisible = false
            }
            imageView.loadImageCenterCrop(item.mediaUrl, R.drawable.image_holder)
        // }
        itemView.setOnClickListener {
            onClickListener(itemView.thumbnail_iv, item.messageId)
        }
    }
}