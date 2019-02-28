package one.mixin.android.widget

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
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
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.view_chat_control.view.*
import one.mixin.android.R
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.widget.audio.SlidePanelView
import one.mixin.android.widget.keyboard.InputAwareLayout
import org.jetbrains.anko.dip

@SuppressLint("CheckResult")
class ChatControlView : FrameLayout {

    companion object {
        const val REPLY = -1
        const val SEND = 0
        const val AUDIO = 1

        const val STICKER = 0
        const val KEYBOARD = 1

        const val RECORD_DELAY = 200L
        const val RECORD_TIP_MILLIS = 2000L
    }

    lateinit var callback: Callback
    lateinit var inputLayout: InputAwareLayout
    lateinit var stickerContainer: StickerLayout
    lateinit var menuContainer: MenuLayout
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

    var isRecording = false

    var activity: Activity? = null
    private lateinit var recordCircle: RecordCircleView
    lateinit var cover: View
    private var upBeforeGrant = false
    private var keyboardShown = false

    private val sendDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_send, null) }
    private val sendCheckedDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_send_checked, null) }
    private val audioDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_mic, null) }
    private val audioActiveDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_mic_checked, null) }

    private val stickerDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_sticker, null) }
    private val keyboardDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_keyboard, null) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_chat_control, this, true)

        chat_et.addTextChangedListener(editTextWatcher)
        chat_send_ib.setOnTouchListener(sendOnTouchListener)
        chat_more_iv.setOnClickListener(onChatMoreClickListener)
        chat_sticker_ib.setOnClickListener(onStickerClickListener)
        chat_img_iv.setOnClickListener(onChatImgClickListener)
        chat_bot_iv.setOnClickListener(onChatBotClickListener)
        chat_slide.callback = chatSlideCallback
    }

    fun setCircle(record_circle: RecordCircleView) {
        recordCircle = record_circle
        recordCircle.callback = recordCircleCallback
    }

    fun setSend() {
        if (sendStatus == REPLY) {
            return
        }
        if (!post(safeSetSendRunnable)) {
            realSetSend()
        }
    }

    fun reset() {
        stickerStatus = STICKER
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
        setSend()
    }

    fun hideOtherInput() {
        if (!botHide) {
            chat_bot_iv.isGone = true
        }
        chat_sticker_ib.isGone = true
        chat_more_iv.isGone = true
        sendStatus = REPLY
    }

    fun showOtherInput() {
        if (!botHide) {
            chat_bot_iv.isVisible = true
        }
        checkSticker()
        chat_more_iv.isVisible = true
        if (sendStatus == REPLY && chat_et.text.toString().trim().isNotEmpty()) {
            return
        }
        sendStatus = lastSendStatus
    }

    private var botHide = false

    fun hideBot() {
        botHide = true
        chat_bot_iv.visibility = View.GONE
        initTransitions()
    }

    fun showBot() {
        botHide = false
        chat_bot_iv.visibility = View.VISIBLE
        initTransitions()
    }

    fun toggleKeyboard(shown: Boolean) {
        keyboardShown = shown
        if (shown) {
            cover.alpha = 0f
            activity?.window?.statusBarColor = Color.TRANSPARENT
            stickerStatus = STICKER
        } else {
            if (inputLayout.isInputOpen) {
                stickerStatus = KEYBOARD
            }
        }
        setSend()
    }

    fun uncheckBot() {
        chat_img_iv.isChecked = false
    }

    private fun initTransitions() {
        post {
            bottom_ll.layoutTransition = createTransitions()
            edit_ll.layoutTransition = createEditTransitions()
        }
    }

    private fun checkSend() {
        val d = when (sendStatus) {
            REPLY -> sendDrawable
            SEND -> if (isEditEmpty()) sendDrawable else sendCheckedDrawable
            AUDIO -> if (isRecording) audioActiveDrawable else audioDrawable
            else -> throw IllegalArgumentException("error send status")
        }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        startScaleAnim(chat_send_ib, d)
    }

    private fun checkSticker() {
        val d = when (stickerStatus) {
            STICKER -> stickerDrawable
            KEYBOARD -> keyboardDrawable
            else -> null
        }
        d?.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        startScaleAnim(chat_sticker_ib, d)
    }

    private fun startScaleAnim(v: ImageButton, d: Drawable?) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)).apply {
            duration = 100
            interpolator = AccelerateInterpolator()
        }
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)).apply {
            duration = 100
            interpolator = DecelerateInterpolator()
        }
        scaleDown.doOnEnd {
            v.setImageDrawable(d)
            scaleUp.start()
        }
        scaleDown.start()
    }

    private fun cleanUp(locked: Boolean = false) {
        startX = 0f
        originX = 0f
        if (!locked) {
            isRecording = false
        }
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

    private fun currentAudio() = sendStatus == AUDIO

    @SuppressLint("ObjectAnimatorBinding")
    private fun createTransitions(): LayoutTransition {
        val scaleDownTransX = chat_send_ib.width
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat())).apply {
            duration = 100
            interpolator = DecelerateInterpolator()
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f),
            PropertyValuesHolder.ofFloat("translationX", 0f)).apply {
            duration = 100
            interpolator = AccelerateInterpolator()
        }

        return getLayoutTransition(scaleUp, scaleDown)
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun createEditTransitions(): LayoutTransition {
        val scaleDownTransX = right - chat_more_iv.width - chat_send_ib.width - edit_ll.width
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat())).apply {
            duration = 100
            interpolator = DecelerateInterpolator()
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f),
            PropertyValuesHolder.ofFloat("translationX", 0f)).apply {
            duration = 100
            interpolator = DecelerateInterpolator()
        }

        return getLayoutTransition(scaleUp, scaleDown)
    }

    private fun getLayoutTransition(scaleUp: ObjectAnimator, scaleDown: ObjectAnimator): LayoutTransition {
        val layoutTransition = LayoutTransition()
        layoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp)
        layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, scaleDown)
        layoutTransition.setStartDelay(LayoutTransition.APPEARING, 0)
        layoutTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0)
        layoutTransition.setDuration(LayoutTransition.CHANGE_APPEARING, 150)
        layoutTransition.setDuration(LayoutTransition.CHANGE_DISAPPEARING, 150)
        layoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
        layoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
        return layoutTransition
    }

    private fun clickSend() {
        when (sendStatus) {
            SEND, REPLY -> {
                chat_et.text?.let {
                    callback.onSendClick(it.trim().toString())
                }
            }
            AUDIO -> {
                if (recordTipView.visibility == View.INVISIBLE) {
                    recordTipView.fadeIn()
                    postDelayed(hideRecordTipRunnable, RECORD_TIP_MILLIS)
                } else {
                    removeCallbacks(hideRecordTipRunnable)
                }
                postDelayed(hideRecordTipRunnable, RECORD_TIP_MILLIS)
            }
        }
    }

    private fun isEditEmpty() = chat_et.text.toString().trim().isEmpty()

    private fun realSetSend() {
        sendStatus = if (!isEditEmpty()) {
            if (!chat_sticker_ib.isGone) {
                chat_sticker_ib.isGone = true
            }
            if (!botHide) {
                if (!chat_bot_iv.isGone) {
                    chat_bot_iv.isGone = true
                }
            }
            if (!chat_img_iv.isGone) {
                chat_img_iv.isGone = true
            }
            SEND
        } else {
            if (!chat_sticker_ib.isVisible) {
                chat_sticker_ib.isVisible = true
            }
            if (!botHide) {
                if (!chat_bot_iv.isVisible) {
                    chat_bot_iv.isVisible = true
                }
            }
            if (!chat_img_iv.isVisible) {
                chat_img_iv.isVisible = true
            }
            lastSendStatus
        }
    }

    private fun onStickerClick() {
        if (stickerStatus == KEYBOARD) {
            stickerStatus = STICKER
            inputLayout.showSoftKey(chat_et)
        } else {
            stickerStatus = KEYBOARD
            inputLayout.show(chat_et, stickerContainer)
            callback.onStickerClick()

            if (stickerStatus == KEYBOARD && inputLayout.isInputOpen &&
                sendStatus == AUDIO && lastSendStatus == AUDIO) {
                setSend()
            }
        }
    }

    private val onChatMoreClickListener = OnClickListener {
        if (chat_more_iv.isChecked) {
            inputLayout.show(chat_et, menuContainer)
            callback.onMenuClick()
        } else {
            if (stickerStatus == STICKER) {
                inputLayout.showSoftKey(chat_et)
            } else {
                inputLayout.show(chat_et, stickerContainer)
            }
        }
    }

    private val onStickerClickListener = OnClickListener {
        onStickerClick()
    }

    private val onChatBotClickListener = OnClickListener {
        callback.onBotClick()
    }

    private val onChatImgClickListener = OnClickListener {
        callback.onImageClick()
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
    private var hasStartRecord = false
    private var locked = false
    private var maxScrollX = context.dip(100f)
    var calling = false

    private val sendOnTouchListener = OnTouchListener { _, event ->
        if (calling && sendStatus == AUDIO) {
            callback.onCalling()
            return@OnTouchListener false
        }
        chat_send_ib.onTouchEvent(event)
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
                hasStartRecord = false
                locked = false
                if (currentAudio()) {
                    postDelayed(recordRunnable, RECORD_DELAY)
                }
                return@OnTouchListener true
            }
            ACTION_MOVE -> {
                if (!currentAudio() || recordCircle.sendButtonVisible || !hasStartRecord) return@OnTouchListener false

                val x = recordCircle.setLockTranslation(event.y)
                if (x == 2) {
                    ObjectAnimator.ofFloat(recordCircle, "lockAnimatedTranslation",
                        recordCircle.startTranslation).apply {
                        duration = 150
                        interpolator = DecelerateInterpolator()
                        doOnEnd { locked = true }
                    }.start()
                    chat_slide.toCancel()
                    return@OnTouchListener false
                }

                val moveX = event.rawX
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
                    cleanUp()
                    triggeredCancel = false
                    return@OnTouchListener false
                }

                if (!hasStartRecord) {
                    removeCallbacks(recordRunnable)
                    removeCallbacks(checkReadyRunnable)
                    cleanUp()
                    if (!post(sendClickRunnable)) {
                        clickSend()
                    }
                } else if (hasStartRecord && !locked && System.currentTimeMillis() - startTime < 500) {
                    removeCallbacks(recordRunnable)
                    removeCallbacks(checkReadyRunnable)
                    // delay check sendButtonVisible
                    postDelayed({
                        if (!recordCircle.sendButtonVisible) {
                            handleCancelOrEnd(true)
                        } else {
                            recordCircle.sendButtonVisible = false
                        }
                    }, 200)
                    return@OnTouchListener false
                }

                if (isRecording && !recordCircle.sendButtonVisible) {
                    handleCancelOrEnd(event.action == ACTION_CANCEL)
                } else {
                    cleanUp(true)
                }

                if (!callback.isReady()) {
                    upBeforeGrant = true
                }
            }
        }
        return@OnTouchListener true
    }

    private val safeSetSendRunnable = Runnable { realSetSend() }

    private val sendClickRunnable = Runnable { clickSend() }

    private val hideRecordTipRunnable = Runnable {
        if (recordTipView.visibility == View.VISIBLE) {
            recordTipView.fadeOut()
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            hasStartRecord = true
            removeCallbacks(hideRecordTipRunnable)
            post(hideRecordTipRunnable)

            if (activity == null || !currentAudio()) return@Runnable

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
        fun onCalling()
        fun onMenuClick()
        fun onBotClick()
        fun onImageClick()
    }
}