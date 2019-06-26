package one.mixin.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.demo.systemuidemo.SystemUIManager
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.view_photo_viewer.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.supportsOreo
import one.mixin.android.util.AnimationProperties
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.widget.BackgroundDrawable
import timber.log.Timber

class VideoViewer {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var Instance: VideoViewer? = null

        fun getInstance(): VideoViewer {
            var localInstance = Instance
            if (localInstance == null) {
                synchronized(VideoViewer::class.java) {
                    localInstance = Instance
                    if (localInstance == null) {
                        localInstance = VideoViewer()
                        Instance = localInstance
                    }
                }
            }
            return localInstance!!
        }

        fun destroy() {
            Instance?.destroy()
            Instance = null
        }

        @JvmStatic
        fun switchActivity(activity: Activity, show: Boolean) {
            if (show) {
                Instance?.setParentActivity(activity)
            } else if (Instance?.parentActivity == activity) {
                destroy()
            }
        }
    }

    private lateinit var parentActivity: Activity
    private lateinit var windowView: FrameLayout
    private var changedTextureView: TextureView? = null
        set(value) {
            field = value
            value?.surfaceTextureListener = surfaceTextureListener
        }

    private var changingTextureView = false
    private val surfaceTextureListener by lazy {
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Timber.d("onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                Timber.d("onSurfaceTextureUpdated")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Timber.d("onSurfaceTextureDestroyed")
                if (changingTextureView) {
                }
                return true
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Timber.d("onSurfaceTextureDestroyed")
            }
        }
    }
    private val windowLayoutParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams()
    }
    private val windowManager by lazy {
        parentActivity.getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val backgroundDrawable by lazy { BackgroundDrawable(0xff000000.toInt()) }

    private val mixinPlayer: MixinPlayer by lazy {
        MixinPlayer().apply {
            setOnVideoPlayerListener(videoListener)
        }
    }

    private val videoListener = object : MixinPlayer.VideoPlayerListenerWrapper() {
        override fun onRenderedFirstFrame() {
            windowView.viewer_pip.visibility = VISIBLE
            windowView.viewer_image.visibility = GONE
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            if (mixinPlayer.isPlaying() && isLoading && mixinPlayer.player.playbackState == Player.STATE_BUFFERING) {

            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    Timber.d("IDLE")
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("BUFFERING")
                }
                Player.STATE_READY -> {
                    Timber.d("READY")
                }
                Player.STATE_ENDED -> {
                    Timber.d("ENDED")
                }
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            var nWidth = width
            var nHeight = height
            if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                nWidth = height
                nHeight = width
            }
            val ratio = (if (nHeight == 0) 1 else nWidth * pixelWidthHeightRatio / nHeight) as Float
            windowView.viewer_aspect_ratio?.setAspectRatio(ratio, unappliedRotationDegrees)
        }
    }

    private var lastInsets: WindowInsets? = null
    @SuppressLint("ClickableViewAccessibility")
    fun setParentActivity(activity: Activity) {
        this.parentActivity = activity
    }

    private var init = false
    fun init() {
        if (init) return
        init = true
        parentActivity.window.addFlags(FLAG_KEEP_SCREEN_ON)
        windowView = object : FrameLayout(parentActivity) {
            override fun dispatchKeyEventPreIme(event: KeyEvent?): Boolean {
                if (event != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
                    VideoViewer.destroy()

                    return true
                }
                return super.dispatchKeyEventPreIme(event)
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                attachedToWindow = true
                val position = IntArray(2)
                windowView.viewer_image.getLocationOnScreen(position)
                Timber.d("${windowView.viewer_image.x}--${windowView.viewer_image.y}")
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                attachedToWindow = true
            }
        }
        LayoutInflater.from(parentActivity).inflate(R.layout.view_photo_viewer, windowView, true)
        windowView.clipChildren = true
        windowView.isFocusable = false
        windowView.background = backgroundDrawable
        windowView.viewer_pip?.setOnClickListener {
            switchToPip()
        }
        windowView.setOnApplyWindowInsetsListener { _, insets ->
            lastInsets = insets
            insets.consumeSystemWindowInsets()
        }
        supportsOreo {
            windowView.viewer_pip.layoutParams = SystemUIManager.generateSafeMarginLayoutParams(parentActivity.window, windowView.viewer_pip.layoutParams as ViewGroup.MarginLayoutParams)
        }
        windowView.viewer_texture?.let { videoTextureView ->
            videoTextureView.pivotX = 0f
            videoTextureView.pivotY = 0f
            videoTextureView.isOpaque = false
            changedTextureView = videoTextureView
            mixinPlayer.setVideoTextureView(videoTextureView)
        }
        windowLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowLayoutParams.format = PixelFormat.TRANSLUCENT
        windowLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
        windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
        if (Build.VERSION.SDK_INT >= 28) {
            windowLayoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private var attachedToWindow = false

    private var url: String? = null

    private fun attach(scale: Float? = null, x: Int? = null, y: Int? = null) {
        if (attachedToWindow) return
        try {
            windowManager.addView(windowView, windowLayoutParams)
            val animatorSet = AnimatorSet()
            animatorSet.duration = 250
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.playTogether(
                ObjectAnimator.ofInt(backgroundDrawable, AnimationProperties.COLOR_DRAWABLE_ALPHA, 0, 255)
            )
            if (scale != null) {
                animatorSet.playTogether(
                    ObjectAnimator.ofFloat(windowView.viewer_image, View.SCALE_X, scale, 1f),
                    ObjectAnimator.ofFloat(windowView.viewer_image, View.SCALE_Y, scale, 1f)
                )
            }
            if (x != null) {
                animatorSet.playTogether(
                    ObjectAnimator.ofFloat(windowView.viewer_image, View.TRANSLATION_X, x.toFloat(), 0f)
                )
            }
            if (y != null) {
                animatorSet.playTogether(
                    ObjectAnimator.ofFloat(windowView.viewer_image, View.TRANSLATION_Y, y.toFloat(), 0f)
                )
            }
            animatorSet.start()
            systemUi(true)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun show(activity: Activity, url: String, bitMap: Bitmap? = null, x: Int? = null, y: Int? = null) {
        setParentActivity(activity)
        init()
        windowView.viewer_image.setImageBitmap(bitMap)

        bitMap.notNullWithElse({
            val scale = it.width.toFloat() / activity.realSize().x
            attach(scale, x, y)
        }, {
            attach()
        })
        if (this.url != url) {
            mixinPlayer.loadHlsVideo(url)
            mixinPlayer.start()
        }
    }

    private fun checkInlinePermissions(): Boolean {
        if (Settings.canDrawOverlays(parentActivity)) {
            return true
        } else {
            parentActivity.let { activity ->
                AlertDialog.Builder(activity)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.live_permission)
                    .setPositiveButton(R.string.live_setting) { _, _ ->
                        try {
                            activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName)))
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }.show()
            }
        }
        return false
    }

    private var inPip = false
    private var pipAnimationInProgress = false
    private fun switchToPip() {
        if (!checkInlinePermissions() || pipAnimationInProgress) {
            return
        }
        pipAnimationInProgress = true
        val rect = PipVideoView.getPipRect(windowView.viewer_aspect_ratio.aspectRatio)
        Timber.d("${rect.x} - ${rect.y}")

        if (!inPip) {
            val with = windowView.width
            val scale = rect.width / with
            val animatorSet = AnimatorSet()
            val position = IntArray(2)
            windowView.viewer_aspect_ratio.getLocationOnScreen(position)
            animatorSet.playTogether(
                ObjectAnimator.ofInt(backgroundDrawable, AnimationProperties.COLOR_DRAWABLE_ALPHA, 0),
                ObjectAnimator.ofFloat(windowView.viewer_texture, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(windowView.viewer_texture, View.SCALE_Y, scale),
                ObjectAnimator.ofFloat(windowView.viewer_aspect_ratio, View.TRANSLATION_X, rect.x - windowView.viewer_aspect_ratio.x
                    + lastInsets.notNullWithElse({ it.systemWindowInsetLeft }, 0)),
                ObjectAnimator.ofFloat(windowView.viewer_aspect_ratio, View.TRANSLATION_Y, rect.y - windowView.viewer_aspect_ratio.y
                ))
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.duration = 250
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    pipAnimationInProgress = false
                    inPip = true
//                    changedTextureView = pipVideoView.show(parentActivity, this@VideoViewer, windowView.viewer_aspect_ratio.aspectRatio, windowView.viewer_aspect_ratio.videoRotation)
//                    mixinPlayer.setVideoTextureView(changedTextureView!!)
                    Handler().postDelayed({
                        windowManager.removeView(windowView)
                    }, 20)
                }
            })
            systemUi(false)
            animatorSet.start()
        }
    }

    internal fun exitFromPip() {
        if (pipAnimationInProgress) {
            return
        }
        pipAnimationInProgress = true
        if (inPip) {
            pipAnimationInProgress = false
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(
                ObjectAnimator.ofInt(backgroundDrawable, AnimationProperties.COLOR_DRAWABLE_ALPHA, 255),
                ObjectAnimator.ofFloat(windowView.viewer_texture, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(windowView.viewer_texture, View.SCALE_Y, 1f),
                ObjectAnimator.ofFloat(windowView.viewer_aspect_ratio, View.TRANSLATION_X, 0f),
                ObjectAnimator.ofFloat(windowView.viewer_aspect_ratio, View.TRANSLATION_Y, 0f))
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.duration = 250
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                    windowView.viewer_aspect_ratio.translationX = 0f
                    windowView.viewer_aspect_ratio.translationY = 0f
                    pipVideoView.close()
                    changedTextureView = windowView.viewer_texture
                    mixinPlayer.setVideoTextureView(changedTextureView!!)
                    windowManager.addView(windowView, windowLayoutParams)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    pipAnimationInProgress = false
                    inPip = false
                }
            })
            systemUi(true)
            animatorSet.start()
        }
    }

    private val pipVideoView by lazy {
        PipVideoView()
    }

    private fun destroy() {
        try {
            systemUi(false)
            parentActivity.window.clearFlags(FLAG_KEEP_SCREEN_ON)
            mixinPlayer.stop()
            mixinPlayer.release()
            if (!attachedToWindow) return
            windowManager.removeView(windowView)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun systemUi(show: Boolean) {
        if (show) {
            SystemUIManager.setSystemUiColor(parentActivity.window, Color.BLACK)
            SystemUIManager.lightUI(parentActivity.window, false)
        } else {
            SystemUIManager.setSystemUiColor(parentActivity.window, Color.WHITE)
            SystemUIManager.lightUI(parentActivity.window, true)
        }
    }
}