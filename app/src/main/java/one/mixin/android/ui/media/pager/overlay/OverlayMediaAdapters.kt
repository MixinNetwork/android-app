package one.mixin.android.ui.media.pager.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.util.LruCache
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants.BIG_IMAGE_SIZE
import one.mixin.android.R
import one.mixin.android.extension.displayRatio
import one.mixin.android.extension.inflate
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import one.mixin.android.ui.common.recyclerview.SafePagingDataAdapter
import one.mixin.android.ui.media.pager.LargeImageHolder
import one.mixin.android.ui.media.pager.MediaItemType
import one.mixin.android.ui.media.pager.MediaPagerActivity.Companion.PREFIX
import one.mixin.android.ui.media.pager.MediaPagerAdapterListener
import one.mixin.android.ui.media.pager.MediaPagerHolder
import one.mixin.android.ui.media.pager.PhotoHolder
import one.mixin.android.ui.media.pager.VideoHolder
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isImage
import one.mixin.android.widget.CircleProgress
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.largeimage.LargeImageView
import one.mixin.android.ui.media.pager.transcript.LargeImageHolder as TranscriptLargeImageHolder
import one.mixin.android.ui.media.pager.transcript.MediaPagerAdapterListener as TranscriptMediaPagerAdapterListener
import one.mixin.android.ui.media.pager.transcript.MediaPagerHolder as TranscriptMediaPagerHolder
import one.mixin.android.ui.media.pager.transcript.PhotoHolder as TranscriptPhotoHolder
import one.mixin.android.ui.media.pager.transcript.VideoHolder as TranscriptVideoHolder

@UnstableApi
internal class OverlayMediaPagerAdapter(
    private val context: Context,
    private val onDismissListener: MediaDismissLayout.OnDismissListener,
    private val listener: MediaPagerAdapterListener,
    private val onOutsideTap: () -> Unit,
) : SafePagingDataAdapter<MessageItem, MediaPagerHolder>(MessageItem.DIFF_CALLBACK) {

    var initialPos: Int = 0
    private val videoStatusCache = LruCache<String, String>(100)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MediaPagerHolder {
        val layout = createDismissContainer(context, onDismissListener)
        val circleProgress = createCircleProgress(layout)
        return when (viewType) {
            MediaItemType.LargeImage.ordinal -> {
                layout.addView(createLargeImageView(parent))
                layout.addView(circleProgress)
                LargeImageHolder(layout)
            }
            MediaItemType.Video.ordinal -> {
                layout.inflate(R.layout.item_pager_video_layout, attachToRoot = true)
                layout.addView(circleProgress)
                VideoHolder(layout, listener)
            }
            else -> {
                layout.addView(createPhotoView(parent, onOutsideTap))
                layout.addView(circleProgress)
                PhotoHolder(layout)
            }
        }
    }

    override fun onBindViewHolder(
        holder: MediaPagerHolder,
        position: Int,
    ) {
        getItem(position)?.let { messageItem ->
            (holder.itemView as? MediaDismissLayout)?.resetTransformImmediate()
            holder.itemView.tag = "$PREFIX${messageItem.messageId}"
            when (holder) {
                is PhotoHolder -> {
                    holder.bind(messageItem, position == initialPos, listener)
                }
                is LargeImageHolder -> {
                    holder.bind(messageItem, position == initialPos, listener)
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
        val messageItem = getItem(position) ?: return MediaItemType.Invalid.ordinal
        return if (messageItem.isAppCard()) {
            MediaItemType.Image.ordinal
        } else if (messageItem.isImage()) {
            if (isLargeImage(context, messageItem.mediaMimeType, messageItem.mediaWidth, messageItem.mediaHeight, messageItem.mediaSize)) {
                MediaItemType.LargeImage.ordinal
            } else {
                MediaItemType.Image.ordinal
            }
        } else {
            MediaItemType.Video.ordinal
        }
    }
}

@UnstableApi
internal class OverlayTranscriptPagerAdapter(
    private val context: Context,
    private val onDismissListener: MediaDismissLayout.OnDismissListener,
    private val listener: TranscriptMediaPagerAdapterListener,
    private val onOutsideTap: () -> Unit,
) : RecyclerView.Adapter<TranscriptMediaPagerHolder>() {

    var initialPos: Int = 0
    private val videoStatusCache = LruCache<String, String>(100)

    var list: List<ChatHistoryMessageItem> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TranscriptMediaPagerHolder {
        val layout = createDismissContainer(context, onDismissListener)
        val circleProgress = createCircleProgress(layout)
        return when (viewType) {
            MediaItemType.LargeImage.ordinal -> {
                layout.addView(createLargeImageView(parent))
                layout.addView(circleProgress)
                TranscriptLargeImageHolder(layout)
            }
            MediaItemType.Video.ordinal -> {
                layout.inflate(R.layout.item_pager_video_layout, attachToRoot = true)
                layout.addView(circleProgress)
                TranscriptVideoHolder(layout, listener)
            }
            else -> {
                layout.addView(createPhotoView(parent, onOutsideTap))
                layout.addView(circleProgress)
                TranscriptPhotoHolder(layout)
            }
        }
    }

    override fun onBindViewHolder(
        holder: TranscriptMediaPagerHolder,
        position: Int,
    ) {
        val messageItem = list[position]
        (holder.itemView as? MediaDismissLayout)?.resetTransformImmediate()
        holder.itemView.tag = "$PREFIX${messageItem.messageId}"
        when (holder) {
            is TranscriptPhotoHolder -> {
                holder.bind(messageItem, position == initialPos, listener)
            }
            is TranscriptLargeImageHolder -> {
                holder.bind(messageItem, position == initialPos, listener)
            }
            else -> {
                holder as TranscriptVideoHolder
                holder.bind(messageItem, position == initialPos, videoStatusCache)
                messageItem.mediaStatus?.let {
                    videoStatusCache.put(messageItem.messageId, it)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val messageItem = list[position]
        return if (messageItem.isImage()) {
            if (isLargeImage(context, messageItem.mediaMimeType, messageItem.mediaWidth, messageItem.mediaHeight, messageItem.mediaSize)) {
                MediaItemType.LargeImage.ordinal
            } else {
                MediaItemType.Image.ordinal
            }
        } else {
            MediaItemType.Video.ordinal
        }
    }

    override fun getItemCount(): Int = list.size

    fun getItem(position: Int): ChatHistoryMessageItem? = list.getOrNull(position)
}

@UnstableApi
private fun createDismissContainer(
    context: Context,
    listener: MediaDismissLayout.OnDismissListener,
): MediaDismissLayout =
    MediaDismissLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        dismissListener = listener
    }

private fun createCircleProgress(container: ViewGroup): CircleProgress =
    (container.inflate(R.layout.view_circle_progress) as CircleProgress).apply {
        updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = Gravity.CENTER
        }
        id = R.id.circle_progress
    }

private fun createLargeImageView(parent: ViewGroup): LargeImageView =
    LargeImageView(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

private fun createPhotoView(
    parent: ViewGroup,
    onOutsideTap: () -> Unit,
): PhotoView =
    PhotoView(parent.context).apply {
        setOnOutsidePhotoTapListener { onOutsideTap() }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

private fun isLargeImage(
    context: Context,
    mediaMimeType: String?,
    mediaWidth: Int?,
    mediaHeight: Int?,
    mediaSize: Long?,
): Boolean =
    !mediaMimeType.equals(MimeType.GIF.toString(), true) &&
        mediaHeight != null && mediaWidth != null && mediaWidth > 0 &&
        (
            mediaHeight / mediaWidth.toFloat() > context.displayRatio() * 1.5f ||
                mediaHeight > context.screenHeight() * 3 ||
                mediaWidth > context.screenWidth() * 3 ||
                (mediaSize != null && mediaSize >= BIG_IMAGE_SIZE)
            )
