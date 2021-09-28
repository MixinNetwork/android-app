package one.mixin.android.ui.player

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.Keep
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.getPixelsInCM
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.putInt
import one.mixin.android.extension.realSize
import one.mixin.android.util.MusicPlayer
import one.mixin.android.widget.RLottieDrawable
import one.mixin.android.widget.RLottieImageView
import kotlin.math.abs

class FloatingPlayer(private var isNightMode: Boolean) {
    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }

        @SuppressLint("StaticFieldLeak")
        private var Instance: FloatingPlayer? = null

        fun getInstance(isNightMode: Boolean = false): FloatingPlayer {
            var localInstance = Instance
            if (localInstance == null) {
                synchronized(FloatingPlayer::class.java) {
                    localInstance = Instance
                    if (localInstance == null) {
                        localInstance = FloatingPlayer(isNightMode)
                        Instance = localInstance
                    }
                }
            }
            return requireNotNull(localInstance)
        }

        fun recreate(isNightMode: Boolean): FloatingPlayer {
            var localInstance = Instance
            if (localInstance != null) {
                synchronized(FloatingPlayer::class.java) {
                    localInstance = Instance
                    if (localInstance != null) {
                        Instance?.hide()
                        Instance = null
                        localInstance = FloatingPlayer(isNightMode)
                        Instance = localInstance
                    }
                }
            } else {
                localInstance = getInstance(isNightMode)
            }
            return requireNotNull(localInstance)
        }

        private const val FX = "floating_player_x"
        private const val FY = "floating_player_y"
    }

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var rLottieDrawable: RLottieDrawable? = null

    var conversationId: String? = null

    private lateinit var windowView: ViewGroup
    private lateinit var musicView: RLottieImageView
    private lateinit var musicBgView: View
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private val preferences by lazy {
        appContext.defaultSharedPreferences
    }
    private var isShown = false
    fun init() {
        if (!::windowView.isInitialized) {
            initWindowView()
        }
        if (!::windowLayoutParams.isInitialized) {
            initWindowLayoutParams()
        }
    }

    fun show(force: Boolean = true, conversationId: String? = null) {
        if (!appContext.checkInlinePermissions()) return

        if (conversationId != null && this.conversationId != conversationId) {
            this.conversationId = conversationId
        }

        if (isNightMode != appContext.isNightMode()) {
            recreate(appContext.isNightMode()).show(true, this.conversationId)
        } else {
            if (!isShown) {
                init()
                isShown = true
                windowManager.addView(windowView, windowLayoutParams)
            }
            if (force) {
                reload()
            }
        }
    }

    fun reload() {
        animateToBoundsMaybe()
    }

    private fun initWindowView() {
        val realSize = appContext.realSize()
        val realX = realSize.x
        val realY = realSize.y
        windowView = object : FrameLayout(appContext) {
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
                        conversationId?.let { MusicActivity.show(appContext, it) }
                    } else {
                        animateToBoundsMaybe()
                    }
                }
                return true
            }
        }

        musicView = RLottieImageView(appContext)
        musicBgView = View(appContext).apply {
            setBackgroundResource(R.drawable.bg_music)
        }

        windowView.addView(
            FrameLayout(appContext).apply {
                if (isNightMode) {
                    setBackgroundResource(R.drawable.bg_floating_shadow_night)
                } else {
                    setBackgroundResource(R.drawable.bg_floating_shadow)
                }
                addView(musicBgView, FrameLayout.LayoutParams(48.dp, 48.dp).apply { gravity = Gravity.CENTER })
                addView(musicView, FrameLayout.LayoutParams(30.dp, 30.dp).apply { gravity = Gravity.CENTER })
            },
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )

        musicView.setAutoRepeat(true)
        rLottieDrawable = if (isNightMode) {
            RLottieDrawable(R.raw.anim_music_night, "music_night", 30.dp, 30.dp)
        } else {
            RLottieDrawable(R.raw.anim_music, "music", 30.dp, 30.dp)
        }
        musicView.setAnimation(rLottieDrawable)
        if (MusicPlayer.get().exoPlayer.isPlaying) {
            musicView.playAnimation()
        }
    }

    fun hide() {
        if (!isShown) return
        isShown = false
        windowManager.removeView(windowView)
    }

    fun stopAnim() {
        if (::musicView.isInitialized) {
            musicView.stopAnimation()
        }
    }

    fun startAnim() {
        if (::musicView.isInitialized) {
            musicView.playAnimation()
        }
    }

    private fun initWindowLayoutParams() {
        windowLayoutParams = WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        windowLayoutParams.width = 64.dp
        windowLayoutParams.height = 64.dp
        windowLayoutParams.x = preferences.getInt(FX, appContext.realSize().x - 48.dp)
        windowLayoutParams.y = preferences.getInt(FY, 120.dp)
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
        if (!isShown) return
        windowManager.updateViewLayout(windowView, windowLayoutParams)
    }

    @Keep
    fun setY(value: Int) {
        windowLayoutParams.y = value
        if (!isShown) return
        windowManager.updateViewLayout(windowView, windowLayoutParams)
    }
}

fun collapse(conversationId: String? = null) {
    FloatingPlayer.getInstance().show(conversationId = conversationId)
}
