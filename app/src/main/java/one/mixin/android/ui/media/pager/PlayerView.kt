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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.spherical.SingleTapListener
import com.google.android.exoplayer2.video.VideoListener
import one.mixin.android.R
import one.mixin.android.widget.AspectRatioFrameLayout

class PlayerView(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    val controller: PlayerControlView
    val textureView: TextureView
    val contentFrame: AspectRatioFrameLayout

    var player: Player? = null
        set(value) {
            field?.apply {
                videoComponent?.clearVideoTextureView(textureView)
                videoComponent?.removeVideoListener(componentListener)
                removeListener(componentListener)
            }
            field = value
            if (useController) {
                controller.player = value
            }
            value?.apply {
                videoComponent?.setVideoTextureView(textureView)
                videoComponent?.addVideoListener(componentListener)
                addListener(componentListener)
            }
            maybeShowController(false)
        }

    var callback: Callback? = null

    private var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
    private var controllerAutoShow = true
    var textureViewRotation = 0
    var useController = true

    private val componentListener = ComponentListener()

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_player_view, this)
        controller = findViewById(R.id.player_control_view)
        textureView = findViewById(R.id.video_texture)
        contentFrame = findViewById(R.id.video_aspect_ratio)

        val useController = true
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.PlayerView)
        this.useController = ta.getBoolean(R.styleable.PlayerView_use_controller, useController)
        ta.recycle()

        setOnClickListener {
            callback?.onClick()
            if (!this.useController || player == null) return@setOnClickListener
            toggleControllerVisibility()
        }
        setOnLongClickListener {
            callback?.onLongClick()
            return@setOnLongClickListener false
        }
        hideController()
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        super.setLayoutDirection(layoutDirection)
        controller.layoutDirection = layoutDirection
    }

    fun setUseLayout(useTopLayout: Boolean, useBottomLayout: Boolean) {
        controller.useTopLayout = useTopLayout
        controller.useBottomLayout = useBottomLayout
    }

    fun setRefreshAction(action: () -> Unit) {
        controller.refreshAction = action
    }

    private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth == 0f || textureViewHeight == 0f || textureViewRotation == 0) {
            textureView.setTransform(null)
        } else {
            val transformMatrix = Matrix()
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(),
                textureViewHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
            textureView.setTransform(transformMatrix)
        }
    }

    protected fun onContentAspectRatioChanged(
        contentAspectRatio: Float,
        contentFrame: AspectRatioFrameLayout
    ) {
        contentFrame.setAspectRatio(contentAspectRatio, textureViewRotation)
    }

    private fun toggleControllerVisibility(): Boolean {
        if (!useController ||player == null) return false

        if (!controller.isVisible) {
            maybeShowController(true)
        } else {
            controller.hide()
        }
        return true
    }

    private fun maybeShowController(isForced: Boolean) {
        if (!useController) return

        val wasShowingIndefinitely = controller.isVisible && controller.showTimeoutMs <= 0
        val shouldShowIndefinitely = shouldShowControllerIndefinitely()
        if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
            showController(shouldShowIndefinitely)
        }
    }

    private fun showController(showIndefinitely: Boolean) {
        if (!useController) return

        controller.showTimeoutMs = if (showIndefinitely) 0 else controllerShowTimeoutMs
        controller.show()
    }

    fun hideController() {
        controller.hide()
    }

    private fun shouldShowControllerIndefinitely(): Boolean {
        if (player == null) return true

        val playbackState = player!!.playbackState
        return controllerAutoShow
            && (playbackState == Player.STATE_IDLE
            || playbackState == Player.STATE_ENDED
            || !player!!.playWhenReady)
    }

    inner class ComponentListener : Player.EventListener, VideoListener, OnLayoutChangeListener, SingleTapListener {
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
            applyTextureViewRotation(v as TextureView, textureViewRotation)
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return toggleControllerVisibility()
        }

        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            var videoAspectRatio: Float = if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
            if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                videoAspectRatio = 1 / videoAspectRatio
            }
            if (textureViewRotation != 0) {
                textureView.removeOnLayoutChangeListener(this)
            }
            textureViewRotation = unappliedRotationDegrees
            if (textureViewRotation != 0) {
                textureView.addOnLayoutChangeListener(this)
            }
            applyTextureViewRotation(textureView, textureViewRotation)

            onContentAspectRatioChanged(videoAspectRatio, contentFrame)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            maybeShowController(false)
        }

        override fun onRenderedFirstFrame() {
            controller.updateLiveView()
            callback?.onRenderFirstFrame()
        }
    }

    interface Callback {
        fun onClick()

        fun onLongClick()

        fun onRenderFirstFrame()
    }
}