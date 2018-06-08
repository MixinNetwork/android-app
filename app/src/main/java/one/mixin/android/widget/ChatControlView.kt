package one.mixin.android.widget

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.view_chat_control.view.*
import one.mixin.android.R
import one.mixin.android.widget.audio.SlidePanelView
import one.mixin.android.widget.keyboard.InputAwareLayout
import org.jetbrains.anko.dip
import kotlin.math.abs

class ChatControlView : FrameLayout {

    companion object {
        const val SEND = 0
        const val AUDIO = 1
        const val VIDEO = 2
        const val UP = 3
        const val DOWN = 4

        const val NONE = 0
        const val STICKER = 1
        const val KEYBOARD = 2

        const val SEND_CLICK_DELAY = 300L
        const val RECORD_DELAY = 200L
    }

    lateinit var callback: Callback
    lateinit var inputLayout: InputAwareLayout
    lateinit var stickerContainer: StickerLayout

    var sendStatus = AUDIO
        set(value) {
            if (value == field) return

            field = value
            checkSend()
        }
    var isUp = true
    private var stickerStatus = STICKER
        set(value) {
            if (value == field) return

            field = value
            checkSticker()
        }

    private var lastSendStatus = AUDIO

    private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var isRecording = false

    var activity: Activity? = null

    private val sendDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_send, null) }
    private val audioDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_black, null) }
    private val audioActiveDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_blue, null) }
    private val videoDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_record_mic_black, null) }
    private val upDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_arrow_up, null) }
    private val downDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_arrow_down, null) }

    private val stickerDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_sticker, null) }
    private val keyboardDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_keyboard, null) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_chat_control, this, true)

        chat_et.setOnTouchListener(editTouchListener)
        chat_et.addTextChangedListener(editTextWatcher)
        chat_send_ib.setOnTouchListener(sendOnTouchListener)
        chat_slide.callback = chatSlideCallback
    }

    lateinit var record_circle: RecordCircleView

    fun setCircle(record_circle: RecordCircleView) {
        this.record_circle = record_circle
        record_circle.callback = recordCircleCallback
    }

    fun setSendWithSticker() {
        sendStatus = if (stickerStatus == NONE) {
            SEND
        } else if (stickerStatus == KEYBOARD) {
            if (isUp) UP else DOWN
        } else {
            lastSendStatus
        }
    }

    fun reset() {
        stickerStatus = STICKER
        isUp = true
        setSendWithSticker()
        inputLayout.hideCurrentInput(chat_et)
    }

    fun cancelExternal() {
        removeCallbacks(recordRunnable)
        chat_slide.onEnd()
        cleanUp()
        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
    }

    private fun checkSend() {
        val d = when (sendStatus) {
            SEND -> sendDrawable
            AUDIO -> if (isRecording) audioActiveDrawable else audioDrawable
            VIDEO -> videoDrawable
            UP -> upDrawable
            DOWN -> downDrawable
            else -> throw IllegalArgumentException("error send status")
        }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        chat_send_ib.setImageDrawable(d)
    }

    private fun checkSticker() {
        val d = when (stickerStatus) {
            STICKER -> stickerDrawable
            KEYBOARD -> keyboardDrawable
            else -> null
        }
        d?.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        chat_et.setCompoundDrawables(null, null, d, null)
    }

    private fun cleanUp() {
        startX = 0f
        originX = 0f
        isRecording = false
        checkSend()
    }

    private fun handleCancelOrEnd(cancel: Boolean) {
        if (cancel) callback.onRecordCancel() else callback.onRecordEnd()
        chat_slide.onEnd()
        cleanUp()
        updateRecordCircleAndSendIcon()
    }

    private fun updateRecordCircleAndSendIcon() {
        if (isRecording) {
            record_circle.visibility = View.VISIBLE
            record_circle.setAmplitude(.0)
            record_circle.animate().apply {
                record_circle.scale = 1f
                interpolator = DecelerateInterpolator()
            }.start()
            chat_send_ib.animate().alpha(0f).start()
        } else {
            record_circle.animate().apply {
                record_circle.scale = 0f
                interpolator = AccelerateInterpolator()
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        setListener(null)
                        record_circle.visibility = View.GONE
                        record_circle.setSendButtonInvisible()
                    }
                })
            }.start()
            chat_send_ib.animate().alpha(1f).start()
        }
    }

    private fun audioOrVideo() = sendStatus == AUDIO || sendStatus == VIDEO

    private val editTouchListener = OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            v as TextView
            val endCompound = v.compoundDrawables[2] ?: return@OnTouchListener false
            if (event.rawX >= v.right - endCompound.bounds.width() - context.dip(10)) {
                stickerStatus = if (inputLayout.currentInput == stickerContainer) {
                    inputLayout.showSoftKey(chat_et)
                    STICKER
                } else {
                    inputLayout.show(chat_et, stickerContainer)
                    chat_et.clearFocus()
                    callback.onStickerClick()
                    KEYBOARD
                }
                setSendWithSticker()
            } else {
                inputLayout.showSoftKey(chat_et)
                stickerStatus = STICKER
                setSendWithSticker()
            }
        }
        return@OnTouchListener false
    }

    private val editTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val etEmpty = chat_et.text.trim().isBlank()
            stickerStatus = if (!etEmpty) {
                NONE
            } else {
                if (inputLayout.currentInput == stickerContainer) {
                    KEYBOARD
                } else {
                    STICKER
                }
            }
            setSendWithSticker()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private var startX = 0f
    private var originX = 0f
    private var startTime = 0L
    private var triggeredCancel = false
    private var maxScrollX = context.dip(150f)

    private val sendOnTouchListener = OnTouchListener { _, event ->
        when (event.action) {
            ACTION_DOWN -> {
                if (record_circle.sendButtonVisible) {
                    return@OnTouchListener false
                }

                originX = event.rawX
                startX = event.rawX
                val w = chat_slide.slideWidth
                if (w > 0) {
                    maxScrollX = w
                }
                startTime = System.currentTimeMillis()
                if (audioOrVideo()) {
                    postDelayed(recordRunnable, RECORD_DELAY)
                }
                postDelayed(sendClickRunnable, SEND_CLICK_DELAY)
                return@OnTouchListener true
            }
            ACTION_MOVE -> {
                if (!audioOrVideo() || record_circle.sendButtonVisible) return@OnTouchListener false

                val x = record_circle.setLockTranslation(event.y)
                if (x == 2) {
                    record_circle.animate().apply {
                        record_circle.lockAnimatedTranslation = record_circle.startTranslation
                        duration = 150
                        interpolator = DecelerateInterpolator()
                    }.start()
                    chat_slide.toCancel()
                    return@OnTouchListener false
                }

                val moveX = event.rawX
                if (abs(moveX - startX) > touchSlop) {
                    removeCallbacks(sendClickRunnable)
                }
                if (moveX != 0f) {
                    chat_slide.slideText(startX - moveX)
                    if (originX - moveX > maxScrollX) {
                        removeCallbacks(recordRunnable)
                        handleCancelOrEnd(true)
                        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
                        triggeredCancel = true
                        return@OnTouchListener false
                    }
                }
                startX = moveX
            }
            ACTION_UP, ACTION_CANCEL -> {
                if (triggeredCancel) {
                    triggeredCancel = false
                    return@OnTouchListener false
                }
                if (!audioOrVideo() || record_circle.sendButtonVisible) return@OnTouchListener false

                cleanUp()
                if (System.currentTimeMillis() - startTime >= SEND_CLICK_DELAY) {
                    removeCallbacks(sendClickRunnable)
                    if (event.action == ACTION_UP) callback.onRecordEnd() else callback.onRecordCancel()
                    chat_slide.onEnd()
                    updateRecordCircleAndSendIcon()
                } else {
                    removeCallbacks(recordRunnable)
                }
            }
        }
        return@OnTouchListener true
    }

    private val sendClickRunnable = Runnable {
        removeCallbacks(recordRunnable)
        when (sendStatus) {
            SEND -> {
                val t = chat_et.text.trim().toString()
                callback.onSendClick(t)
                sendStatus = lastSendStatus
            }
            AUDIO -> {
            }
            VIDEO -> {
                sendStatus = AUDIO
                lastSendStatus = sendStatus
            }
            UP -> {
                callback.onUpClick()
            }
            DOWN -> {
                callback.onDownClick()
            }
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            removeCallbacks(sendClickRunnable)

            if (activity == null || !audioOrVideo()) return@Runnable

            if (sendStatus == AUDIO) {
                if (!RxPermissions(activity!!).isGranted(Manifest.permission.RECORD_AUDIO)) {
                    RxPermissions(activity!!).request(Manifest.permission.RECORD_AUDIO)
                        .subscribe({}, { Bugsnag.notify(it) })
                    return@Runnable
                }
            } else {
                if (RxPermissions(activity!!).isGranted(Manifest.permission.RECORD_AUDIO) &&
                    RxPermissions(activity!!).isGranted(Manifest.permission.CAMERA)) {
                    RxPermissions(activity!!).request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        .subscribe({}, { Bugsnag.notify(it) })
                    return@Runnable
                }
            }
            callback.onRecordStart(sendStatus == AUDIO)
            post(checkReadyRunnable)
            chat_send_ib.parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    private val checkReadyRunnable: Runnable by lazy {
        Runnable {
            if (callback.isReady()) {
                isRecording = true
                checkSend()
                chat_slide.onStart()
                updateRecordCircleAndSendIcon()
                record_circle.setLockTranslation(10000f)
            } else {
                postDelayed(checkReadyRunnable, 100)
            }
        }
    }

    private val chatSlideCallback = object : SlidePanelView.Callback {
        override fun onTimeout() {
            handleCancelOrEnd(false)
        }

        override fun onCancel() {
            handleCancelOrEnd(true)
        }
    }

    private val recordCircleCallback = object : RecordCircleView.Callback {
        override fun onSend() {
            handleCancelOrEnd(false)
        }
    }

    interface Callback {
        fun onStickerClick()
        fun onSendClick(text: String)
        fun onUpClick()
        fun onDownClick()
        fun onRecordStart(audio: Boolean)
        fun isReady(): Boolean
        fun onRecordEnd()
        fun onRecordCancel()
    }
}