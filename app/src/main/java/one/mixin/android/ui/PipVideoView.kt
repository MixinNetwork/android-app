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
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.core.view.isVisible
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.ui.conversation.media.DragMediaActivity
import one.mixin.android.ui.conversation.media.VideoPlayer
import one.mixin.android.widget.AspectRatioFrameLayout
import org.jetbrains.anko.dip
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.round

class PipVideoView {

    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }
        private val SIDEX = "sidex"
        private val SIDEY = "sidey"
        private val PX = "px"
        private val PY = "py"

        fun getPipRect(aspectRatio: Float): Rect {
            val prefreences = appContext.defaultSharedPreferences
            val sidex = prefreences.getInt(SIDEX, 1)
            val sidey = prefreences.getInt(SIDEY, 0)
            val px = prefreences.getFloat(PX, 0f)
            val py = prefreences.getFloat(PY, 0f)
            val videoWidth: Int
            val videoHeight: Int
            if (aspectRatio > 0) {
                videoWidth = appContext.realSize().x * 2 / 3
                videoHeight = (videoWidth / aspectRatio).toInt()
            } else {
                videoHeight = appContext.realSize().x * 2 / 3
                videoWidth = (videoHeight * aspectRatio).toInt()
            }
            return Rect(getSideCoord(true, sidex, px, videoWidth).toFloat(), getSideCoord(false, sidey, py, videoHeight).toFloat(), videoWidth.toFloat(), videoHeight.toFloat())
        }

        fun getSideCoord(isX: Boolean, side: Int, p: Float, sideSize: Int): Int {
            val total = if (isX) {
                appContext.realSize().x - sideSize
            } else {
                appContext.realSize().y - sideSize
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
    }

    private lateinit var windowView: FrameLayout
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(WINDOW_SERVICE) as WindowManager
    }

    fun show(activity: Activity, aspectRatio: Float, rotation: Int, conversationId: String, messageId: String): TextureView {
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
                    if (abs(startX - x) >= appContext.getPixelsInCM(0.3f, true) || abs(startY - y) >= appContext.getPixelsInCM(0.3f, true)) {
                        startX = x
                        startY = y
                        return true
                    }
                }
                return super.onInterceptTouchEvent(event)
            }

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
                    } else if (windowLayoutParams.x > appContext.realSize().x - windowLayoutParams.width + maxDiff) {
                        windowLayoutParams.x = appContext.realSize().x - windowLayoutParams.width + maxDiff
                    }
                    var alpha = 1.0f
                    if (windowLayoutParams.x < 0) {
                        alpha = 1.0f + windowLayoutParams.x / maxDiff.toFloat() * 0.5f
                    } else if (windowLayoutParams.x > appContext.realSize().x - windowLayoutParams.width) {
                        alpha = 1.0f - (windowLayoutParams.x - appContext.realSize().x + windowLayoutParams.width) / maxDiff.toFloat() * 0.5f
                    }
                    if (windowView.alpha != alpha) {
                        windowView.alpha = alpha
                    }
                    maxDiff = 0
                    if (windowLayoutParams.y < -maxDiff) {
                        windowLayoutParams.y = -maxDiff
                    } else if (windowLayoutParams.y > appContext.realSize().y - windowLayoutParams.height - appContext.navigationBarHeight() * 2 + maxDiff) {
                        windowLayoutParams.y = appContext.realSize().y - windowLayoutParams.height - appContext.navigationBarHeight() * 2 + maxDiff
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
        if (aspectRatio > 0) {
            videoWidth = appContext.realSize().x * 2 / 3
            videoHeight = (videoWidth / aspectRatio).toInt()
        } else {
            videoHeight = appContext.realSize().x * 2 / 3
            videoWidth = (videoHeight * aspectRatio).toInt()
        }
        val aspectRatioFrameLayout = AspectRatioFrameLayout(activity)
        aspectRatioFrameLayout.setAspectRatio(aspectRatio, rotation)
        windowView.addView(aspectRatioFrameLayout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))

        val textureView = TextureView(activity)
        aspectRatioFrameLayout.addView(textureView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val inlineButton = ImageView(activity)
        inlineButton.scaleType = ImageView.ScaleType.CENTER
        inlineButton.visibility = View.GONE
        inlineButton.setImageResource(one.mixin.android.R.drawable.ic_outinline)
        windowView.addView(inlineButton, FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.END))
        inlineButton.setOnClickListener {
            DragMediaActivity.show(MixinApplication.appContext, conversationId, messageId, aspectRatio, VideoPlayer.player().currentPosition())
            close()
        }

        val closeButton = ImageView(activity)
        closeButton.scaleType = ImageView.ScaleType.CENTER
        closeButton.visibility = View.GONE
        closeButton.setImageResource(one.mixin.android.R.drawable.ic_close_white_24dp)
        windowView.addView(closeButton, FrameLayout.LayoutParams(appContext.dpToPx(56f), appContext.dpToPx(48f), Gravity.TOP or Gravity.START))
        closeButton.setOnClickListener {
            close()
            VideoPlayer.destroy()
        }

        textureView.setOnClickListener {
            if (closeButton.isVisible) {
                closeButton.fadeOut()
                inlineButton.fadeOut()
            } else {
                closeButton.fadeIn()
                inlineButton.fadeIn()
            }
        }
        val prefreences = appContext.defaultSharedPreferences
        val sidex = prefreences.getInt(SIDEX, 1)
        val sidey = prefreences.getInt(SIDEY, 0)
        val px = prefreences.getFloat(PX, 0f)
        val py = prefreences.getFloat(PY, 0f)
        try {
            windowLayoutParams = WindowManager.LayoutParams()
            windowLayoutParams.width = videoWidth
            windowLayoutParams.height = videoHeight
            windowLayoutParams.x = getSideCoord(true, sidex, px, videoWidth)
            windowLayoutParams.y = getSideCoord(false, sidey, py, videoHeight)
            windowLayoutParams.format = PixelFormat.TRANSLUCENT
            windowLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
            if (Build.VERSION.SDK_INT >= 26) {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.addView(windowView, windowLayoutParams)
            shown = true
        } catch (e: Exception) {
            Timber.e(e)
        }
        windowView.keepScreenOn = true
        return textureView
    }

    var shown = false

    fun close() {
        try {
            shown = false
            windowManager.removeView(windowView)
        } catch (e: Exception) {
        }
    }

    private var decelerateInterpolator: DecelerateInterpolator? = null
    private fun animateToBoundsMaybe() {
        val startX = getSideCoord(true, 0, 0f, videoWidth)
        val endX = getSideCoord(true, 1, 0f, videoWidth)
        val startY = getSideCoord(false, 0, 0f, videoHeight)
        val endY = getSideCoord(false, 1, 0f, videoHeight)
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
        } else if (abs(endX - windowLayoutParams.x) <= maxDiff || windowLayoutParams.x > appContext.realSize().x - videoWidth &&
            windowLayoutParams.x < appContext.realSize().x - videoWidth * 3 / 5) {
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
                animators.add(ObjectAnimator.ofInt(this, "x", -videoWidth))
            } else {
                animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, appContext.realSize().x))
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
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        VideoPlayer.destroy()
                    }
                })
            }
            animatorSet.playTogether(animators)
            animatorSet.start()
        }
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