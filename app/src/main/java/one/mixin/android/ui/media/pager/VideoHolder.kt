package one.mixin.android.ui.media.pager

import android.content.Context
import android.util.LruCache
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.PlaybackPreparer
import kotlinx.android.synthetic.main.item_pager_video_layout.view.*
import kotlinx.android.synthetic.main.layout_player_view.view.*
import kotlinx.android.synthetic.main.view_player_control.view.*
import one.mixin.android.R
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.media.pager.MediaPagerActivity.Companion.PREFIX
import one.mixin.android.util.Session
import one.mixin.android.util.VideoPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLive
import one.mixin.android.vo.loadVideoOrLive
import one.mixin.android.widget.CircleProgress

class VideoHolder(
    itemView: View,
    private val mediaPagerAdapterListener: MediaPagerAdapterListener
) : MediaPagerHolder(itemView) {

    fun bind(
        messageItem: MessageItem,
        needPostTransition: Boolean,
        videoStatusCache: LruCache<String, String>
    ) {
        val context = itemView.context
        val circleProgress = itemView.findViewById<CircleProgress>(R.id.circle_progress)
        itemView.close_iv.setOnClickListener {
            mediaPagerAdapterListener.finishAfterTransition()
        }
        itemView.pip_iv.setOnClickListener {
            itemView.player_view.hideController()
            mediaPagerAdapterListener.switchToPin(messageItem, itemView)
        }
        itemView.fullscreen_iv.setOnClickListener {
            mediaPagerAdapterListener.switchFullscreen()
        }
        itemView.pip_iv.isEnabled = false
        itemView.pip_iv.alpha = 0.5f
        itemView.close_iv.post {
            val statusBarHeight = context.statusBarHeight()
            itemView.bottom_ll.setPadding(0, statusBarHeight, 0, 0)
            itemView.bottom_ll.translationY = -context.navigationBarHeight().toFloat()
        }

        itemView.player_view.apply {
            currentMessageId = messageItem.messageId
            setPlaybackPrepare(PlaybackPreparer {
                messageItem.loadVideoOrLive { showPb() }
            })
            if (needPostTransition) {
                player = VideoPlayer.player().player
            }
            refreshAction = {
                messageItem.loadVideoOrLive { showPb() }
            }
            callback = object : PlayerView.Callback {
                override fun onLongClick() {
                    mediaPagerAdapterListener.onLongClick(messageItem, itemView)
                }

                override fun onRenderFirstFrame() {
                    if (VideoPlayer.player().mId == messageItem.messageId) {
                        itemView.video_aspect_ratio.updateLayoutParams {
                            width = MATCH_PARENT
                            height = MATCH_PARENT
                        }
                        itemView.preview_iv.isVisible = false
                        itemView.pip_iv.isEnabled = true
                        itemView.pip_iv.alpha = 1f
                    } else {
                        itemView.preview_iv.isVisible = true
                        itemView.pip_iv.isEnabled = false
                        itemView.pip_iv.alpha = .5f
                    }
                }
            }
        }
        val ratio = messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat()
        setSize(context, ratio, itemView)
        itemView.tag = "$PREFIX${messageItem.messageId}"
        if (messageItem.isLive()) {
            circleProgress.isVisible = false
            itemView.preview_iv.loadImage(messageItem.thumbUrl, messageItem.thumbImage)
        } else {
            if (messageItem.mediaUrl != null) {
                itemView.preview_iv.loadVideo(messageItem.mediaUrl)
            } else {
                val imageData = messageItem.thumbImage?.decodeBase64()
                Glide.with(itemView).load(imageData).into(itemView.preview_iv)
            }
            if (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name) {
                maybeLoadVideo(videoStatusCache, messageItem)
                circleProgress.isVisible = false
                circleProgress.setBindId(null)
            } else {
                circleProgress.isVisible = true
                circleProgress.setBindId(messageItem.messageId)
                if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
                    circleProgress.enableLoading()
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
        }
        if (needPostTransition) {
            ViewCompat.setTransitionName(itemView, "transition")
            mediaPagerAdapterListener.onReadyPostTransition(itemView)
        }
    }

    private fun maybeLoadVideo(videoStatusCache: LruCache<String, String>, messageItem: MessageItem) {
        val preStatus = videoStatusCache[messageItem.messageId] ?: return
        if (preStatus != MediaStatus.DONE.name && preStatus != MediaStatus.READ.name &&
            (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name)) {
            messageItem.loadVideoOrLive {
                VideoPlayer.player().start()
            }
        }
    }

    private fun setSize(context: Context, ratio: Float, view: View) {
        val w = context.realSize().x
        val h = context.realSize().y
        val deviceRatio = w / h.toFloat()
        val ratioParams = view.player_view.video_aspect_ratio.layoutParams
        val previewParams = view.preview_iv.layoutParams
        if (deviceRatio > ratio) {
            ratioParams.height = h
            ratioParams.width = (h * ratio).toInt()
        } else {
            ratioParams.width = w
            ratioParams.height = (w / ratio).toInt()
        }
        previewParams.width = ratioParams.width
        previewParams.height = ratioParams.height
        view.player_view.video_aspect_ratio.layoutParams = ratioParams
        view.preview_iv.layoutParams = previewParams
    }
}
