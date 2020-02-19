package one.mixin.android.widget

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.text.style.MetricAffectingSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.VelocityTracker
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnKeyListener
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.bugsnag.android.Bugsnag
import com.jakewharton.rxbinding3.view.clicks
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.view_chat_control.view.*
import one.mixin.android.R
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_NONE
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_UP
import one.mixin.android.widget.audio.SlidePanelView
import one.mixin.android.widget.keyboard.InputAwareLayout
import org.jetbrains.anko.dip

class ChatControlView : FrameLayout {

    companion object {
        const val REPLY = -1
        const val SEND = 0
        const val AUDIO = 1

        const val STICKER = 0
        const val KEYBOARD = 1

        const val RECORD_DELAY = 200L
        const val RECORD_TIP_MILLIS = 2000L

        const val NONE = 0
        const val MENU = 1
        const val IMAGE = 2
    }

    lateinit var callback: Callback
    lateinit var inputLayout: InputAwareLayout
    lateinit var stickerContainer: InputAwareFrameLayout
    lateinit var menuContainer: InputAwareFrameLayout
    lateinit var galleryContainer: InputAwareFrameLayout
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

    private var currentChecked = NONE
        set(value) {
            if (value == field) return

            val lastChecked = field
            field = value
            checkChecked(lastChecked)
        }

    var isRecording = false

    var activity: Activity? = null
    private lateinit var recordCircle: RecordCircleView
    private var upBeforeGrant = false
    private var keyboardShown = false

    private val sendDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_send_checked, context.theme) }
    private val audioDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_mic, context.theme) }

    private val stickerDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_sticker, context.theme) }
    private val keyboardDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_chat_keyboard, context.theme) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_chat_control, this, true)

        chat_et.addTextChangedListener(editTextWatcher)
        chat_et.setOnKeyListener(keyListener)
        chat_et.setOnClickListener(onChatEtClickListener)
        chat_send_ib.setOnTouchListener(sendOnTouchListener)
        chat_menu_iv.setOnClickListener(onChatMenuClickListener)
        chat_sticker_ib.setOnClickListener(onStickerClickListener)
        chat_img_iv.setOnClickListener(onChatImgClickListener)
        chat_bot_iv.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                callback.onBotClick()
            }
        chat_slide.callback = chatSlideCallback

        remainFocusable()
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
        currentChecked = NONE
        setSend()
        inputLayout.hideCurrentInput(chat_et)
    }

    fun cancelExternal() {
        removeCallbacks(recordRunnable)
        cleanUp()
        updateRecordCircleAndSendIcon()
        chat_slide.parent.requestDisallowInterceptTouchEvent(false)
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
            stickerStatus = STICKER
            currentChecked = NONE
        } else {
            if (inputLayout.isInputOpen) {
                stickerStatus = KEYBOARD
            }
        }
        setSend()
    }

    fun getDraggableContainer() = when {
        stickerContainer.isVisible -> stickerContainer
        galleryContainer.isVisible -> galleryContainer
        else -> null
    }

    fun getVisibleContainer() = when {
        stickerContainer.isVisible -> stickerContainer
        galleryContainer.isVisible -> galleryContainer
        menuContainer.isVisible -> menuContainer
        else -> null
    }

    // remove focus but remain focusable
    private fun remainFocusable() {
        post {
            chat_et.isFocusableInTouchMode = false
            chat_et.isFocusable = false
            chat_et.isFocusableInTouchMode = true
            chat_et.isFocusable = true
        }
    }

    private fun initTransitions() {
        post {
            bottom_ll.layoutTransition = createTransitions()
            edit_ll.layoutTransition = createEditTransitions()
        }
    }

    private fun checkSend(anim: Boolean = true) {
        val d = when (sendStatus) {
            REPLY, SEND -> sendDrawable
            AUDIO -> audioDrawable
            else -> throw IllegalArgumentException("error send status")
        }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        if (anim) {
            startScaleAnim(chat_send_ib, d)
        } else {
            chat_send_ib.setImageDrawable(d)
        }
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

    private fun checkChecked(lastChecked: Int) {
        when (currentChecked) {
            MENU -> {
                if (lastChecked != MENU) {
                    rotateChatMenu(true)
                }
                chat_img_iv.setImageResource(R.drawable.ic_chat_img)
            }
            IMAGE -> {
                if (lastChecked == MENU) {
                    rotateChatMenu(false)
                }
                chat_img_iv.setImageResource(R.drawable.ic_chat_img_checked)
            }
            else -> {
                if (lastChecked == MENU) {
                    rotateChatMenu(false)
                }
                chat_img_iv.setImageResource(R.drawable.ic_chat_img)
            }
        }
    }

    private fun startScaleAnim(v: ImageView, d: Drawable?) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat("scaleX", 0.6f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.6f, 1f)).apply {
            duration = 100
        }
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.6f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.6f)).apply {
            duration = 100
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
            recordCircle.locked = false
        }
        checkSend(false)
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
                    recordCircle.locked = false
                }, onCancel = {
                    recordCircle.visibility = View.GONE
                    recordCircle.locked = false
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
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f),
            PropertyValuesHolder.ofFloat("alpha", 1f, 0f),
            PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat())).apply {
            duration = 50
            interpolator = DecelerateInterpolator()
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f),
            PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
            PropertyValuesHolder.ofFloat("translationX", 0f)).apply {
            duration = 50
            interpolator = DecelerateInterpolator()
        }

        return getLayoutTransition(scaleUp, scaleDown)
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun createEditTransitions(): LayoutTransition {
        val scaleDownTransX = right - chat_menu_iv.width - chat_send_ib.width - edit_ll.width
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f),
            PropertyValuesHolder.ofFloat("alpha", 1f, 0f),
            PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat())).apply {
            duration = 50
            interpolator = DecelerateInterpolator()
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f),
            PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
            PropertyValuesHolder.ofFloat("translationX", 0f)).apply {
            duration = 50
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
        layoutTransition.setDuration(LayoutTransition.CHANGE_APPEARING, 100)
        layoutTransition.setDuration(LayoutTransition.CHANGE_DISAPPEARING, 100)
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

    private fun rotateChatMenu(checked: Boolean) {
        val anim = chat_menu_iv.animate().rotation(if (checked) 45f else -45f)
        anim.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                chat_menu_iv.rotation = 0f
                chat_menu_iv.setImageResource(if (checked) R.drawable.ic_chat_more_checked else R.drawable.ic_chat_more)
            }
        })
        anim.start()
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

    private val onChatMenuClickListener = OnClickListener {
        if (currentChecked != MENU) {
            currentChecked = MENU
            inputLayout.show(chat_et, menuContainer)
            callback.onMenuClick()
        } else {
            currentChecked = NONE
            inputLayout.showSoftKey(chat_et)
        }
        remainFocusable()
    }

    private val onStickerClickListener = OnClickListener {
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
            remainFocusable()
        }
        currentChecked = NONE
    }

    private val onChatImgClickListener = OnClickListener {
        RxPermissions(activity!! as FragmentActivity)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe({ granted ->
                if (granted) {
                    clickGallery()
                } else {
                    context?.openPermissionSetting()
                }
            }, {})
    }

    private fun clickGallery() {
        if (currentChecked != IMAGE) {
            currentChecked = IMAGE
            inputLayout.show(chat_et, galleryContainer)
            callback.onGalleryClick()
        } else {
            currentChecked = NONE
            stickerStatus = STICKER
            inputLayout.hideCurrentInput(chat_et)
        }
        remainFocusable()
    }

    private fun getFling(event: MotionEvent): Int {
        velocityTracker?.addMovement(event)
        velocityTracker?.computeCurrentVelocity(1000)
        val vY = velocityTracker?.yVelocity
        val vX = velocityTracker?.xVelocity
        velocityTracker?.recycle()
        velocityTracker = null
        return if (vY != null && Math.abs(vY) >= minVelocity) {
            if (vX != null && Math.abs(vX) > Math.abs(vY)) {
                FLING_NONE
            } else {
                if (startY > event.rawY) {
                    FLING_UP
                } else {
                    FLING_DOWN
                }
            }
        } else {
            FLING_NONE
        }
    }

    private val onChatEtClickListener = OnClickListener {
        currentChecked = NONE
    }

    private val keyListener = OnKeyListener { v, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            callback.onDelete()
        }
        false
    }
    private val editTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            setSend()
            s?.let { ed ->
                val toBeRemovedSpans = ed.getSpans(0, ed.length, MetricAffectingSpan::class.java)
                if (toBeRemovedSpans.isNotEmpty()) {
                    for (span in toBeRemovedSpans) {
                        ed.removeSpan(span)
                    }
                    val curString = ed.trim()
                    chat_et.setText(curString)
                    chat_et.setSelection(curString.length)
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callback.onTextChanged(s, start, before, count)
        }
    }

    private var velocityTracker: VelocityTracker? = null
    private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var downY = 0f
    private var startY = 0f
    private var dragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                getDraggableContainer() ?: return super.dispatchTouchEvent(event)
                downY = event.rawY
                startY = event.rawY

                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
            }
            ACTION_MOVE -> {
                val moveY = event.rawY
                if (downY != 0f && getDraggableContainer() != null && !isRecording) {
                    val dif = moveY - downY
                    dragging = if (!dragging) {
                        Math.abs(moveY - startY) > touchSlop
                    } else dragging
                    if (dif != 0f) {
                        triggeredCancel = true
                        removeRecordRunnable()
                    }
                    callback.onDragChatControl(dif)
                } else {
                    startY = event.rawY
                }
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                }
                velocityTracker?.addMovement(event)
                downY = moveY
            }
            ACTION_UP, ACTION_CANCEL -> {
                downY = 0f
                if (dragging) {
                    dragging = false
                    callback.onReleaseChatControl(getFling(event))
                    startY = 0f
                    return true
                }
                startY = 0f
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return if (dragging) true else super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                getDraggableContainer() ?: return false
                downY = event.rawY
                startY = event.rawY
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                return true
            }
            ACTION_MOVE -> {
                val moveY = event.rawY
                if (downY != 0f) {
                    val dif = moveY - downY
                    dragging = if (!dragging) {
                        Math.abs(dif) > touchSlop
                    } else dragging
                    if (dif != 0f) {
                        triggeredCancel = true
                        removeRecordRunnable()
                    }
                    callback.onDragChatControl(dif)
                } else {
                    startY = event.rawY
                }
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                }
                velocityTracker?.addMovement(event)
                downY = moveY
            }
            ACTION_UP, ACTION_CANCEL -> {
                startY = 0f
                downY = 0f
                dragging = false
                callback.onReleaseChatControl(getFling(event))
            }
        }
        return false
    }

    private fun removeRecordRunnable() {
        removeCallbacks(recordRunnable)
        removeCallbacks(checkReadyRunnable)
    }

    private var startX = 0f
    private var originX = 0f
    private var startTime = 0L
    private var triggeredCancel = false
    private var hasStartRecord = false
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
                if (recordCircle.locked) {
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
                if (currentAudio()) {
                    postDelayed(recordRunnable, RECORD_DELAY)
                }
                return@OnTouchListener true
            }
            ACTION_MOVE -> {
                if (!currentAudio() || recordCircle.locked || !hasStartRecord) return@OnTouchListener false

                val x = recordCircle.setLockTranslation(event.y)
                if (x == 2) {
                    ObjectAnimator.ofFloat(recordCircle, "lockAnimatedTranslation",
                        recordCircle.startTranslation).apply {
                        duration = 150
                        interpolator = DecelerateInterpolator()
                    }.start()
                    chat_slide.toCancel()
                    callback.onRecordLocked()
                    return@OnTouchListener false
                }

                val moveX = event.rawX
                if (moveX != 0f) {
                    chat_slide.slideText(startX - moveX)
                    if (originX - moveX > maxScrollX) {
                        removeRecordRunnable()
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
                    removeRecordRunnable()
                    cleanUp()
                    if (event.action != ACTION_CANCEL) {
                        if (!post(sendClickRunnable)) {
                            clickSend()
                        }
                    }
                } else if (!isRecording) {
                    removeRecordRunnable()
                    handleCancelOrEnd(true)
                } else if (!recordCircle.locked) {
                    removeRecordRunnable()
                    handleCancelOrEnd(false)
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

            if (!RxPermissions(activity!! as FragmentActivity).isGranted(Manifest.permission.RECORD_AUDIO)) {
                RxPermissions(activity!! as FragmentActivity)
                    .request(Manifest.permission.RECORD_AUDIO)
                    .autoDispose(this)
                    .subscribe({}, { Bugsnag.notify(it) })
                return@Runnable
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
        fun onGalleryClick()
        fun onDragChatControl(dis: Float)
        fun onReleaseChatControl(fling: Int)
        fun onRecordLocked()
        fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
        fun onDelete()
    }
}
