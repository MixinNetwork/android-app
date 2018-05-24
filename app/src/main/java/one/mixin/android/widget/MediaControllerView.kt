package one.mixin.android.widget

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.content.systemService
import kotlinx.android.synthetic.main.layout_media_controller.view.*
import one.mixin.android.R
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.widget.PlayView.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView.Companion.STATUS_PAUSING
import one.mixin.android.widget.PlayView.Companion.STATUS_PLAYING

class MediaControllerView : FrameLayout {

    lateinit var callback: MediaControllerCallback

    private var isShowing = true
    private var dragging = false
    private var reload = false

    private val animInTop: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.slide_in_top) }
    private val animInBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom) }
    private val animOutTop: Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.slide_out_top).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {
                    close_iv.visibility = VISIBLE
                    share_iv.visibility = VISIBLE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    close_iv.visibility = GONE
                    share_iv.visibility = GONE
                }
            })
        }
    }
    private val animOutBottom: Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {
                    controller.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    controller.visibility = View.INVISIBLE
                    removeCallbacks(hideSystemUI)
                    if (isShowing) {
                        postDelayed(hideSystemUI, DEFAULT_TIME_OUT)
                    }
                }
            })
        }
    }

    private val am: AudioManager by lazy { context.systemService<AudioManager>() }
    private var volume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
    private val maxVolume: Int = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.layout_media_controller, this, true)
        play_view.setOnTouchListener(playViewTouchListener)
        seek_bar.setOnSeekBarChangeListener(seekBarListener)

        val statusBarHeight = context.statusBarHeight().toFloat()
        close_iv.translationY = statusBarHeight
        share_iv.translationY = statusBarHeight
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_MUTE -> return super.dispatchKeyEvent(event)
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val step = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
                    setVolume(volume + step)
                    removeCallbacks(hideVolume)
                    postDelayed(hideVolume, 500)
                    return true
                }
            }
            if (event.repeatCount == 0 && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
                doPauseResume()
                show()
            } else if (event.action == KeyEvent.KEYCODE_MEDIA_STOP) {
                if (callback.isPlaying()) {
                    callback.pause()
                    play_view.status = PlayView.STATUS_PAUSING
                }
            } else if (event.action == KeyEvent.KEYCODE_BACK) {
                callback.stop()
            } else {
                show()
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (callback.isVideo()) {
                if (isShowing) {
                    hide()
                } else {
                    show(DEFAULT_TIME_OUT + 500, 500)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // Called outside
    fun start() {
        callback.start(reload)
        play_view.status = STATUS_PLAYING
        hide(true)
    }

    // Called outside
    fun stop() {
        reload = true
        removeCallbacks(showRunnable)
        callback.stop()
        play_view.status = STATUS_IDLE
        hide(!callback.isVideo())
    }

    // Called outside
    fun hide() {
        removeCallbacks(showRunnable)
        hide(true)
    }

    private fun doPauseResume() {
        if (callback.isPlaying()) {
            callback.pause()
            play_view.status = STATUS_PAUSING
        } else {
            callback.start(false)
            play_view.status = STATUS_PLAYING
        }
    }

    private fun show(timeout: Long = DEFAULT_TIME_OUT, delay: Long = 0) {
        if (timeout > 0L) {
            removeCallbacks(fadeOut)
            postDelayed(fadeOut, timeout)
        }
        if (delay == 0L) {
            post(showRunnable)
        } else {
            postDelayed(showRunnable, delay)
        }
    }

    private fun hide(hidePlay: Boolean) {
        if (isShowing) {
            isShowing = false

            share_iv.startAnimation(animOutTop)
            close_iv.startAnimation(animOutTop)
            if (hidePlay && play_view.visibility == View.VISIBLE) {
                play_view.fadeOut()
            }
            controller.startAnimation(animOutBottom)
            removeCallbacks(showProgress)
        } else {
            if (hidePlay && play_view.visibility == View.VISIBLE) {
                play_view.fadeOut()
            } else if (!hidePlay && play_view.visibility == View.INVISIBLE) {
                play_view.fadeIn()
            }
        }
    }

    private fun setVolume(v: Int) {
        val vo = when {
            v > maxVolume -> maxVolume
            v < 0 -> 0
            else -> v
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, vo, 0)
        // TODO refresh UI
    }

    private fun showSystemUi(visible: Boolean) {
        val flag = if (visible)
            0
        else
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LOW_PROFILE
        systemUiVisibility = flag
    }

    private fun setProgress(): Int {
        if (dragging) return 0

        val pos = callback.getCurrentPosition()
        val duration = callback.getDuration()
//        play_view.status = STATUS_BUFFERING
        cur_tv.text = pos.toLong().formatMillis()
        duration_tv.text = duration.toLong().formatMillis()
        if (duration > 0) {
            seek_bar.progress = pos * 200 / duration
        }
        return pos
    }

    private val playViewTouchListener = OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            play_view.requestFocus()
            when (play_view.status) {
                PlayView.STATUS_IDLE -> {
                    callback.start(reload)
                    play_view.status = STATUS_PLAYING
                    hide(true)
                }
                PlayView.STATUS_LOADING, STATUS_PLAYING, PlayView.STATUS_BUFFERING -> {
                    callback.pause()
                    play_view.status = STATUS_PAUSING
                }
                STATUS_PAUSING -> {
                    callback.start(false)
                    play_view.status = STATUS_PLAYING
                    hide(true)
                }
            }
        }
        return@OnTouchListener true
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            dragging = true
            show(3600000)
            removeCallbacks(showProgress)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            dragging = false
            setProgress()
            show(DEFAULT_TIME_OUT)
            post(showProgress)
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser) return

            val newPos = (callback.getDuration() * progress) / 1000L
            callback.seekTo(newPos)
            cur_tv.text = newPos.formatMillis()
        }
    }

    private val showProgress: Runnable by lazy {
        Runnable {
            val pos = setProgress()
            if (!dragging && isShowing && callback.isPlaying()) {
                postDelayed(showProgress, 1000L - (pos % 1000))
            }
        }
    }

    private val showRunnable = Runnable {
        if (!isShowing) {
            isShowing = true
            setProgress()
            removeCallbacks(hideSystemUI)
            showSystemUi(true)
            share_iv.startAnimation(animInTop)
            close_iv.startAnimation(animInTop)
            if (play_view.visibility == INVISIBLE) {
                play_view.fadeIn()
            }
            controller.startAnimation(animInBottom)
            controller.visibility = VISIBLE
            close_iv.visibility = VISIBLE
            share_iv.visibility = VISIBLE
        }
        post(showProgress)
    }

    private val fadeOut = Runnable { hide(callback.isPlaying()) }

    private val hideSystemUI = Runnable { showSystemUi(false) }

    private val hideVolume = Runnable { }

    companion object {
        const val DEFAULT_TIME_OUT = 3000L
    }

    interface MediaControllerCallback {
        fun start(reload: Boolean)
        fun pause()
        fun stop()
        fun seekTo(pos: Long)
        fun isPlaying(): Boolean
        fun getCurrentPosition(): Int
        fun getDuration(): Int
        fun isVideo(): Boolean
    }
}