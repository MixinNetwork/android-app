package one.mixin.android.ui.media.pager.transcript

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.session.Session
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.absolutePath
import one.mixin.android.widget.CircleProgress
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.gallery.MimeType

class PhotoHolder(itemView: View) : MediaPagerHolder(itemView) {
    fun bind(
        messageItem: ChatHistoryMessageItem,
        needPostTransition: Boolean,
        mediaPagerAdapterListener: MediaPagerAdapterListener,
    ) {
        val imageView = (itemView as ViewGroup).getChildAt(0) as PhotoView
        val photoViewAttacher = imageView.attacher
        val circleProgress = itemView.findViewById<CircleProgress>(R.id.circle_progress)
        if (messageItem.mediaMimeType.equals(MimeType.GIF.toString(), true)) {
            imageView.loadImage(
                messageItem.absolutePath(),
                onSuccess = { _, _ ->
                    photoViewAttacher.isZoomable = true
                    if (needPostTransition) {
                        ViewCompat.setTransitionName(imageView, "transition")
                        mediaPagerAdapterListener.onReadyPostTransition(imageView)
                    }

                },
                base64Holder = messageItem.thumbImage,
            )
        } else {
            imageView.loadImage(
                messageItem.absolutePath(),
                base64Holder = messageItem.thumbImage,
                onSuccess = { _, _ ->
                    photoViewAttacher.isZoomable = true
                    if (needPostTransition) {
                        ViewCompat.setTransitionName(imageView, "transition")
                        mediaPagerAdapterListener.onReadyPostTransition(imageView)
                    }
                },
            )
        }
        if (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name) {
            circleProgress.isVisible = false
            circleProgress.setBindId(messageItem.messageId)
        } else {
            circleProgress.isVisible = true
            circleProgress.setBindId(messageItem.messageId)
            if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
                circleProgress.enableLoading(getAttachmentProcess(messageItem.messageId))
            } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
                if (Session.getAccountId() == messageItem.userId) {
                    circleProgress.enableUpload()
                } else {
                    circleProgress.enableDownload()
                }
            } else {
                // TODO expired
            }
            circleProgress.setOnClickListener {
                mediaPagerAdapterListener.onCircleProgressClick(messageItem)
            }
        }
        imageView.setOnClickListener {
            mediaPagerAdapterListener.onClick(messageItem)
        }
        imageView.setOnLongClickListener {
            mediaPagerAdapterListener.onLongClick(messageItem, itemView)
            return@setOnLongClickListener true
        }
        if (needPostTransition) {
            ViewCompat.setTransitionName(imageView, "transition")
            mediaPagerAdapterListener.onReadyPostTransition(imageView)
        }
    }
}
