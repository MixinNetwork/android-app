package one.mixin.android.ui.media.pager

import android.content.Context
import android.util.LruCache
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.exo_playback_control_view.view.*
import kotlinx.android.synthetic.main.item_pager_video_layout.view.*
import one.mixin.android.R
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadVideo
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
        itemView.pip_iv.isEnabled = false
        itemView.pip_iv.alpha = 0.5f
        itemView.close_iv.post {
            val statusBarHeight = context.statusBarHeight()
            itemView.bottom_ll.setPadding(0, statusBarHeight, 0, 0)
        }
        itemView.player_view.apply {
            if (needPostTransition) {
                player = VideoPlayer.player().player
            }
            setRefreshAction {
                messageItem.mediaUrl?.let {
                    if (messageItem.isLive()) {
                        VideoPlayer.player().loadHlsVideo(it, messageItem.messageId)
                    } else {
                        VideoPlayer.player().loadVideo(it, messageItem.messageId)
                    }
                }
            }
            callback = object : PlayerView.Callback {
                override fun onClick() {
                }

                override fun onLongClick() {
                    mediaPagerAdapterListener.onLongClick(messageItem, itemView)
                }

                override fun onRenderFirstFrame() {
                    if (VideoPlayer.player().mId == messageItem.messageId) {
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
            itemView.player_view.setUseLayout(useTopLayout = true, useBottomLayout = false)
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
                itemView.player_view.setUseLayout(useTopLayout = true, useBottomLayout = true)
                circleProgress.setBindId(null)
            } else {
                itemView.player_view.hideController()
                itemView.player_view.setUseLayout(useTopLayout = true, useBottomLayout = false)
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
        if (preStatus != MediaStatus.DONE.name && preStatus != MediaStatus.READ.name
            && (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name)) {
            messageItem.loadVideoOrLive {
                itemView.player_view.player = VideoPlayer.player().player
                VideoPlayer.player().start()
            }
        }
    }

    private fun setSize(context: Context, ratio: Float, view: View) {
        val w = context.realSize().x
        val h = context.realSize().y
        val ratioParams = view.player_view.contentFrame.layoutParams
        val previewParams = view.preview_iv.layoutParams
        if (ratio >= 1f) {
            val scaleH = (w / ratio).toInt()
            if (scaleH > h) {
                ratioParams.width = (h * ratio).toInt()
                ratioParams.height = h
            } else {
                ratioParams.width = w
                ratioParams.height = scaleH
            }
        } else {
            val scaleW = (h * ratio).toInt()
            if (scaleW > w) {
                ratioParams.width = w
                ratioParams.height = (w / ratio).toInt()
            } else {
                ratioParams.width = scaleW
                ratioParams.height = h
            }
        }
        previewParams.width = ratioParams.width
        previewParams.height = ratioParams.height
        view.player_view.contentFrame.layoutParams = ratioParams
        view.preview_iv.layoutParams = previewParams
    }
}