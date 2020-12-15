package one.mixin.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.round
import one.mixin.android.extension.toast
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.XiaomiUtilities
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.widget.AspectRatioFrameLayout
import one.mixin.android.widget.PlayView
import one.mixin.android.widget.PlayView.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView.Companion.STATUS_LOADING
import one.mixin.android.widget.PlayView.Companion.STATUS_PAUSE
import one.mixin.android.widget.PlayView.Companion.STATUS_PLAYING
import org.jetbrains.anko.dip
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.round

@SuppressLint("InvalidWakeLockTag")
class PipVideoView {

    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }
        private const val SIDEX = "sidex"
        private const val SIDEY = "sidey"
        private const val PX = "px"
        private const val PY = "py"

        fun getPipRect(aspectRatio: Float): Rect {
            val prefreences = appContext.defaultSharedPreferences
            val sidex = prefreences.getInt(SIDEX, 1)
            val sidey = prefreences.getInt(SIDEY, 0)
            val px = prefreences.getFloat(PX, 0f)
            val py = prefreences.getFloat(PY, 0f)

            val isLandscape = appContext.isLandscape()
            val realSize = appContext.realSize()
            val realX = if (isLandscape) realSize.y else realSize.x
            val realY = if (isLandscape) realSize.x else realSize.y

            var videoWidth: Int
            var videoHeight: Int
            if (aspectRatio > 1f) {
                videoWidth = realX * 2 / 3
                videoHeight = (videoWidth / aspectRatio).toInt()
            } else {
                videoHeight = realY / 3
                videoWidth = (videoHeight * aspectRatio).toInt()
                if (videoWidth > realX / 2) {
                    videoWidth = realX / 2
                    videoHeight = (videoWidth / aspectRatio).toInt()
                }
            }
            return Rect(
                getSideCoord(true, sidex, px, videoWidth, realX, realY).toFloat(),
                getSideCoord(false, sidey, py, videoHeight, realX, realY).toFloat(),
                videoWidth.toFloat(),
                videoHeight.toFloat()
            )
        }

        fun getSideCoord(isX: Boolean, side: Int, p: Float, sideSize: Int, realX: Int, realY: Int): Int {
            val total = if (isX) {
                realX - sideSize
            } else {
                realY - sideSize
            }
            return when (side) {
                0 -> appContext.dpToPx(10f)
                1 -> total - appContext.dpToPx(10f)
                else -> (round((total - appContext.dpToPx(20f)) * p) + appContext.dpToPx(10f)).toInt()
            }
        }

        @SuppressLint("StaticFieldLeak")
        private var Instance: PipVideoView? = null

        fun getInstance(): PipVideoView {
            var localInstance = Instance
            if (localInstance == null) {
                synchronized(PipVideoView::class.java) {
                    localInstance = Instance
                    if (localInstance == null) {
                        localInstance = PipVideoView()
                        Instance = localInstance
                    }
                }
            }
            return localInstance!!
        }

        fun release() {
            Instance?.close(true)
        }
    }

    private lateinit var windowView: FrameLayout
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    private var inlineButton: ImageView? = null
    private var closeButton: ImageView? = null
    private var playView: PlayView? = null

    private var mediaUrl: String? = null

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private val powerManager: PowerManager by lazy {
        appContext.getSystemService<PowerManager>()!!
    }

    private val aodWakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "mixin"
        )
    }

    fun show(
        activity: Activity,
        aspectRatio: Float,
        rotation: Int,
        conversationId: String,
        messageId: String,
        isVideo: Boolean,
        excludeLive: Boolean,
        mediaUrl: String?
    ): TextureView {
        this.mediaUrl = mediaUrl
        val isLandscape = appContext.isLandscape()
        val realSize = appContext.realSize()
        val realX = if (isLandscape) realSize.y else realSize.x
        val realY = if (isLandscape) realSize.x else realSize.y
        windowView = object : FrameLayout(activity) {
            private var startX: Float = 0f
            private var startY: Float = 0f

            override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
                val x = event.rawX
                val y = event.rawY
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startX = x
                    startY = y
                } else if (event.action == MotionEvent.ACTION_MOVE) {
                    if (abs(startX - x) >= appContext.getPixelsInCM(
                            0.3f,
                            true
                        ) || abs(startY - y) >= appContext.getPixelsInCM(0.3f, true)
                    ) {
                        startX = x
                        startY = y
                        return true
                    }
                }
                return super.onInterceptTouchEvent(event)
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                val x = event.rawX
                val y = event.rawY
                if (event.action == MotionEvent.ACTION_MOVE) {
                    val dx = x - startX
                    val dy = y - startY
                    windowLayoutParams.x = (windowLayoutParams.x + dx).toInt()
                    windowLayoutParams.y = (windowLayoutParams.y + dy).toInt()
                    var maxDiff = videoWidth * 2 / 3
                    if (windowLayoutParams.x < -maxDiff) {
                        windowLayoutParams.x = -maxDiff
                    } else if (windowLayoutParams.x > realX - windowLayoutParams.width + maxDiff) {
                        windowLayoutParams.x = realX - windowLayoutParams.width + maxDiff
                    }
                    var alpha = 1.0f
                    if (windowLayoutParams.x < 0) {
                        alpha = 1.0f + windowLayoutParams.x / maxDiff.toFloat() * 0.5f
                    } else if (windowLayoutParams.x > realX - windowLayoutParams.width) {
                        alpha =
                            1.0f - (windowLayoutParams.x - realX + windowLayoutParams.width) / maxDiff.toFloat() * 0.5f
                    }
                    if (windowView.alpha != alpha) {
                        windowView.alpha = alpha
                    }
                    maxDiff = 0
                    if (windowLayoutParams.y < -maxDiff) {
                        windowLayoutParams.y = -maxDiff
                    } else if (windowLayoutParams.y > realY - windowLayoutParams.height - appContext.navigationBarHeight() * 2 + maxDiff) {
                        windowLayoutParams.y =
                            realY - windowLayoutParams.height - appContext.navigationBarHeight() * 2 + maxDiff
                    }
                    windowManager.updateViewLayout(windowView, windowLayoutParams)
                    startX = x
                    startY = y
                } else if (event.action == MotionEvent.ACTION_UP) {
                    animateToBoundsMaybe()
                }
                return true
            }
        }
        if (aspectRatio > 1f) {
            videoWidth = realX * 2 / 3
            videoHeight = (videoWidth / aspectRatio).toInt()
        } else {
            videoHeight = realY / 3
            videoWidth = (videoHeight * aspectRatio).toInt()
            if (videoWidth > realX / 2) {
                videoWidth = realX / 2
                videoHeight = (videoWidth / aspectRatio).toInt()
            }
        }
        val aspectRatioFrameLayout = AspectRatioFrameLayout(activity)
        aspectRatioFrameLayout.setAspectRatio(aspectRatio, rotation)
        aspectRatioFrameLayout.round(8.dp)
        windowView.addView(aspectRatioFrameLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))
        val textureView = TextureView(activity)
        aspectRatioFrameLayout.addView(textureView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        inlineButton = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.CENTER
            visibility = GONE
            setImageResource(R.drawable.ic_pip_maximum)
            windowView.addView(
                this,
                FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.END)
            )
            setOnClickListener {
                if (XiaomiUtilities.isMIUI() && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
                    appContext.toast(R.string.need_background_permission)
                }
                MediaPagerActivity.show(
                    MixinApplication.appContext,
                    conversationId,
                    messageId,
                    aspectRatio,
                    excludeLive
                )
            }
        }

        closeButton = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.CENTER
            visibility = GONE
            setImageResource(R.drawable.ic_close_white_24dp)
            windowView.addView(
                this,
                FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.START)
            )
            setOnClickListener {
                close(true)
                VideoPlayer.destroy()
            }
        }

        val dp42 = appContext.dpToPx(42f)
        playView = PlayView(activity).apply {
            windowView.addView(this, FrameLayout.LayoutParams(dp42, dp42, Gravity.CENTER))
            val playbackState = VideoPlayer.player().player.playbackState
            status = when (playbackState) {
                STATE_IDLE, STATE_ENDED -> {
                    isVisible = true
                    STATUS_IDLE
                }
                STATE_BUFFERING -> {
                    isVisible = true
                    STATUS_LOADING
                }
                else -> {
                    if (VideoPlayer.player().isPlaying()) {
                        isVisible = false
                        STATUS_PLAYING
                    } else {
                        isVisible = true
                        STATUS_IDLE
                    }
                }
            }
            setOnClickListener {
                when (status) {
                    STATUS_IDLE -> {
                        mediaUrl?.let {
                            if (isPlayerIdle()) {
                                if (isVideo) {
                                    VideoPlayer.player().loadVideo(it, messageId, true)
                                } else {
                                    VideoPlayer.player().loadHlsVideo(it, messageId, true)
                                }
                            }
                            start()
                        }
                    }
                    STATUS_LOADING, STATUS_PLAYING -> {
                        pause()
                    }
                    STATUS_PAUSE -> {
                        start()
                    }
                    PlayView.STATUS_REFRESH -> {
                        mediaUrl?.let {
                            if (isVideo) {
                                VideoPlayer.player().loadVideo(it, messageId, true)
                            } else {
                                VideoPlayer.player().loadHlsVideo(it, messageId, true)
                            }
                        }
                    }
                }
            }
        }

        VideoPlayer.player().setOnMediaPlayerListener(
            object : MixinPlayer.MediaPlayerListenerWrapper() {
                override fun onPlayerError(mid: String, error: ExoPlaybackException) {
                    playView?.fadeIn()
                    playView?.status = PlayView.STATUS_REFRESH
                }

                override fun onPlayerStateChanged(mid: String, playWhenReady: Boolean, playbackState: Int) {
                    when (playbackState) {
                        STATE_ENDED -> {
                            stop()
                            if (aodWakeLock.isHeld) {
                                aodWakeLock.release()
                            }
                        }
                        STATE_IDLE -> {
                            if (isVideo) {
                                stop()
                                fadeIn()
                            } else {
                                playView?.fadeIn()
                                playView?.status = PlayView.STATUS_REFRESH
                            }
                            if (aodWakeLock.isHeld) {
                                aodWakeLock.release()
                            }
                        }
                        STATE_READY -> {
                            if (playWhenReady) {
                                fadeOut()
                                playView?.status = STATUS_PLAYING
                            } else {
                                playView?.status = STATUS_PAUSE
                            }
                            if (!aodWakeLock.isHeld) {
                                aodWakeLock.acquire()
                            }
                        }
                        STATE_BUFFERING -> {
                            playView?.fadeIn()
                            playView?.status = STATUS_LOADING
                        }
                    }
                }
            }
        )

        textureView.setOnClickListener {
            if (closeButton?.isVisible == true) {
                fadeOut()
            } else {
                fadeIn()
            }
        }
        val preferences = appContext.defaultSharedPreferences
        val sidex = preferences.getInt(SIDEX, 1)
        val sidey = preferences.getInt(SIDEY, 0)
        val px = preferences.getFloat(PX, 0f)
        val py = preferences.getFloat(PY, 0f)
        try {
            windowLayoutParams = WindowManager.LayoutParams()
            windowLayoutParams.width = videoWidth
            windowLayoutParams.height = videoHeight
            windowLayoutParams.x = getSideCoord(true, sidex, px, videoWidth, realX, realY)
            windowLayoutParams.y = getSideCoord(false, sidey, py, videoHeight, realX, realY)
            windowLayoutParams.format = PixelFormat.TRANSLUCENT
            windowLayoutParams.gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 26) {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            windowLayoutParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.addView(windowView, windowLayoutParams)
            shown = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        windowView.keepScreenOn = true
        if (!aodWakeLock.isHeld) {
            aodWakeLock.acquire()
        }
        return textureView
    }

    var shown = false

    fun close(stop: Boolean) {
        try {
            if (stop) {
                stop()
            }
            shown = false
            if (aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
            windowManager.removeView(windowView)
        } catch (e: Exception) {
        }
    }

    private fun fadeIn() {
        closeButton?.fadeIn()
        inlineButton?.fadeIn()
        if (playView?.isVisible == false) {
            playView?.fadeIn()
        }
    }

    private fun fadeOut() {
        closeButton?.fadeOut()
        inlineButton?.fadeOut()
        if (playView?.status != PlayView.STATUS_REFRESH) {
            playView?.fadeOut()
        }
    }

    private fun isPlayerIdle() =
        VideoPlayer.player().player.playbackState == STATE_IDLE ||
            VideoPlayer.player().player.playbackState == STATE_ENDED

    private var decelerateInterpolator: DecelerateInterpolator? = null
    private fun animateToBoundsMaybe() {
        val realSize = appContext.realSize()
        val isLandscape = appContext.isLandscape()
        val realX = if (isLandscape) realSize.y else realSize.x
        val realY = if (isLandscape) realSize.x else realSize.y
        val startX = getSideCoord(true, 0, 0f, videoWidth, realX, realY)
        val endX = getSideCoord(true, 1, 0f, videoWidth, realX, realY)
        val startY = getSideCoord(false, 0, 0f, videoHeight, realX, realY)
        val endY = getSideCoord(false, 1, 0f, videoHeight, realX, realY)
        var animators: ArrayList<Animator>? = null
        val editor = appContext.defaultSharedPreferences.edit()
        val maxDiff = appContext.dip(20f)
        var slideOut = false
        if (abs(startX - windowLayoutParams.x) <= maxDiff || windowLayoutParams.x < 0 && windowLayoutParams.x > -videoWidth * 2 / 5) {
            if (animators == null) {
                animators = ArrayList()
            }
            editor.putInt(SIDEX, 0)
            if (windowView.alpha != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView, "alpha", 1.0f))
            }
            animators.add(ObjectAnimator.ofInt(this, "x", startX))
        } else if (abs(endX - windowLayoutParams.x) <= maxDiff || windowLayoutParams.x > realX - videoWidth &&
            windowLayoutParams.x < realX - videoWidth * 3 / 5
        ) {
            if (animators == null) {
                animators = ArrayList()
            }
            editor.putInt(SIDEX, 1)
            if (windowView.alpha != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView, "alpha", 1.0f))
            }
            animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, endX))
        } else if (windowView.alpha != 1.0f) {
            if (animators == null) {
                animators = ArrayList()
            }
            if (windowLayoutParams.x < 0) {
                animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, -videoWidth))
            } else {
                animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, realX))
            }
            slideOut = true
        } else {
            editor.putFloat(PX, (windowLayoutParams.x - startX) / (endX - startX).toFloat())
            editor.putInt(SIDEX, 2)
        }
        if (!slideOut) {
            when {
                abs(startY - windowLayoutParams.y) <= maxDiff -> {
                    if (animators == null) {
                        animators = ArrayList()
                    }
                    editor.putInt(SIDEY, 0)
                    animators.add(ObjectAnimator.ofInt(this, "y", startY))
                }
                abs(endY - windowLayoutParams.y) <= maxDiff -> {
                    if (animators == null) {
                        animators = ArrayList()
                    }
                    editor.putInt(SIDEY, 1)
                    animators.add(ObjectAnimator.ofInt(this, "y", endY))
                }
                else -> {
                    editor.putFloat(PY, (windowLayoutParams.y - startY) / (endY - startY).toFloat())
                    editor.putInt(SIDEY, 2)
                }
            }
            editor.apply()
        }
        if (animators != null) {
            if (decelerateInterpolator == null) {
                decelerateInterpolator = DecelerateInterpolator()
            }
            val animatorSet = AnimatorSet()
            animatorSet.interpolator = decelerateInterpolator
            animatorSet.duration = 150
            if (slideOut) {
                animators.add(ObjectAnimator.ofFloat(windowView, "alpha", 0.0f))
                animatorSet.addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            close(true)
                            VideoPlayer.destroy()
                        }
                    }
                )
            }
            animatorSet.playTogether(animators)
            animatorSet.start()
        }
    }

    private fun start() {
        playView?.status = STATUS_PLAYING
        VideoPlayer.player().start()
    }

    private fun pause() {
        playView?.status = STATUS_PAUSE
        VideoPlayer.player().pause()
    }

    private fun stop() {
        playView?.status = STATUS_IDLE
        VideoPlayer.player().stop()
    }

    @Keep
    fun getX() {
        windowLayoutParams.x
    }

    @Keep
    fun getY() {
        windowLayoutParams.y
    }

    @Keep
    fun setX(value: Int) {
        windowLayoutParams.x = value
        windowManager.updateViewLayout(windowView, windowLayoutParams)
    }

    @Keep
    fun setY(value: Int) {
        windowLayoutParams.y = value
        windowManager.updateViewLayout(windowView, windowLayoutParams)
    }
}
