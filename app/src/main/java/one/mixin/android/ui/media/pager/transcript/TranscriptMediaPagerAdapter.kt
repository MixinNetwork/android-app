package one.mixin.android.ui.media.pager.transcript

import android.annotation.SuppressLint
import android.content.Context
import android.util.LruCache
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.shizhefei.view.largeimage.LargeImageView
import one.mixin.android.Constants.BIG_IMAGE_SIZE
import one.mixin.android.R
import one.mixin.android.extension.displayRatio
import one.mixin.android.extension.inflate
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import one.mixin.android.ui.media.pager.MediaItemType
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.isImage
import one.mixin.android.widget.CircleProgress
import one.mixin.android.widget.PhotoView.DismissFrameLayout
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.PhotoView.PhotoViewAttacher
import one.mixin.android.widget.gallery.MimeType

class TranscriptMediaPagerAdapter(
    private val context: Context,
    private val onDismissListener: DismissFrameLayout.OnDismissListener,
    private val onMediaPagerAdapterListener: MediaPagerAdapterListener,
) : RecyclerView.Adapter<MediaPagerHolder>() {

    var initialPos: Int = 0

    private val videoStatusCache = LruCache<String, String>(100)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaPagerHolder {
        val layout = DismissFrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        layout.setDismissListener(onDismissListener)
        val circleProgress = layout.inflate(R.layout.view_circle_progress) as CircleProgress
        circleProgress.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = Gravity.CENTER
        }
        circleProgress.id = R.id.circle_progress
        return when (viewType) {
            MediaItemType.LargeImage.ordinal -> {
                layout.addView(createLargeImageView(parent))
                layout.addView(circleProgress)
                LargeImageHolder(layout)
            }
            MediaItemType.Video.ordinal -> {
                createVideoView(layout)
                layout.addView(circleProgress)
                VideoHolder(layout, onMediaPagerAdapterListener)
            }
            else -> {
                layout.addView(createPhotoView(parent))
                layout.addView(circleProgress)
                PhotoHolder(layout)
            }
        }
    }

    override fun onBindViewHolder(holder: MediaPagerHolder, position: Int) {
        getItem(position).let { messageItem ->
            when (holder) {
                is PhotoHolder -> {
                    holder.bind(messageItem, position == initialPos, onMediaPagerAdapterListener)
                }
                is LargeImageHolder -> {
                    holder.bind(messageItem, position == initialPos, onMediaPagerAdapterListener)
                }
                else -> {
                    holder as VideoHolder
                    holder.bind(messageItem, position == initialPos, videoStatusCache)
                    messageItem.mediaStatus?.let {
                        videoStatusCache.put(messageItem.messageId, it)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val messageItem = getItem(position)
        return if (messageItem.isImage()) {
            if (!messageItem.mediaMimeType.equals(
                    MimeType.GIF.toString(),
                    true
                ) && messageItem.mediaHeight != null && messageItem.mediaWidth != null &&
                (
                    messageItem.mediaHeight / messageItem.mediaWidth.toFloat() > context.displayRatio() * 1.5f ||
                        messageItem.mediaHeight > context.screenHeight() * 3 ||
                        messageItem.mediaWidth > context.screenWidth() * 3 ||
                        (messageItem.mediaSize != null && messageItem.mediaSize >= BIG_IMAGE_SIZE)
                    )
            ) {
                MediaItemType.LargeImage.ordinal
            } else {
                MediaItemType.Image.ordinal
            }
        } else {
            MediaItemType.Video.ordinal
        }
    }

    private fun createVideoView(container: ViewGroup) = container.inflate(R.layout.item_pager_video_layout, attachToRoot = true)

    private fun createLargeImageView(container: ViewGroup) = LargeImageView(container.context)

    private fun createPhotoView(parent: ViewGroup): PhotoView {
        val imageView = PhotoView(parent.context)
        val photoViewAttacher = PhotoViewAttacher(imageView)
        photoViewAttacher.isZoomable = false
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return imageView
    }

    var list: List<ChatHistoryMessageItem> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getItem(position: Int) = list[position]
}
