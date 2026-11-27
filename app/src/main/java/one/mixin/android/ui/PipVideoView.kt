@file:Suppress("DEPRECATION")

package one.mixin.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.hardware.display.DisplayManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.Keep
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.round
import one.mixin.android.extension.safeAddView
import one.mixin.android.extension.safeRemoveView
import one.mixin.android.extension.toast
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.util.RomUtil
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.XiaomiUtilities
import one.mixin.android.ui.auth.AppAuthActivity
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.media.pager.overlay.MediaThumbLocation
import one.mixin.android.ui.media.pager.overlay.MediaViewerOverlay
import one.mixin.android.widget.AspectRatioFrameLayout
import one.mixin.android.widget.PlayView
import one.mixin.android.widget.PlayView.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView.Companion.STATUS_LOADING
import one.mixin.android.widget.PlayView.Companion.STATUS_PAUSE
import one.mixin.android.widget.PlayView.Companion.STATUS_PLAYING
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.round

@UnstableApi
@SuppressLint("InvalidWakeLockTag")
class PipVideoView {
    data class PipSource(
        val conversationId: String,
        val messageId: String,
        val transcriptId: String?,
        val isVideo: Boolean,
        val mediaSource: MediaPagerActivity.MediaSource,
        val mediaUrl: String?,
        val aspectRatio: Float,
        val rotation: Int,
    )

    data class PipExpandRequest(val source: PipSource, val thumbLocation: MediaThumbLocation?)

    companion object {
        private var pendingExpandRequest: PipExpandRequest? = null

        fun consumePendingExpandRequest() = pendingExpandRequest.also { pendingExpandRequest = null }

        fun onDisplayChanged() {
            Instance?.updateWindowBounds()
        }

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

            val screen = availableSize()
            val realX = screen.x
            val realY = screen.y
            val ratio = aspectRatio.takeIf { it.isFinite() && it > 0 } ?: 1f
            val maxWidth = if (ratio > 1f) realX * 2 / 3 else realX / 2
            val maxHeight = (realY - appContext.statusBarHeight()) / if (ratio > 1f) 2 else 3
            val videoWidth = minOf(maxWidth, (maxHeight * ratio).toInt()).coerceAtLeast(1)
            val videoHeight = (videoWidth / ratio).toInt().coerceIn(1, maxHeight.coerceAtLeast(1))
            return Rect(
                getSideCoord(true, sidex, px, videoWidth, realX, realY).toFloat(),
                getSideCoord(false, sidey, py, videoHeight, realX, realY).toFloat(),
                videoWidth.toFloat(),
                videoHeight.toFloat(),
            )
        }

        private fun availableSize(): Point {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = appContext.getSystemService<DisplayManager>()!!.getDisplay(Display.DEFAULT_DISPLAY)
                val context = appContext.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
                val metrics = context.getSystemService<WindowManager>()!!.currentWindowMetrics
                val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
                return Point(
                    metrics.bounds.width() - insets.left - insets.right,
                    metrics.bounds.height() - insets.top - insets.bottom,
                )
            }
            return Point().also { appContext.getSystemService<WindowManager>()!!.defaultDisplay.getSize(it) }
        }

        fun getSideCoord(
            isX: Boolean,
            side: Int,
            p: Float,
            sideSize: Int,
            realX: Int,
            realY: Int,
        ): Int {
            val margin = 10.dp
            val start = if (isX) margin else appContext.statusBarHeight() + margin
            val end = ((if (isX) realX else realY) - sideSize - margin).coerceAtLeast(start)
            return when (side) {
                0 -> start
                1 -> end
                else -> start + round((end - start) * p.coerceIn(0f, 1f)).toInt()
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

    private var windowView: FrameLayout? = null
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    private var inlineButton: ImageView? = null
    private var closeButton: ImageView? = null
    private var playView: PlayView? = null

    private var currentSource: PipSource? = null
    private var muteButton: ImageView? = null
    private var progressBar: ProgressBar? = null
    private var screen = Point()
    private val handler = Handler(Looper.getMainLooper())
    private val hideControls = Runnable { fadeOut() }
    private val updateProgress = object : Runnable {
        override fun run() {
            val player = VideoPlayer.player().player
            val duration = player.duration
            val visible = currentSource?.isVideo == true && !player.isCurrentMediaItemDynamic && duration > 0
            progressBar?.isVisible = visible
            if (visible) {
                progressBar?.progress = (player.currentPosition.toDouble() / duration * 1000).toInt()
                if (player.isPlaying) handler.postDelayed(this, 200)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            handler.removeCallbacks(updateProgress)
            updateProgress.run()
            if (events.contains(Player.EVENT_VOLUME_CHANGED)) updateMuteButton()
            if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYER_ERROR)) {
                updatePlayState(player)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            fadeIn()
        }
    }

    private fun updateMuteButton() {
        val muted = VideoPlayer.player().isMuted()
        muteButton?.setImageResource(if (muted) R.drawable.ic_player_volume_off else R.drawable.ic_player_volume_on)
        muteButton?.contentDescription = appContext.getString(if (muted) R.string.Unmute else R.string.Mute)
    }

    private fun updatePlayState(player: Player) {
        playView?.status = when {
            player.playerError != null || player.playbackState == STATE_ENDED -> PlayView.STATUS_REFRESH
            player.playbackState == STATE_BUFFERING && player.playWhenReady -> STATUS_LOADING
            player.isPlaying -> STATUS_PLAYING
            else -> STATUS_PAUSE
        }
        fadeIn()
        if (player.playWhenReady && player.playbackState in STATE_BUFFERING..STATE_READY) {
            if (!aodWakeLock.isHeld) aodWakeLock.acquire(10 * 60 * 1000L)
        } else if (aodWakeLock.isHeld) {
            aodWakeLock.release()
        }
    }


    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private val powerManager: PowerManager by lazy {
        appContext.getSystemService<PowerManager>()!!
    }

    private val aodWakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "mixin",
        )
    }

    fun show(
        aspectRatio: Float,
        rotation: Int,
        conversationId: String,
        messageId: String,
        isVideo: Boolean,
        mediaSource: MediaPagerActivity.MediaSource,
        mediaUrl: String?,
        transcriptId: String? = null,
    ): TextureView {
        if (windowView != null) close(false)
        val source = PipSource(conversationId, messageId, transcriptId, isVideo, mediaSource, mediaUrl, aspectRatio, rotation)
        currentSource = source
        screen = availableSize()
        val rect = getPipRect(aspectRatio)
        videoWidth = rect.width.toInt()
        videoHeight = rect.height.toInt()
        windowView =
            object : FrameLayout(appContext) {
                private var startX: Float = 0f
                private var startY: Float = 0f

                override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
                    val x = event.rawX
                    val y = event.rawY
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startX = x
                        startY = y
                    } else if (event.action == MotionEvent.ACTION_MOVE) {
                        if (abs(startX - x) >=
                            appContext.getPixelsInCM(
                                0.3f,
                                true,
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
                        val maxDiff = (videoWidth * 2 / 3).coerceAtLeast(1)
                        val right = screen.x - videoWidth
                        windowLayoutParams.x = windowLayoutParams.x.coerceIn(-maxDiff, right + maxDiff)
                        val overflow = maxOf(-windowLayoutParams.x, windowLayoutParams.x - right, 0)
                        windowView?.alpha = 1f - overflow / maxDiff.toFloat() * .5f
                        val top = appContext.statusBarHeight()
                        windowLayoutParams.y = windowLayoutParams.y.coerceIn(top, (screen.y - videoHeight).coerceAtLeast(top))
                        windowView?.let { windowManager.updateViewLayout(it, windowLayoutParams) }
                        startX = x
                        startY = y
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        animateToBoundsMaybe()
                    }
                    return true
                }
            }
        windowView?.apply {
            round(12.dp)
            setBackgroundColor(Color.BLACK)
            elevation = 8.dp.toFloat()
        }
        val aspectRatioFrameLayout = AspectRatioFrameLayout(appContext)
        aspectRatioFrameLayout.setAspectRatio(aspectRatio, rotation)
        aspectRatioFrameLayout.round(12.dp)
        windowView?.addView(aspectRatioFrameLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))
        val textureView = TextureView(appContext)
        aspectRatioFrameLayout.addView(textureView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        progressBar = ProgressBar(appContext, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x40FFFFFF)
            aspectRatioFrameLayout.addView(this, FrameLayout.LayoutParams(MATCH_PARENT, 2.dp, Gravity.BOTTOM))
        }
        muteButton = ImageView(appContext).apply {
            visibility = GONE
            scaleType = ImageView.ScaleType.CENTER
            windowView?.addView(this, FrameLayout.LayoutParams(44.dp, 44.dp, Gravity.BOTTOM or Gravity.START))
            setOnClickListener {
                VideoPlayer.player().toggleMute()
                fadeIn()
            }
        }
        updateMuteButton()
        inlineButton =
            ImageView(appContext).apply {
                scaleType = ImageView.ScaleType.CENTER
                visibility = GONE
                contentDescription = appContext.getString(androidx.media3.ui.R.string.exo_controls_fullscreen_enter_description)
                setImageResource(R.drawable.ic_pip_maximum)
                windowView?.addView(
                    this,
                    FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.END),
                )
                setOnClickListener {
                    expand(source, textureView)
                }
            }

        closeButton =
            ImageView(appContext).apply {
                scaleType = ImageView.ScaleType.CENTER
                visibility = GONE
                contentDescription = appContext.getString(R.string.Close)
                setImageResource(R.drawable.ic_close_white_24dp)
                windowView?.addView(
                    this,
                    FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.START),
                )
                setOnClickListener {
                    close(true)
                    VideoPlayer.destroy()
                }
            }

        val dp42 = appContext.dpToPx(42f)
        playView =
            PlayView(appContext).apply {
                setBackgroundResource(R.drawable.bg_play_control)
                windowView?.addView(this, FrameLayout.LayoutParams(dp42, dp42, Gravity.CENTER))
                setOnClickListener {
                    val video = VideoPlayer.player()
                    when (video.player.playbackState) {
                        STATE_ENDED -> {
                            video.seekTo(0L)
                            video.start()
                        }
                        STATE_IDLE -> mediaUrl?.let {
                            if (isVideo) video.loadVideo(it, messageId, true) else video.loadHlsVideo(it, messageId, true)
                            video.start()
                        }
                        else -> video.player.playWhenReady = !video.player.playWhenReady
                    }
                    fadeIn()
                }
            }
        VideoPlayer.player().player.addListener(playerListener)
        textureView.setOnClickListener {
            if (closeButton?.isVisible == true) {
                fadeOut()
            } else {
                fadeIn()
            }
        }
        try {
            windowLayoutParams = WindowManager.LayoutParams()
            windowLayoutParams.width = videoWidth
            windowLayoutParams.height = videoHeight
            windowLayoutParams.x = rect.x.toInt()
            windowLayoutParams.y = rect.y.toInt()
            windowLayoutParams.format = PixelFormat.TRANSLUCENT
            windowLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
            if (Build.VERSION.SDK_INT >= 26) {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            windowLayoutParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.safeAddView(windowView, windowLayoutParams)
            shown = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        windowView?.keepScreenOn = true
        updatePlayState(VideoPlayer.player().player)
        updateProgress.run()
        return textureView
    }

    private fun expand(source: PipSource, textureView: TextureView) {
        val loc = IntArray(2)
        textureView.getLocationOnScreen(loc)
        val thumb = MediaThumbLocation.fromScreenBounds(
            RectF(loc[0].toFloat(), loc[1].toFloat(), (loc[0] + textureView.width).toFloat(), (loc[1] + textureView.height).toFloat()),
            cornerRadii = MediaThumbLocation.uniformRadii(12.dp.toFloat()),
            drawable = textureView.bitmap?.let { BitmapDrawable(appContext.resources, it) },
        )
        val app = MixinApplication.get()
        val host = app.topActivity?.takeUnless { it.isFinishing || it.isDestroyed }
        if (app.activityInForeground && host is FragmentActivity && host !is AppAuthActivity && host !is CallActivity && !app.isAppAuthShown()) {
            MediaViewerOverlay.showFromPip(host, source, thumb)
        } else {
            pendingExpandRequest = PipExpandRequest(source, thumb)
            if (!app.activityInForeground) {
                if (RomUtil.isMiui && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
                    toast(R.string.need_background_permission)
                }
                val intent = if (host != null) {
                    Intent(appContext, host::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                    )
                } else {
                    appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                intent?.let(appContext::startActivity)
            }
        }
    }

    private fun updateWindowBounds() {
        val source = currentSource ?: return
        if (!shown) return
        screen = availableSize()
        val rect = getPipRect(source.aspectRatio)
        videoWidth = rect.width.toInt()
        videoHeight = rect.height.toInt()
        windowLayoutParams.width = videoWidth
        windowLayoutParams.height = videoHeight
        windowLayoutParams.x = rect.x.toInt()
        windowLayoutParams.y = rect.y.toInt()
        windowView?.let { windowManager.updateViewLayout(it, windowLayoutParams) }
    }

    var shown = false

    fun close(stop: Boolean) {
        if (currentSource == null) return
        currentSource = null
        pendingExpandRequest = null
        handler.removeCallbacksAndMessages(null)
        VideoPlayer.player().player.removeListener(playerListener)
        try {
            if (stop) {
                VideoPlayer.player().stop()
            }
            shown = false
            if (aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
            windowView?.let { windowManager.safeRemoveView(it) }
        } catch (ignored: Exception) {
        }
        windowView = null
        progressBar = null
        muteButton = null
        playView = null
        inlineButton = null
        closeButton = null
    }

    private fun fadeIn() {
        handler.removeCallbacks(hideControls)
        if (VideoPlayer.player().player.isPlaying) handler.postDelayed(hideControls, 3000)
        muteButton?.fadeIn()
        closeButton?.fadeIn()
        inlineButton?.fadeIn()
        if (playView?.isVisible == false) {
            playView?.fadeIn()
        }
    }

    private fun fadeOut() {
        muteButton?.fadeOut()
        closeButton?.fadeOut()
        inlineButton?.fadeOut()
        if (playView?.status != PlayView.STATUS_REFRESH) {
            playView?.fadeOut()
        }
    }

    private var decelerateInterpolator: DecelerateInterpolator? = null

    private fun animateToBoundsMaybe() {
        val realX = screen.x
        val realY = screen.y
        val startX = getSideCoord(true, 0, 0f, videoWidth, realX, realY)
        val endX = getSideCoord(true, 1, 0f, videoWidth, realX, realY)
        val startY = getSideCoord(false, 0, 0f, videoHeight, realX, realY)
        val endY = getSideCoord(false, 1, 0f, videoHeight, realX, realY)
        var animators: ArrayList<Animator>? = null
        val editor = appContext.defaultSharedPreferences.edit()
        val maxDiff = 20f.dp
        var slideOut = false
        if (abs(startX - windowLayoutParams.x) <= maxDiff || windowLayoutParams.x < 0 && windowLayoutParams.x > -videoWidth * 2 / 5) {
            if (animators == null) {
                animators = ArrayList()
            }
            editor.putInt(SIDEX, 0)
            if (windowView?.alpha != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView!!, "alpha", 1.0f))
            }
            animators.add(ObjectAnimator.ofInt(this, "x", startX))
        } else if (abs(endX - windowLayoutParams.x) <= maxDiff || windowLayoutParams.x > realX - videoWidth &&
            windowLayoutParams.x < realX - videoWidth * 3 / 5
        ) {
            if (animators == null) {
                animators = ArrayList()
            }
            editor.putInt(SIDEX, 1)
            if (windowView?.alpha != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView!!, "alpha", 1.0f))
            }
            animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, endX))
        } else if (windowView?.alpha != 1.0f) {
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
                animators.add(ObjectAnimator.ofFloat(windowView!!, "alpha", 0.0f))
                animatorSet.addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            close(true)
                            VideoPlayer.destroy()
                        }
                    },
                )
            }
            animatorSet.playTogether(animators)
            animatorSet.start()
        }
    }

    @Keep
    fun getX(): Int = windowLayoutParams.x

    @Keep
    fun getY(): Int = windowLayoutParams.y

    @Keep
    fun setX(value: Int) {
        windowLayoutParams.x = value
        windowView?.let { windowManager.updateViewLayout(it, windowLayoutParams) }
    }

    @Keep
    fun setY(value: Int) {
        windowLayoutParams.y = value
        windowView?.let { windowManager.updateViewLayout(it, windowLayoutParams) }
    }
}
