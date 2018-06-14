package one.mixin.android.widget

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.addListener
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.view_chat_control.view.*
import one.mixin.android.R
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
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

        const val STICKER = 0
        const val KEYBOARD = 1

        const val SEND_CLICK_DELAY = 200L
        const val RECORD_DELAY = 100L
    }

    lateinit var callback: Callback
    lateinit var inputLayout: InputAwareLayout
    lateinit var stickerContainer: StickerLayout
    lateinit var recordTipView: View

    private var sendStatus = AUDIO
        set(value) {
            if (value == field) return

            field = value
            checkSend()
        }
    private var stickerStatus = STICKER
        set(value) {
            if (value == field) return

            field = value
            checkSticker()
        }

    private var lastSendStatus = AUDIO
    private var isUp = true

    private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    var isRecording = false

    var activity: Activity? = null
    lateinit var recordCircle: RecordCircleView
    lateinit var cover: View
    private var upBeforeGrant = false
    private var keyboardShown = false
    private var stickerShown = false

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

        chat_et.addTextChangedListener(editTextWatcher)
        chat_send_ib.setOnTouchListener(sendOnTouchListener)
        chat_sticker_ib.setOnClickListener(stickerClickListener)
        chat_slide.callback = chatSlideCallback
    }

    fun setCircle(record_circle: RecordCircleView) {
        recordCircle = record_circle
        recordCircle.callback = recordCircleCallback
    }

    fun setSend() {
        val editEmpty = chat_et.text.toString().isBlank() && chat_et.lineCount <= 1
        sendStatus = if (!editEmpty) {
            if (chat_more_ib.visibility != View.GONE) {
                chat_more_ib.visibility = View.GONE
            }
            SEND
        } else {
            if (chat_more_ib.visibility != View.VISIBLE) {
                chat_more_ib.visibility = View.VISIBLE
            }
            if (!keyboardShown) {
                if (stickerShown) {
                    if (isUp) UP else DOWN
                } else {
                    lastSendStatus
                }
            } else {
                lastSendStatus
            }
        }
    }

    fun reset() {
        stickerStatus = STICKER
        isUp = true
        stickerShown = false
        setSend()
        inputLayout.hideCurrentInput(chat_et)
    }

    fun cancelExternal() {
        removeCallbacks(recordRunnable)
        cleanUp()
        updateRecordCircleAndSendIcon()
        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
    }

    fun updateUp(up: Boolean) {
        isUp = up
        setSend()
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
        chat_sticker_ib.setImageDrawable(d)
    }

    private fun cleanUp() {
        startX = 0f
        originX = 0f
        isUp = true
        isRecording = false
        checkSend()
    }

    private fun handleCancelOrEnd(cancel: Boolean) {
        if (cancel) callback.onRecordCancel() else callback.onRecordEnd()
        cleanUp()
        updateRecordCircleAndSendIcon()
    }

    private fun updateRecordCircleAndSendIcon() {
        if (isRecording) {
            recordCircle.visibility = View.VISIBLE
            recordCircle.setAmplitude(.0)
            ObjectAnimator.ofFloat(recordCircle, "scale", 1f).apply {
                interpolator = DecelerateInterpolator()
                duration = 200
                addListener(onEnd = {
                    recordCircle.visibility = View.VISIBLE
                }, onCancel = {
                    recordCircle.visibility = View.VISIBLE
                })
            }.start()
            chat_send_ib.animate().setDuration(200).alpha(0f).start()
            chat_slide.onStart()
        } else {
            ObjectAnimator.ofFloat(recordCircle, "scale", 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 200
                addListener(onEnd = {
                    recordCircle.visibility = View.GONE
                    recordCircle.setSendButtonInvisible()
                }, onCancel = {
                    recordCircle.visibility = View.GONE
                    recordCircle.setSendButtonInvisible()
                })
            }.start()
            chat_send_ib.animate().setDuration(200).alpha(1f).start()
            chat_slide.onEnd()
        }
    }

    private fun audioOrVideo() = sendStatus == AUDIO || sendStatus == VIDEO

    fun toggleKeyboard(shown: Boolean) {
        keyboardShown = shown
        if (shown) {
            isUp = true
            cover.alpha = 0f
            activity?.window?.statusBarColor = Color.TRANSPARENT
            if (stickerShown) {
                stickerStatus = STICKER
            }
        } else {
            if (stickerShown) {
                stickerStatus = KEYBOARD
            }
        }
        setSend()
    }

    private val stickerClickListener = OnClickListener {
        if (stickerStatus == KEYBOARD) {
            stickerShown = false
            stickerStatus = STICKER
            inputLayout.showSoftKey(chat_et)
        } else {
            stickerShown = true
            stickerStatus = KEYBOARD
            inputLayout.show(chat_et, stickerContainer)
            callback.onStickerClick()
        }
        setSend()
    }

    private val editTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            setSend()
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
                if (recordCircle.sendButtonVisible) {
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
                if (!audioOrVideo() || recordCircle.sendButtonVisible) return@OnTouchListener false

                val x = recordCircle.setLockTranslation(event.y)
                if (x == 2) {
                    recordCircle.animate().apply {
                        recordCircle.lockAnimatedTranslation = recordCircle.startTranslation
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
                if (!audioOrVideo() || recordCircle.sendButtonVisible) return@OnTouchListener false

                cleanUp()
                if (System.currentTimeMillis() - startTime >= SEND_CLICK_DELAY) {
                    removeCallbacks(sendClickRunnable)
                    if (event.action == ACTION_UP && callback.isReady()) callback.onRecordEnd() else callback.onRecordCancel()
                } else {
                    removeCallbacks(recordRunnable)
                    removeCallbacks(checkReadyRunnable)
                    callback.onRecordCancel()
                }
                updateRecordCircleAndSendIcon()

                if (!callback.isReady()) {
                    upBeforeGrant = true
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
            }
            AUDIO -> {
                if (recordTipView.visibility == View.INVISIBLE) {
                    recordTipView.fadeIn()
                    postDelayed(hideRecordTipRunnable, 3000)
                } else {
                    removeCallbacks(hideRecordTipRunnable)
                }
                postDelayed(hideRecordTipRunnable, 3000)
            }
            VIDEO -> {
                sendStatus = AUDIO
                lastSendStatus = sendStatus
            }
            UP -> {
                callback.onUp()
            }
            DOWN -> {
                callback.onDown()
            }
        }
    }

    private val hideRecordTipRunnable =  Runnable {
        if (recordTipView.visibility == View.VISIBLE) {
            recordTipView.fadeOut()
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            removeCallbacks(sendClickRunnable)
            removeCallbacks(hideRecordTipRunnable)
            post(hideRecordTipRunnable)

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
            upBeforeGrant = false
            post(checkReadyRunnable)
            chat_send_ib.parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    private val checkReadyRunnable: Runnable by lazy {
        Runnable {
            if (callback.isReady()) {
                if (upBeforeGrant) {
                    upBeforeGrant = false
                    return@Runnable
                }
                isRecording = true
                checkSend()
                updateRecordCircleAndSendIcon()
                recordCircle.setLockTranslation(10000f)
            } else {
                postDelayed(checkReadyRunnable, 50)
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

        override fun onCancel() {
            handleCancelOrEnd(true)
        }
    }

    interface Callback {
        fun onStickerClick()
        fun onSendClick(text: String)
        fun onRecordStart(audio: Boolean)
        fun isReady(): Boolean
        fun onRecordEnd()
        fun onRecordCancel()
        fun onUp()
        fun onDown()
    }
}