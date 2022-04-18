package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.runOnUiThread
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.Rect
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.webrtc.TAG_CALL
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.round

class PipCallView {
    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }
        private const val CALL_SIDE_X = "call_side_x"
        private const val CALL_SIDE_Y = "call_side_y"
        private const val CALL_PX = "call_px"
        private const val CALL_PY = "call_py"

        private const val SIZE = 80f

        private var INSTANCE: PipCallView? = null

        fun get(): PipCallView {
            var localInstance = INSTANCE
            if (localInstance == null) {
                synchronized(PipCallView::class.java) {
                    localInstance = INSTANCE
                    if (localInstance == null) {
                        localInstance = PipCallView()
                        INSTANCE = localInstance
                    }
                }
            }
            return localInstance!!
        }

        fun getPipRect(): Rect {
            val sp = appContext.defaultSharedPreferences
            val sideX = sp.getInt(CALL_SIDE_X, 1)
            val sideY = sp.getInt(CALL_SIDE_Y, 0)
            val px = sp.getFloat(CALL_PX, 0f)
            val py = sp.getFloat(CALL_PY, 0f)
            val size = SIZE.dp.toFloat()

            val isLandscape = appContext.isLandscape()
            val realSize = appContext.realSize()
            val realX = if (isLandscape) realSize.y else realSize.x
            val realY = if (isLandscape) realSize.x else realSize.y

            return Rect(
                getSideCoordinate(true, sideX, px, size, realX, realY),
                getSideCoordinate(false, sideY, py, size, realX, realY),
                size,
                size
            )
        }

        private fun getSideCoordinate(isX: Boolean, side: Int, p: Float, sideSize: Float, realX: Int, realY: Int): Float {
            val total = if (isX) {
                realX - sideSize
            } else {
                realY - sideSize
            }
            return when (side) {
                0 -> if (isX) 0f else appContext.statusBarHeight().toFloat()
                1 -> if (isX) total else total - appContext.navigationBarHeight()
                else -> round(total * p)
            }
        }
    }

    private var windowView: FrameLayout? = null
    private lateinit var windowLayoutParams: WindowManager.LayoutParams

    private var timeView: TextView? = null

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show(
        connectedTime: Long? = null,
        callState: CallStateLiveData
    ) {
        windowView?.let { windowManager.removeView(it) }

        val isLandscape = appContext.isLandscape()
        val realSize = appContext.realSize()
        val realX = if (isLandscape) realSize.y else realSize.x
        val realY = if (isLandscape) realSize.x else realSize.y
        windowView = object : FrameLayout(appContext) {
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
                    windowView?.let { windowManager.updateViewLayout(it, windowLayoutParams) }
                    startX = x
                    startY = y
                } else if (event.action == MotionEvent.ACTION_UP) {
                    animateToBoundsMaybe()
                }
                return true
            }
        }

        val size = SIZE.dp
        val view = LayoutInflater.from(appContext).inflate(R.layout.view_pip_call, null)
        windowView?.addView(view, FrameLayout.LayoutParams(size, size, Gravity.START or Gravity.TOP))
        view.setOnClickListener {
            CallActivity.show(appContext)
        }
        timeView = view.findViewById(R.id.time_tv)
        if (appContext.isNightMode()) {
            view.setBackgroundResource(R.drawable.bg_pip_call_dark)
            timeView?.setTextColor(ContextCompat.getColor(appContext, R.color.white))
        } else {
            view.setBackgroundResource(R.drawable.bg_pip_call)
            timeView?.setTextColor(ContextCompat.getColor(appContext, R.color.call_light_green))
        }

        val sp = appContext.defaultSharedPreferences
        val sideX = sp.getInt(CALL_SIDE_X, 1)
        val sideY = sp.getInt(CALL_SIDE_Y, 0)
        val px = sp.getFloat(CALL_PX, 0f)
        val py = sp.getFloat(CALL_PY, 0f)
        try {
            windowLayoutParams = WindowManager.LayoutParams().apply {
                width = size
                height = size
                x = getSideCoordinate(true, sideX, px, size.toFloat(), realX, realY).toInt()
                y = getSideCoordinate(false, sideY, py, size.toFloat(), realX, realY).toInt()
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                type = if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            windowView?.let { windowManager.addView(it, windowLayoutParams) }
            shown = true
        } catch (e: Exception) {
            Timber.e(e)
        }

        if (callState.isNotIdle()) {
            if (connectedTime != null) {
                startTimer(connectedTime)
            } else {
                timeView?.text = appContext.getString(R.string.Waiting)
            }
        } else {
            close()
        }
    }

    var shown = false

    fun close() {
        Timber.d("$TAG_CALL$shown")
        shown = false
        if (windowView != null) {
            try {
                windowManager.removeView(windowView)
            } catch (e: Exception) {
                Timber.w("$TAG_CALL remove windowView throw $e")
            }
        }
        timeView = null
        windowView = null
        stopTimer()
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    fun startTimer(connectedTime: Long) {
        Timber.d("$TAG_CALL startTimer timer: $timer")
        if (timer != null) {
            setDuration(connectedTime)
            return
        }

        timer = Timer(true)
        timerTask?.cancel()
        timerTask = object : TimerTask() {
            override fun run() {
                appContext.runOnUiThread {
                    setDuration(connectedTime)
                }
            }
        }
        timer?.schedule(timerTask, 0, 1000)
    }

    fun stopTimer() {
        Timber.d("$TAG_CALL stopTimer")
        timerTask?.cancel()
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    private fun setDuration(connectedTime: Long) {
        val duration = System.currentTimeMillis() - connectedTime
        val text = duration.formatMillis()
        timeView?.text = text
    }

    private var decelerateInterpolator: DecelerateInterpolator? = null
    @SuppressLint("Recycle")
    private fun animateToBoundsMaybe() {
        val realSize = appContext.realSize()
        val isLandscape = appContext.isLandscape()
        val realX = if (isLandscape) realSize.y else realSize.x
        val realY = if (isLandscape) realSize.x else realSize.y
        val size = SIZE.dp.toFloat()
        val startX = getSideCoordinate(true, 0, 0f, size, realX, realY).toInt()
        val endX = getSideCoordinate(true, 1, 0f, size, realX, realY).toInt()
        val startY = getSideCoordinate(false, 0, 0f, size, realX, realY).toInt()
        val endY = getSideCoordinate(false, 1, 0f, size, realX, realY).toInt()
        var animatorY: Animator? = null
        val editor = appContext.defaultSharedPreferences.edit()
        val animatorX = when {
            windowLayoutParams.x < startX || windowLayoutParams.x <= endX / 2 -> {
                editor.putInt(CALL_SIDE_X, 0)
                ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, 0)
            }
            else -> {
                editor.putInt(CALL_SIDE_X, 1)
                ObjectAnimator.ofInt(this, "x", windowLayoutParams.x, endX)
            }
        }
        when {
            windowLayoutParams.y < startY -> {
                editor.putFloat(CALL_PY, startY.toFloat() / realY)
                animatorY = ObjectAnimator.ofInt(this, "y", windowLayoutParams.y, startY)
            }
            windowLayoutParams.y > endY -> {
                editor.putFloat(CALL_PY, endY.toFloat() / realY)
                animatorY = ObjectAnimator.ofInt(this, "y", windowLayoutParams.y, endY)
            }
            else -> editor.putFloat(CALL_PY, windowLayoutParams.y.toFloat() / realY)
        }
        editor.putInt(CALL_SIDE_Y, 2)
        editor.apply()
        val animators = mutableListOf<Animator>()
        animatorX?.let { animators.add(it) }
        animatorY?.let { animators.add(it) }
        AnimatorSet().apply {
            playTogether(animators)
            if (decelerateInterpolator == null) {
                decelerateInterpolator = DecelerateInterpolator()
            }
            interpolator = decelerateInterpolator
            duration = 300
            start()
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
        windowView?.let {
            windowManager.updateViewLayout(it, windowLayoutParams)
        }
    }

    @Keep
    fun setY(value: Int) {
        windowLayoutParams.y = value
        windowView?.let {
            windowManager.updateViewLayout(it, windowLayoutParams)
        }
    }
}
