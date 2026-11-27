@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package one.mixin.android.ui.media.pager

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import one.mixin.android.R
import one.mixin.android.databinding.LayoutPlayerViewBinding
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.reportExoPlayerException
import one.mixin.android.widget.AspectRatioFrameLayout

@UnstableApi class PlayerView(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet) {
    var player: ExoPlayer? = null
        set(value) {
            field?.apply {
                clearVideoTextureView(binding.videoTexture)
                removeListener(componentListener)
            }
            field = value
            if (useController) {
                binding.playerControlView.player = value
            }
            value?.apply {
                setVideoTextureView(binding.videoTexture)
                addListener(componentListener)
                componentListener.onVideoSizeChanged(videoSize)
            }
        }

    var callback: Callback? = null

    var currentMessageId: String? = null
        set(value) {
            field = value
            if (useController) {
                binding.playerControlView.messageId = value
            }
        }

    private var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
    private var controllerAutoShow = true
    var videoTextureRotation = 0
    var useController = true

    var refreshAction: (() -> Unit)? = null

    private val componentListener = ComponentListener()
    private val binding = LayoutPlayerViewBinding.inflate(LayoutInflater.from(context), this)
    val videoAspectRatio get() = binding.videoAspectRatio
    val playerControlView get() = binding.playerControlView

    private var lastSeekTime = 0L
    private var seekSeconds = 0
    private val gestures = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean = performClick()

        override fun onLongPress(e: MotionEvent) {
            callback?.onLongClick()
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val activePlayer = player ?: return false
            val delta = when {
                e.x < width * .35f -> -10
                e.x > width * .65f -> 10
                else -> 0
            }
            if (delta != 0 && !playerControlView.isLive && activePlayer.isCurrentMediaItemSeekable && activePlayer.duration != C.TIME_UNSET) {
                VideoPlayer.player().seekBy(delta * 1000L)
                showSeekFeedback(delta)
            } else {
                when (activePlayer.playbackState) {
                    Player.STATE_IDLE -> {
                        playerControlView.preparePlayback?.invoke()
                        activePlayer.playWhenReady = true
                    }
                    Player.STATE_ENDED -> {
                        activePlayer.seekTo(0)
                        activePlayer.playWhenReady = true
                    }
                    else -> activePlayer.playWhenReady = !activePlayer.playWhenReady
                }
            }
            return true
        }
    })

    private fun showSeekFeedback(delta: Int) {
        val now = SystemClock.uptimeMillis()
        seekSeconds = if (now - lastSeekTime < 800 && (seekSeconds > 0) == (delta > 0)) seekSeconds + delta else delta
        lastSeekTime = now
        val badge = if (delta < 0) binding.seekBackwardOverlay else binding.seekForwardOverlay
        val other = if (delta < 0) binding.seekForwardOverlay else binding.seekBackwardOverlay
        other.isVisible = false
        badge.animate().cancel()
        badge.text = if (delta > 0) "+${seekSeconds}s" else "${seekSeconds}s"
        badge.alpha = 1f
        badge.isVisible = true
        badge.animate().alpha(0f).setStartDelay(800).setDuration(180).withEndAction { badge.isVisible = false }.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!useController) return super.onTouchEvent(event)
        gestures.onTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return toggleControllerVisibility()
    }

    init {
        val useController = true
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.PlayerView)
        this.useController = ta.getBoolean(R.styleable.PlayerView_use_controller, useController)
        ta.recycle()

        isClickable = true
        binding.refreshView.setOnClickListener {
            refreshAction?.invoke()
            updateRefreshViewVisibility(false)
        }
        hideController()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        player?.addListener(componentListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player?.removeListener(componentListener)
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        super.setLayoutDirection(layoutDirection)
        binding.playerControlView.layoutDirection = layoutDirection
    }

    private fun updateRefreshViewVisibility(visible: Boolean) {
        binding.refreshView.isVisible = visible
        binding.playerControlView.inRefreshState = visible
        if (!visible && playerControlView.useBuiltInTopBar) {
            hideController()
        }
    }

    private fun applyTextureViewRotation(
        textureView: TextureView,
        video_textureRotation: Int,
    ) {
        val videoTextureWidth = textureView.width.toFloat()
        val videoTextureHeight = textureView.height.toFloat()
        if (videoTextureWidth == 0f || videoTextureHeight == 0f || video_textureRotation == 0) {
            textureView.setTransform(null)
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
                pivotY,
            )
            textureView.setTransform(transformMatrix)
        }
    }

    private fun onContentAspectRatioChanged(
        contentAspectRatio: Float,
        contentFrame: AspectRatioFrameLayout,
    ) {
        contentFrame.setAspectRatio(contentAspectRatio, videoTextureRotation)
    }

    private fun toggleControllerVisibility(): Boolean {
        if (!useController) return false

        if (!binding.playerControlView.isVisible) {
            maybeShowController(true)
        } else {
            binding.playerControlView.hide()
        }
        return true
    }

    private fun maybeShowController(isForced: Boolean) {
        if (!useController) return

        val wasShowingIndefinitely =
            binding.playerControlView.isVisible && binding.playerControlView.showTimeoutMs <= 0
        val shouldShowIndefinitely = shouldShowControllerIndefinitely()
        if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
            showController(shouldShowIndefinitely)
        }
    }

    fun showController(showIndefinitely: Boolean) {
        if (!useController) return

        binding.playerControlView.showTimeoutMs =
            if (showIndefinitely) 0 else controllerShowTimeoutMs
        binding.playerControlView.show()
    }

    fun hideController() {
        binding.playerControlView.hide()
    }

    fun switchFullscreen(fullscreen: Boolean) {
        binding.playerControlView.switchFullscreen(fullscreen)
    }

    fun setPlaybackPrepare(preparePlayback: () -> Unit) {
        if (!useController) return

        binding.playerControlView.preparePlayback = preparePlayback
    }

    fun showPb() {
        binding.pbView.isVisible = true
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
        Player.Listener,
        OnLayoutChangeListener,
        OnClickListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int,
        ) {
            applyTextureViewRotation(v as TextureView, videoTextureRotation)
        }

        override fun onClick(v: View?) {
            toggleControllerVisibility()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
            val unappliedRotationDegrees = videoSize.unappliedRotationDegrees
            val width = videoSize.width
            val height = videoSize.height
            if (width == 0 || height == 0 || VideoPlayer.player().mId != currentMessageId) {
                return
            }
            var videoAspectRatio: Float =
                if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
            if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                videoAspectRatio = 1 / videoAspectRatio
            }
            if (videoTextureRotation != 0) {
                binding.videoTexture.removeOnLayoutChangeListener(this)
            }
            videoTextureRotation = unappliedRotationDegrees
            if (videoTextureRotation != 0) {
                binding.videoTexture.addOnLayoutChangeListener(this)
            }
            applyTextureViewRotation(binding.videoTexture, videoTextureRotation)

            onContentAspectRatioChanged(videoAspectRatio, binding.videoAspectRatio)
        }

        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int,
        ) {
            if (VideoPlayer.player().mId == currentMessageId) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        showPb()
                    }
                    Player.STATE_READY -> {
                        binding.pbView.isVisible = false
                        if (playWhenReady) {
                            if (playerControlView.useBuiltInTopBar) hideController() else maybeShowController(false)
                        } else {
                            maybeShowController(false)
                        }
                    }
                    else -> {
                        binding.pbView.isVisible = false
                        maybeShowController(false)
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (VideoPlayer.player().mId == currentMessageId) {
                binding.pbView.isVisible = false
                updateRefreshViewVisibility(true)

                if (error.cause is BehindLiveWindowException) {
                    VideoPlayer.player().player.prepare()
                } else {
                    reportExoPlayerException("PlayerView", error)
                }
            }
        }

        override fun onRenderedFirstFrame() {
            if (VideoPlayer.player().mId == currentMessageId) {
                binding.playerControlView.updateLiveView()
            }
            callback?.onRenderFirstFrame()
        }
    }

    interface Callback {
        fun onLongClick()

        fun onRenderFirstFrame()
    }
}
