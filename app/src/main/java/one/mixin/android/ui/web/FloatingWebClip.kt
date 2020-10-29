package one.mixin.android.ui.web

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.Keep
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.putInt
import one.mixin.android.extension.realSize
import one.mixin.android.widget.AvatarsView
import kotlin.math.abs
import kotlin.math.min

class FloatingWebClip {
    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }

        @SuppressLint("StaticFieldLeak")
        private var Instance: FloatingWebClip? = null

        fun getInstance(): FloatingWebClip {
            var localInstance = Instance
            if (localInstance == null) {
                synchronized(FloatingWebClip::class.java) {
                    localInstance = Instance
                    if (localInstance == null) {
                        localInstance = FloatingWebClip()
                        Instance = localInstance
                    }
                }
            }
            return requireNotNull(localInstance)
        }

        private const val FX = "floating_x"
        private const val FY = "floating_y"
    }

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private lateinit var windowView: ViewGroup
    private lateinit var avatarsView: AvatarsView
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private val preferences by lazy {
        appContext.defaultSharedPreferences
    }
    private var isShown = false
    fun init(activity: Activity) {
        if (!::windowView.isInitialized) {
            initWindowView(activity)
        }
        if (!::windowLayoutParams.isInitialized) {
            initWindowLayoutParams()
        }
    }

    fun show(activity: Activity? = null) {
        if (!isShown) {
            if (activity != null) init(activity)
            isShown = true
            windowManager.addView(windowView, windowLayoutParams)
        }
        reload()
    }

    private fun reload() {
        avatarsView.addList(
            clips.map {
                it.app?.iconUrl ?: ""
            }
        )
        updateSize(clips.size)
        animateToBoundsMaybe()
    }

    private fun updateSize(count: Int) {
        windowLayoutParams.width = (44 + 18 * min((count - 1), 2) + 2).dp
        windowLayoutParams.height = (44 + 2).dp
        windowManager.updateViewLayout(windowView, windowLayoutParams)
    }

    private fun initWindowView(activity: Activity) {
        val realSize = appContext.realSize()
        val realX = realSize.x
        val realY = realSize.y
        windowView = object : FrameLayout(activity) {
            private var startX: Float = 0f
            private var startY: Float = 0f
            private var downX = -1f
            private var downY = -1f
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

                if (event.action == MotionEvent.ACTION_DOWN) {
                    downX = event.rawX
                    downY = event.rawY
                } else if (event.action == MotionEvent.ACTION_MOVE) {
                    val dx = x - startX
                    val dy = y - startY

                    windowLayoutParams.x = (windowLayoutParams.x + dx).toInt()
                    windowLayoutParams.y = (windowLayoutParams.y + dy).toInt()
                    var maxDiff = 100.dp
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
                    if (abs(event.rawX - downX) < 8.dp && abs(event.rawY - downY) < 8.dp) {
                        expand(appContext)
                    } else {
                        animateToBoundsMaybe()
                    }
                }
                return true
            }
        }

        avatarsView = AvatarsView(activity).apply {
            initParams(1, 36)
        }

        windowView.addView(
            FrameLayout(activity).apply {
                setBackgroundResource(R.drawable.bg_floating_group)
                z = 1.dp.toFloat()
                addView(
                    avatarsView,
                    ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 4.dp
                        bottomMargin = 4.dp
                    }
                )
            },
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    fun hide() {
        if (!isShown) return
        isShown = false
        windowManager.removeView(windowView)
    }

    private fun initWindowLayoutParams() {
        windowLayoutParams = WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        windowLayoutParams.x = preferences.getInt(FX, 0)
        windowLayoutParams.y = preferences.getInt(FY, 0)
        windowLayoutParams.format = PixelFormat.TRANSLUCENT
        windowLayoutParams.gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 26) {
            windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        windowLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    private var decelerateInterpolator: DecelerateInterpolator? = null
    private fun animateToBoundsMaybe() {
        val realSize = appContext.realSize()
        val realX = realSize.x
        val startX = windowLayoutParams.x
        val endX = if (startX >= realX / 2) {
            realSize.x - windowLayoutParams.width
        } else {
            0
        }
        preferences.putInt(FX, endX)
        preferences.putInt(FY, windowLayoutParams.y)
        var animators: ArrayList<Animator>? = null
        if (animators == null) {
            animators = ArrayList()
        }
        animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, endX))
        if (decelerateInterpolator == null) {
            decelerateInterpolator = DecelerateInterpolator()
        }
        val animatorSet = AnimatorSet()
        animatorSet.interpolator = decelerateInterpolator
        animatorSet.duration = 150
        animatorSet.playTogether(animators)
        animatorSet.start()
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
