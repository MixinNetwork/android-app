package one.mixin.android.ui.media.pager

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackPreparer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.spherical.SingleTapListener
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.layout_player_view.view.*
import kotlinx.android.synthetic.main.layout_player_view.view.video_aspect_ratio
import kotlinx.android.synthetic.main.layout_player_view.view.video_texture
import one.mixin.android.R
import one.mixin.android.util.VideoPlayer
import one.mixin.android.widget.AspectRatioFrameLayout

class PlayerView(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet) {
    var player: Player? = null
        set(value) {
            field?.apply {
                videoComponent?.clearVideoTextureView(video_texture)
                videoComponent?.removeVideoListener(componentListener)
                removeListener(componentListener)
            }
            field = value
            if (useController) {
                player_control_view.player = value
            }
            value?.apply {
                videoComponent?.setVideoTextureView(video_texture)
                videoComponent?.addVideoListener(componentListener)
                addListener(componentListener)
            }
        }

    var callback: Callback? = null

    var currentMessageId: String? = null
        set(value) {
            field = value
            if (useController) {
                player_control_view.messageId = value
            }
        }

    private var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
    private var controllerAutoShow = true
    var videoTextureRotation = 0
    var useController = true

    var refreshAction: (() -> Unit)? = null

    private val componentListener = ComponentListener()

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_player_view, this)

        val useController = true
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.PlayerView)
        this.useController = ta.getBoolean(R.styleable.PlayerView_use_controller, useController)
        ta.recycle()

        setOnClickListener {
            if (!this.useController || player == null) return@setOnClickListener
            toggleControllerVisibility()
        }
        setOnLongClickListener {
            callback?.onLongClick()
            return@setOnLongClickListener false
        }
        refresh_view.setOnClickListener {
            refreshAction?.invoke()
            updateRefreshViewVisibility(false)
        }
        hideController()
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        super.setLayoutDirection(layoutDirection)
        player_control_view.layoutDirection = layoutDirection
    }

    private fun updateRefreshViewVisibility(visible: Boolean) {
        refresh_view.isVisible = visible
        player_control_view.inRefreshState = visible
        if (!visible) {
            hideController()
        }
    }

    private fun applyTextureViewRotation(video_texture: TextureView, video_textureRotation: Int) {
        val videoTextureWidth = video_texture.width.toFloat()
        val videoTextureHeight = video_texture.height.toFloat()
        if (videoTextureWidth == 0f || videoTextureHeight == 0f || video_textureRotation == 0) {
            video_texture.setTransform(null)
        } else {
            val transformMatrix = Matrix()
            val pivotX = videoTextureWidth / 2
            val pivotY = videoTextureHeight / 2
            transformMatrix.postRotate(video_textureRotation.toFloat(), pivotX, pivotY)

            val originalTextureRect = RectF(0f, 0f, videoTextureWidth, videoTextureHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                videoTextureWidth / rotatedTextureRect.width(),
                videoTextureHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
            video_texture.setTransform(transformMatrix)
        }
    }

    private fun onContentAspectRatioChanged(
        contentAspectRatio: Float,
        contentFrame: AspectRatioFrameLayout
    ) {
        contentFrame.setAspectRatio(contentAspectRatio, videoTextureRotation)
    }

    private fun toggleControllerVisibility(): Boolean {
        if (!useController || player == null) return false

        if (!player_control_view.isVisible) {
            maybeShowController(true)
        } else {
            player_control_view.hide()
        }
        return true
    }

    private fun maybeShowController(isForced: Boolean) {
        if (!useController) return

        val wasShowingIndefinitely =
            player_control_view.isVisible && player_control_view.showTimeoutMs <= 0
        val shouldShowIndefinitely = shouldShowControllerIndefinitely()
        if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
            showController(shouldShowIndefinitely)
        }
    }

    fun showController(showIndefinitely: Boolean) {
        if (!useController) return

        player_control_view.showTimeoutMs = if (showIndefinitely) 0 else controllerShowTimeoutMs
        player_control_view.show()
    }

    fun hideController() {
        player_control_view.hide()
    }

    fun switchFullscreen(fullscreen: Boolean) {
        player_control_view.switchFullscreen(fullscreen)
    }

    fun setPlaybackPrepare(playbackPreparer: PlaybackPreparer) {
        if (!useController) return

        player_control_view.playbackPreparer = playbackPreparer
    }

    fun showPb() {
        pb_view.isVisible = true
        updateRefreshViewVisibility(false)
    }

    private fun shouldShowControllerIndefinitely(): Boolean {
        if (player == null) return true

        val playbackState = player!!.playbackState
        return controllerAutoShow &&
            (
                playbackState == Player.STATE_IDLE ||
                    playbackState == Player.STATE_ENDED ||
                    !player!!.playWhenReady
                )
    }

    inner class ComponentListener :
        Player.EventListener,
        VideoListener,
        OnLayoutChangeListener,
        SingleTapListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            applyTextureViewRotation(v as TextureView, videoTextureRotation)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return toggleControllerVisibility()
        }

        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            if (VideoPlayer.player().mId != currentMessageId) {
                return
            }
            var videoAspectRatio: Float =
                if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
            if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                videoAspectRatio = 1 / videoAspectRatio
            }
            if (videoTextureRotation != 0) {
                video_texture.removeOnLayoutChangeListener(this)
            }
            videoTextureRotation = unappliedRotationDegrees
            if (videoTextureRotation != 0) {
                video_texture.addOnLayoutChangeListener(this)
            }
            applyTextureViewRotation(video_texture, videoTextureRotation)

            onContentAspectRatioChanged(videoAspectRatio, video_aspect_ratio)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (VideoPlayer.player().mId == currentMessageId) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        showPb()
                    }
                    Player.STATE_READY -> {
                        pb_view.isVisible = false
                        if (playWhenReady) {
                            hideController()
                        } else {
                            maybeShowController(false)
                        }
                    }
                    else -> {
                        pb_view.isVisible = false
                        maybeShowController(false)
                    }
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (VideoPlayer.player().mId == currentMessageId) {
                pb_view.isVisible = false
                updateRefreshViewVisibility(true)
            }
        }

        override fun onRenderedFirstFrame() {
            if (VideoPlayer.player().mId == currentMessageId) {
                player_control_view.updateLiveView()
            }
            callback?.onRenderFirstFrame()
        }
    }

    interface Callback {
        fun onLongClick()

        fun onRenderFirstFrame()
    }
}
