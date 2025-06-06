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
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ActionMode
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ViewChatControlBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.media.AudioEndStatus
import one.mixin.android.media.OpusAudioRecorder
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.getLanguage
import one.mixin.android.util.reportException
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.isEncrypt
import one.mixin.android.vo.isSignal
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_NONE
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_UP
import one.mixin.android.widget.audio.SlidePanelView
import one.mixin.android.widget.keyboard.KeyboardLayout
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ChatControlView : LinearLayout, ActionMode.Callback {
    companion object {
        const val REPLY = -1
        const val SEND = 0
        const val AUDIO = 1

        const val RECORD_DELAY = 200L
        const val RECORD_TIP_MILLIS = 2000L

        const val LONG_CLICK_DELAY = 400L

        const val PREVIEW = "PREVIEW"
    }

    private enum class STATUS {
        EXPANDED_KEYBOARD, // + ☺ i
        EXPANDED_MENU, // x ☺ i
        EXPANDED_STICKER, // + k i
        COLLAPSED, // + ☺ i
    }

    private enum class StickerStatus {
        STICKER,
        KEYBOARD,
    }

    private enum class MenuStatus {
        EXPANDED,
        COLLAPSED,
    }

    lateinit var callback: Callback
    lateinit var inputLayout: KeyboardLayout
    lateinit var stickerContainer: FrameLayout
    lateinit var menuContainer: FrameLayout
    lateinit var galleryContainer: FrameLayout
    lateinit var recordTipView: View

    private val _binding: ViewChatControlBinding

    private val binding get() = _binding
    val chatEt get() = binding.chatEt
    val replyView get() = binding.replyView
    val anchorView get() =
        if (replyView.isVisible) {
            replyView
        } else {
            binding.chatSendIb
        }

    private var controlState: STATUS = STATUS.COLLAPSED
        set(value) {
            if (value == field) return
            field = value

            when (value) {
                STATUS.EXPANDED_MENU -> {
                    menuStatus = MenuStatus.EXPANDED
                    stickerStatus = StickerStatus.STICKER
                    keyboardDrawable
                    binding.chatImgIv.setImageResource(R.drawable.ic_chat_img)
                    menuContainer.isVisible = true
                    stickerContainer.isVisible = false
                    galleryContainer.isVisible = false
                }
                STATUS.EXPANDED_KEYBOARD -> {
                    menuStatus = MenuStatus.COLLAPSED
                    stickerStatus = StickerStatus.STICKER
                    binding.chatImgIv.setImageResource(R.drawable.ic_chat_img)
                    menuContainer.isVisible = false
                    stickerContainer.isVisible = false
                    galleryContainer.isVisible = false
                }
                STATUS.EXPANDED_STICKER -> {
                    menuStatus = MenuStatus.COLLAPSED
                    stickerStatus = StickerStatus.KEYBOARD
                    binding.chatImgIv.setImageResource(R.drawable.ic_chat_img)
                    menuContainer.isVisible = false
                    stickerContainer.isVisible = true
                    galleryContainer.isVisible = false
                }
                STATUS.COLLAPSED -> {
                    menuStatus = MenuStatus.COLLAPSED
                    stickerStatus = StickerStatus.STICKER
                    binding.chatImgIv.setImageResource(R.drawable.ic_chat_img)
                    menuContainer.isVisible = false
                    stickerContainer.isVisible = false
                    galleryContainer.isVisible = false
                }
            }
        }
    private var sendStatus = AUDIO
        set(value) {
            if (value == field) return

            field = value
            checkSend()
        }
    private var stickerStatus = StickerStatus.STICKER
        set(value) {
            if (value == field) return

            field = value
            checkSticker()
        }

    private var menuStatus = MenuStatus.COLLAPSED
        set(value) {
            if (value == field) return

            field = value
            val anim =
                binding.chatMenuIv.animate()
                    .rotation(if (value == MenuStatus.EXPANDED) 45f else -45f)
            anim.setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.chatMenuIv.rotation = 0f
                        binding.chatMenuIv.setImageResource(if (value == MenuStatus.EXPANDED) R.drawable.ic_chat_more_checked else R.drawable.ic_chat_more)
                    }
                },
            )
            anim.start()
        }

    private var lastSendStatus = AUDIO

    var isRecording = false

    var activity: Activity? = null
    private lateinit var recordCircle: RecordCircleView
    private var upBeforeGrant = false

    private val sendDrawable: Drawable by lazy {
        requireNotNull(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_chat_send_checked,
                context.theme,
            ),
        )
    }
    private val audioDrawable: Drawable by lazy {
        requireNotNull(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_chat_mic,
                context.theme,
            ),
        )
    }

    private val stickerDrawable: Drawable by lazy {
        requireNotNull(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_chat_sticker,
                context.theme,
            ),
        )
    }
    private val keyboardDrawable: Drawable by lazy {
        requireNotNull(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_chat_keyboard,
                context.theme,
            ),
        )
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("CheckResult", "ClickableViewAccessibility")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        orientation = VERTICAL
        _binding = ViewChatControlBinding.inflate(LayoutInflater.from(context), this)

        binding.chatEt.addTextChangedListener(editTextWatcher)
        binding.chatEt.setOnKeyListener(keyListener)
        binding.chatEt.customSelectionActionModeCallback = this
        binding.chatSendIb.setOnTouchListener(sendOnTouchListener)
        binding.chatMenuIv.setOnClickListener(onChatMenuClickListener)
        binding.chatStickerIb.setOnClickListener(onStickerClickListener)
        binding.chatImgIv.setOnClickListener(onChatImgClickListener)
        binding.chatBotIv.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                callback.onBotClick()
            }
        binding.chatSlide.callback = chatSlideCallback

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
        controlState = STATUS.COLLAPSED
        setSend()
        inputLayout.closeInputArea(binding.chatEt)
        getVisibleContainer()?.isVisible = false
    }

    fun cancelExternal() {
        removeCallbacks(recordRunnable)
        cleanUp()
        updateRecordCircleAndSendIcon()
        binding.chatSlide.parent.requestDisallowInterceptTouchEvent(false)
    }

    private var botHide = false

    fun hideBot() {
        botHide = true
        binding.chatBotIv.visibility = View.GONE
        binding.chatEt.hint = getSignalHint()
        initTransitions()
    }

    fun showBot(category: EncryptCategory) {
        botHide = false
        binding.chatBotIv.visibility = View.VISIBLE
        hintEncrypt(category)
        initTransitions()
    }

    fun hintEncrypt(category: EncryptCategory) {
        binding.chatEt.hint =
            if (category.isEncrypt()) {
                getEncryptedHint()
            } else if (category.isSignal()) {
                getSignalHint()
            } else {
                getHint()
            }
    }

    private fun getEncryptedHint(): String =
        if (getLanguage() == Locale.JAPANESE.language) {
            "Encrypted"
        } else {
            context.getString(R.string.Encrypted)
        }

    private fun getSignalHint(): String =
        if (getLanguage() == Locale.JAPANESE.language) {
            "End-to-end Encrypted"
        } else {
            context.getString(R.string.End_to_End_Encryption)
        }

    private fun getHint(): String =
        if (getLanguage() == Locale.JAPANESE.language) {
            "Type message"
        } else {
            context.getString(R.string.Type_message)
        }

    fun toggleKeyboard(shown: Boolean) {
        if (shown) {
            controlState = STATUS.EXPANDED_KEYBOARD
        } else {
            if (controlState == STATUS.EXPANDED_KEYBOARD || controlState == STATUS.COLLAPSED) {
                controlState = STATUS.COLLAPSED
                inputLayout.closeInputArea(binding.chatEt)
            } else {
                if (controlState == STATUS.EXPANDED_MENU && !menuContainer.isVisible) {
                    controlState = STATUS.COLLAPSED
                    inputLayout.closeInputArea(binding.chatEt)
                } else if (controlState == STATUS.EXPANDED_STICKER && !stickerContainer.isVisible) {
                    controlState = STATUS.COLLAPSED
                    inputLayout.closeInputArea(binding.chatEt)
                }
            }
        }
        setSend()
    }

    fun getDraggableContainer() =
        when {
            stickerContainer.isVisible -> stickerContainer
            galleryContainer.isVisible -> galleryContainer
            else -> null
        }

    fun getVisibleContainer() =
        when {
            stickerContainer.isVisible -> stickerContainer
            galleryContainer.isVisible -> galleryContainer
            menuContainer.isVisible -> menuContainer
            else -> null
        }

    // remove focus but remain focusable
    private fun remainFocusable() {
        post {
            binding.chatEt.isFocusableInTouchMode = false
            binding.chatEt.isFocusable = false
            binding.chatEt.isFocusableInTouchMode = true
            binding.chatEt.isFocusable = true
        }
    }

    private fun initTransitions() {
        post {
            binding.bottomLl.layoutTransition = createTransitions()
            binding.editLl.layoutTransition = createEditTransitions()
        }
    }

    private fun checkSend(anim: Boolean = true) {
        val d =
            when (sendStatus) {
                REPLY, SEND -> sendDrawable
                AUDIO -> audioDrawable
                else -> throw IllegalArgumentException("error send status")
            }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        if (anim) {
            startScaleAnim(binding.chatSendIb, d)
        } else {
            binding.chatSendIb.setImageDrawable(d)
        }
    }

    private fun checkSticker() {
        val d =
            when (stickerStatus) {
                StickerStatus.STICKER -> stickerDrawable
                StickerStatus.KEYBOARD -> keyboardDrawable
            }
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        startScaleAnim(binding.chatStickerIb, d)
    }

    private fun startScaleAnim(
        v: ImageView,
        d: Drawable?,
    ) {
        val scaleUp =
            ObjectAnimator.ofPropertyValuesHolder(
                v,
                PropertyValuesHolder.ofFloat("scaleX", 0.6f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.6f, 1f),
            ).apply {
                duration = 100
            }
        val scaleDown =
            ObjectAnimator.ofPropertyValuesHolder(
                v,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.6f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.6f),
            ).apply {
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

    private fun handleCancelOrEnd(status: AudioEndStatus) {
        when (status) {
            AudioEndStatus.SEND -> {
                callback.onRecordSend()
            }
            AudioEndStatus.CANCEL -> {
                callback.onRecordCancel()
            }
            AudioEndStatus.PREVIEW -> {
                callback.onRecordPreview()
            }
        }
        cleanUp()
        updateRecordCircleAndSendIcon()
    }

    private var audioFile: File? = null

    fun previewAudio(
        conversationId: String,
        audioFile: File,
        waveForm: ByteArray,
        duration: Long,
        sendCallback: () -> Unit,
    ) {
        AudioPlayer.clear()
        binding.chatAudioWaveform.setWaveform(waveForm, true)
        binding.chatAudioWaveform.setBind(PREVIEW)
        binding.chatAudioPlay.setBind(PREVIEW)
        this.audioFile?.deleteOnExit()
        this.audioFile = audioFile
        binding.chatAudioPlay.setOnClickListener {
            if (AudioPlayer.isPlay(PREVIEW)) {
                AudioPlayer.pause()
            } else {
                AudioPlayer.play(audioFile.absolutePath)
            }
        }
        binding.chatAudioSend.setOnClickListener {
            AudioPlayer.seekTo(0)
            AudioPlayer.pause()
            sendCallback.invoke()
            MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
                OpusAudioRecorder.deletePreviewAudio(context, conversationId)
            }
            binding.chatAudioLayout.isVisible = false
        }
        binding.chatAudioDelete.setOnClickListener {
            AudioPlayer.seekTo(0)
            AudioPlayer.pause()
            MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
                audioFile.deleteOnExit()
                OpusAudioRecorder.deletePreviewAudio(context, conversationId)
            }
            binding.chatAudioLayout.isVisible = false
        }
        binding.chatAudioDuration.text = duration.formatMillis()
        binding.chatAudioLayout.isVisible = true
    }

    fun isPreviewAudio() = binding.chatAudioLayout.isVisible

    private fun updateRecordCircleAndSendIcon() {
        if (isRecording) {
            recordCircle.visibility = View.VISIBLE
            recordCircle.setAmplitude(.0)
            ObjectAnimator.ofFloat(recordCircle, "scale", 1f).apply {
                interpolator = DecelerateInterpolator()
                duration = 200
                addListener(
                    onEnd = {
                        recordCircle.visibility = View.VISIBLE
                    },
                    onCancel = {
                        recordCircle.visibility = View.VISIBLE
                    },
                )
            }.start()
            binding.chatSendIb.animate().setDuration(200).alpha(0f).start()
            binding.chatSlide.onStart()
        } else {
            ObjectAnimator.ofFloat(recordCircle, "scale", 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 200
                addListener(
                    onEnd = {
                        recordCircle.visibility = View.GONE
                        recordCircle.locked = false
                    },
                    onCancel = {
                        recordCircle.visibility = View.GONE
                        recordCircle.locked = false
                    },
                )
            }.start()
            binding.chatSendIb.animate().setDuration(200).alpha(1f).start()
            binding.chatSlide.onEnd()
        }
    }

    private fun currentAudio() = sendStatus == AUDIO

    @SuppressLint("ObjectAnimatorBinding")
    private fun createTransitions(): LayoutTransition {
        val scaleDownTransX = binding.chatSendIb.width
        val scaleDown =
            ObjectAnimator.ofPropertyValuesHolder(
                null as Any?,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0f),
                PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat()),
            ).apply {
                duration = 50
                interpolator = DecelerateInterpolator()
            }

        val scaleUp =
            ObjectAnimator.ofPropertyValuesHolder(
                null as Any?,
                PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
                PropertyValuesHolder.ofFloat("translationX", 0f),
            ).apply {
                duration = 50
                interpolator = DecelerateInterpolator()
            }

        return getLayoutTransition(scaleUp, scaleDown)
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun createEditTransitions(): LayoutTransition {
        val scaleDownTransX =
            right - binding.chatMenuIv.width - binding.chatSendIb.width - binding.editLl.width
        val scaleDown =
            ObjectAnimator.ofPropertyValuesHolder(
                null as Any?,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0f),
                PropertyValuesHolder.ofFloat("translationX", scaleDownTransX.toFloat()),
            ).apply {
                duration = 50
                interpolator = DecelerateInterpolator()
            }

        val scaleUp =
            ObjectAnimator.ofPropertyValuesHolder(
                null as Any?,
                PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
                PropertyValuesHolder.ofFloat("translationX", 0f),
            ).apply {
                duration = 50
                interpolator = DecelerateInterpolator()
            }

        return getLayoutTransition(scaleUp, scaleDown)
    }

    private fun getLayoutTransition(
        scaleUp: ObjectAnimator,
        scaleDown: ObjectAnimator,
    ): LayoutTransition {
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
                binding.chatEt.text?.let {
                    disposable =
                        Observable.just(it.trim().toString()).debounce(100L, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                {
                                    callback.onSendClick(it)
                                },
                                {},
                            )
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

    private var disposable: Disposable? = null

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    private fun isEditEmpty() = binding.chatEt.text.toString().trim().isEmpty()

    private fun realSetSend() {
        sendStatus =
            if (!isEditEmpty()) {
                if (!binding.chatStickerIb.isGone) {
                    binding.chatStickerIb.isGone = true
                }
                if (!botHide) {
                    if (!binding.chatBotIv.isGone) {
                        binding.chatBotIv.isGone = true
                    }
                }
                if (!binding.chatImgIv.isGone) {
                    binding.chatImgIv.isGone = true
                }
                SEND
            } else {
                if (!binding.chatStickerIb.isVisible) {
                    binding.chatStickerIb.isVisible = true
                }
                if (!botHide) {
                    if (!binding.chatBotIv.isVisible) {
                        binding.chatBotIv.isVisible = true
                    }
                }
                if (!binding.chatImgIv.isVisible) {
                    binding.chatImgIv.isVisible = true
                }
                lastSendStatus
            }
    }

    private val onChatMenuClickListener =
        OnClickListener {
            if (controlState == STATUS.EXPANDED_MENU) {
                controlState = STATUS.EXPANDED_KEYBOARD
                inputLayout.showSoftKey(binding.chatEt)
            } else {
                controlState = STATUS.EXPANDED_MENU
                inputLayout.openInputArea(binding.chatEt)
                callback.onMenuClick()
            }
            remainFocusable()
        }

    private val onStickerClickListener =
        OnClickListener {
            if (controlState == STATUS.EXPANDED_KEYBOARD || controlState == STATUS.COLLAPSED) {
                controlState = STATUS.EXPANDED_STICKER
                inputLayout.openInputArea(binding.chatEt)
                callback.onStickerClick()
            } else if (controlState == STATUS.EXPANDED_STICKER) {
                controlState = STATUS.EXPANDED_KEYBOARD
                inputLayout.showSoftKey(binding.chatEt)
            } else {
                controlState = STATUS.EXPANDED_STICKER
                inputLayout.openInputArea(binding.chatEt)
                callback.onStickerClick()
            }
            remainFocusable()
        }

    @SuppressLint("CheckResult")
    private val onChatImgClickListener =
        OnClickListener {
            clickGallery()
        }

    private fun clickGallery() {
        callback.onGalleryClick()
        remainFocusable()
    }

    private fun getFling(event: MotionEvent): Int {
        velocityTracker?.addMovement(event)
        velocityTracker?.computeCurrentVelocity(1000)
        val vY = velocityTracker?.yVelocity
        val vX = velocityTracker?.xVelocity
        velocityTracker?.recycle()
        velocityTracker = null
        return if (vY != null && abs(vY) >= minVelocity) {
            if (vX != null && abs(vX) > abs(vY)) {
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

    private val keyListener =
        OnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                callback.onDelete()
            }
            false
        }

    private val editTextWatcher =
        object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                setSend()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
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
                    dragging =
                        if (!dragging) {
                            abs(moveY - startY) > touchSlop
                        } else {
                            dragging
                        }
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
                    dragging =
                        if (!dragging) {
                            abs(dif) > touchSlop
                        } else {
                            dragging
                        }
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
    private var maxScrollX = 100.dp
    var calling = false

    @SuppressLint("ClickableViewAccessibility")
    private val sendOnTouchListener =
        OnTouchListener { _, event ->
            if (calling && sendStatus == AUDIO) {
                callback.onCalling()
                return@OnTouchListener false
            }

            binding.chatSendIb.onTouchEvent(event)
            when (event.action) {
                ACTION_DOWN -> {
                    if (recordCircle.locked) {
                        return@OnTouchListener false
                    }

                    originX = event.rawX
                    startX = event.rawX
                    val w = binding.chatSlide.slideWidth
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
                    if (sendStatus == SEND && !triggeredCancel) {
                        if (System.currentTimeMillis() - startTime > LONG_CLICK_DELAY) {
                            val text = binding.chatEt.text?.trim()?.toString()
                            if (!text.isNullOrBlank()) {
                                callback.onSendLongClick(text)
                                triggeredCancel = true
                                cleanUp()
                            }
                        }
                        return@OnTouchListener true
                    }
                    if (!currentAudio() || recordCircle.locked || !hasStartRecord) return@OnTouchListener false
                    val x = recordCircle.setLockTranslation(event.y)
                    if (x == 2) {
                        ObjectAnimator.ofFloat(
                            recordCircle,
                            "lockAnimatedTranslation",
                            recordCircle.startTranslation,
                        ).apply {
                            duration = 150
                            interpolator = DecelerateInterpolator()
                        }.start()
                        binding.chatSlide.toCancel()
                        callback.onRecordLocked()
                        return@OnTouchListener false
                    }

                    val moveX = event.rawX
                    if (moveX != 0f) {
                        binding.chatSlide.slideText(startX - moveX)
                        if (originX - moveX > maxScrollX) {
                            removeRecordRunnable()
                            handleCancelOrEnd(AudioEndStatus.CANCEL)
                            binding.chatSlide.parent.requestDisallowInterceptTouchEvent(false)
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
                        handleCancelOrEnd(AudioEndStatus.CANCEL)
                    } else if (!recordCircle.locked) {
                        removeRecordRunnable()
                        handleCancelOrEnd(AudioEndStatus.SEND)
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

    private val hideRecordTipRunnable =
        Runnable {
            if (recordTipView.visibility == View.VISIBLE) {
                recordTipView.fadeOut()
            }
        }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            hasStartRecord = true
            removeCallbacks(hideRecordTipRunnable)
            post(hideRecordTipRunnable)

            val a = activity
            if (a == null || !currentAudio()) return@Runnable

            val fa = a as FragmentActivity
            if (!RxPermissions(fa).isGranted(Manifest.permission.RECORD_AUDIO) ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !RxPermissions(fa).isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            ) {
                RxPermissions(fa)
                    .request(
                        *mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }.toTypedArray(),
                    )
                    .autoDispose(this)
                    .subscribe({}, { reportException(it) })
                return@Runnable
            }
            callback.onRecordStart(sendStatus == AUDIO)
            upBeforeGrant = false
            post(checkReadyRunnable)
            binding.chatSendIb.parent.requestDisallowInterceptTouchEvent(true)
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

    private val chatSlideCallback =
        object : SlidePanelView.Callback {
            override fun onTimeout() {
                handleCancelOrEnd(AudioEndStatus.SEND)
            }

            override fun onCancel() {
                handleCancelOrEnd(AudioEndStatus.CANCEL)
            }
        }

    private val recordCircleCallback =
        object : RecordCircleView.Callback {
            override fun onSend() {
                handleCancelOrEnd(AudioEndStatus.SEND)
            }

            override fun onCancel() {
                handleCancelOrEnd(AudioEndStatus.CANCEL)
            }

            override fun onPreview() {
                handleCancelOrEnd(AudioEndStatus.PREVIEW)
            }
        }

    interface Callback {
        fun onStickerClick()

        fun onSendClick(text: String)

        fun onSendLongClick(text: String)

        fun onRecordStart(audio: Boolean)

        fun isReady(): Boolean

        fun onRecordSend()

        fun onRecordCancel()

        fun onRecordPreview()

        fun onCalling()

        fun onMenuClick()

        fun onBotClick()

        fun onGalleryClick()

        fun onDragChatControl(dis: Float)

        fun onReleaseChatControl(fling: Int)

        fun onRecordLocked()

        fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        )

        fun onDelete()
    }

    override fun onCreateActionMode(
        actionMode: ActionMode,
        menu: Menu,
    ): Boolean {
        val menuInflater: MenuInflater = actionMode.menuInflater
        menuInflater.inflate(R.menu.selection_action_menu, menu)
        return true
    }

    override fun onPrepareActionMode(
        actionMode: ActionMode,
        menu: Menu,
    ): Boolean {
        return false
    }

    override fun onActionItemClicked(
        actionMode: ActionMode,
        item: MenuItem,
    ): Boolean {
        val start = binding.chatEt.selectionStart
        val end = binding.chatEt.selectionEnd
        binding.chatEt.text?.let { editable ->
            val symbol =
                when (item.itemId) {
                    R.id.bold -> {
                        "**"
                    }
                    R.id.italic -> {
                        "_"
                    }
                    R.id.strikethrough -> {
                        "~~"
                    }
                    R.id.code -> {
                        "`"
                    }
                    else -> ""
                }

            editable.insert(end, symbol)
            editable.insert(start, symbol)
        }

        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
    }
}
