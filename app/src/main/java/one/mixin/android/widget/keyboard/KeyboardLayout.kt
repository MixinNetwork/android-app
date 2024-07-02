package one.mixin.android.widget.keyboard

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.DragReleaseEvent
import one.mixin.android.extension.ANIMATION_DURATION_SHORTEST
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.dp
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.showKeyboard
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_UP
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.max

class KeyboardLayout : LinearLayout {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        Timber.e("Constructor with context, attrs, and defStyleAttr called")
        val ta = context.obtainStyledAttributes(attrs, R.styleable.KeyboardLayout)
        inputAreaId = ta.getResourceIdOrThrow(R.styleable.KeyboardLayout_input_aera_id)
        ta.recycle()
    }

    private val defaultCustomKeyboardSize =
        resources.getDimensionPixelSize(R.dimen.default_custom_keyboard_size)
    private var systemBottom = 0
    private var systemTop = 0

    private enum class STATUS {
        EXPANDED,
        OPENED,
        KEYBOARD_OPENED,
        CLOSED,
    }

    private var status = STATUS.CLOSED

    var keyboardHeight: Int =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("keyboard_height_portrait", defaultCustomKeyboardSize)
        private set(value) {
            if (field != value && value > 0) {
                field = value
                if (value >= 100.dp) {
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putInt("keyboard_height_portrait", value)
                        .apply()
                }
            }
        }

    private val _inputArea get() = requireNotNull(findViewById(inputAreaId))

    @IdRes
    private val inputAreaId: Int

    private var inputAreaHeight: Int = 0
        @SuppressLint("Recycle")
        set(value) {
            if (value != field || _inputArea.layoutParams.height != value) {
                field = value
                Timber.e("Setting inputAreaHeight to $value")
                ValueAnimator.ofInt(_inputArea.layoutParams.height, value)
                    .apply {
                        addUpdateListener { valueAnimator ->
                            _inputArea.layoutParams.height = valueAnimator.animatedValue as Int
                            requestLayout()
                        }
                        interpolator = CubicBezierInterpolator.DEFAULT
                        duration = ANIMATION_DURATION_SHORTEST
                    }.start()
            }
        }

    fun openInputArea(inputTarget: EditText) {
        inputAreaHeight = keyboardHeight - systemBottom
        status = STATUS.OPENED
        hideSoftKey(inputTarget)
        Timber.e("openInputArea: Input area opened with height $inputAreaHeight")
    }

    fun closeInputArea(inputTarget: EditText?) {
        inputAreaHeight = 0
        if (inputTarget != null) {
            status = STATUS.CLOSED
            hideSoftKey(inputTarget)
            Timber.e("closeInputArea: Input area closed")
        } else {
            status = STATUS.OPENED
        }
    }

    fun forceClose(editText: EditText? = null) {
        _inputArea.layoutParams.height = 0
        requestLayout()
        editText?.hideKeyboard()
        status = STATUS.CLOSED
        Timber.e("forceClose: Input area force closed")
    }

    fun showSoftKey(inputTarget: ContentEditText) {
        inputTarget.showKeyboard()
        postDelayed(
            {
                inputTarget.requestFocus()
                Timber.e("showSoftKey: Soft keyboard shown")
            },
            20,
        )
    }

    private fun hideSoftKey(inputTarget: EditText) {
        inputTarget.hideKeyboard()
        Timber.e("hideSoftKey: Soft keyboard hidden")
    }

    init {
        setWillNotDraw(false)
        orientation = VERTICAL
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemInserts ->
                systemBottom = systemInserts.bottom
                systemTop = systemInserts.top
            }
            Timber.e("Window insets applied with systemBottom: $systemBottom, systemTop: $systemTop")
            if (inMultiWindowMode) {
                calculateInsertBottom(insets.getInsets(WindowInsetsCompat.Type.ime()))
            } else {
                max(
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - systemBottom,
                    0,
                ).let { value ->
                    if (lastKeyboardHeight == value) return@let
                    lastKeyboardHeight = value
                    Timber.e("IME insets changed, new height: $value")
                    if (value > 0 && value != inputAreaHeight) {
                        inputAreaHeight = value
                    }
                    if (value > 0) {
                        // If the callback saved keyboard height is very small, and the height obtained at this time is greater than it, reset the new value
                        if (100.dp in (keyboardHeight + 1) until lastKeyboardHeight) {
                            keyboardHeight = lastKeyboardHeight
                        }
                        onKeyboardShownListener?.onKeyboardShown(value)
                    } else {
                        onKeyboardHiddenListener?.onKeyboardHidden()
                    }
                }
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            this,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    Timber.e("WindowInsetsAnimation progress, status: $status")
                    if (status == STATUS.CLOSED || status == STATUS.KEYBOARD_OPENED) {
                        _inputArea.layoutParams.height =
                            max(
                                0,
                                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - systemBottom,
                            )
                        requestLayout()
                    } else if (status == STATUS.EXPANDED) {
                        val percent =
                            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom / keyboardHeight.toFloat()
                        _inputArea.layoutParams.height =
                            (keyboardHeight - systemBottom + gap * (1 - percent)).toInt()
                        requestLayout()
                    }
                    return insets
                }

                private var gap = 0

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    super.onPrepare(animation)
                    Timber.e("WindowInsetsAnimation prepare, status: $status")
                    if (status == STATUS.EXPANDED) {
                        gap = _inputArea.layoutParams.height - keyboardHeight
                    }
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    keyboardHeight = ViewCompat.getRootWindowInsets(this@KeyboardLayout)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                    Timber.e("WindowInsetsAnimation start, new keyboardHeight: $keyboardHeight")
                    return super.onStart(animation, bounds)
                }
            },
        )
    }

    var backgroundImage: Drawable? = null
        set(bitmap) {
            field = bitmap
            invalidate()
            Timber.e("Background image set")
        }

    override fun onDraw(canvas: Canvas) {
        val bg = this.backgroundImage
        if (bg != null) {
            val actionBarHeight = context.appCompatActionBarHeight()
            val scaleX = measuredWidth.toFloat() / bg.intrinsicWidth.toFloat()
            val scaleY = (measuredHeight).toFloat() / bg.intrinsicHeight.toFloat()
            val scale = if (scaleX < scaleY) scaleY else scaleX
            val width = ceil((bg.intrinsicWidth * scale).toDouble()).toInt()
            val height =
                ceil((bg.intrinsicHeight * scale).toDouble()).toInt()
            val x = (measuredWidth - width) / 2
            val y = (measuredHeight - height) / 2
            canvas.save()
            canvas.clipRect(
                0,
                actionBarHeight,
                measuredWidth,
                measuredHeight,
            )
            bg.setBounds(x, y, x + width, y + height)
            bg.draw(canvas)
            canvas.restore()
            Timber.e("Custom background drawn")
        } else {
            super.onDraw(canvas)
        }
    }

    private var onKeyboardHiddenListener: OnKeyboardHiddenListener? = null

    fun setOnKeyBoardHiddenListener(onKeyboardHiddenListener: OnKeyboardHiddenListener?) {
        this.onKeyboardHiddenListener = onKeyboardHiddenListener
        Timber.e("Keyboard hidden listener set")
    }

    private var onKeyboardShownListener: OnKeyboardShownListener? = null

    fun setOnKeyboardShownListener(onKeyboardShownListener: OnKeyboardShownListener?) {
        this.onKeyboardShownListener = onKeyboardShownListener
        Timber.e("Keyboard shown listener set")
    }

    fun drag(dis: Float) {
        if (status == STATUS.KEYBOARD_OPENED) return
        val params = _inputArea.layoutParams
        val targetH = params.height - dis.toInt()
        val total = context.screenHeight() * 2 / 3
        if (targetH <= 0 || targetH >= total) return

        params.height = targetH
        _inputArea.layoutParams = params
        Timber.e("Input area dragged, new height: $targetH")
    }

    fun releaseDrag(
        fling: Int,
        resetCallback: () -> Unit,
    ) {
        if (status == STATUS.KEYBOARD_OPENED) return
        val curH = _inputArea.height
        val max = (context.screenHeight() * 2) / 3
        val maxMid = keyboardHeight + (max - keyboardHeight) / 2
        val minMid = keyboardHeight / 2
        val targetH =
            if (curH > keyboardHeight) {
                if (fling == FLING_UP) {
                    max
                } else if (fling == FLING_DOWN) {
                    keyboardHeight - systemBottom
                } else {
                    if (curH <= maxMid) {
                        keyboardHeight - systemBottom
                    } else {
                        max
                    }
                }
            } else if (curH < keyboardHeight) {
                if (fling == FLING_UP) {
                    keyboardHeight
                } else if (fling == FLING_DOWN) {
                    0
                } else {
                    if (curH > minMid) {
                        keyboardHeight - systemBottom
                    } else {
                        0
                    }
                }
            } else {
                when (fling) {
                    FLING_UP -> {
                        max
                    }
                    FLING_DOWN -> {
                        0
                    }
                    else -> {
                        keyboardHeight
                    }
                }
            }
        when (targetH) {
            0 -> {
                status = STATUS.CLOSED
                resetCallback.invoke()
            }
            max -> {
                status = STATUS.EXPANDED
            }
            else -> {
                status = STATUS.OPENED
            }
        }

        ValueAnimator.ofInt(curH, targetH).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener { valueAnimator ->
                _inputArea.updateLayoutParams<ViewGroup.LayoutParams> {
                    this.height = valueAnimator.animatedValue as Int
                }
                Timber.e("Input area height animated to ${valueAnimator.animatedValue as Int}")
            }
        }.start()

        RxBus.publish(DragReleaseEvent(targetH == max))
        Timber.e("Drag released, target height: $targetH")
    }

    private var lastKeyboardHeight = 0

    private fun calculateInsertBottom(imeInserts: Insets) {
        max(imeInserts.bottom - systemBottom, 0).let { value ->
            if (lastKeyboardHeight == value) return@let
            lastKeyboardHeight = value
            Timber.e("Calculated insert bottom, new height: $value")
            if (value > 0) {
                status = STATUS.KEYBOARD_OPENED
                onKeyboardShownListener?.onKeyboardShown(imeInserts.bottom)
                inputAreaHeight = value
            } else {
                if (status == STATUS.KEYBOARD_OPENED) {
                    status = STATUS.CLOSED
                    inputAreaHeight = value
                }
                onKeyboardHiddenListener?.onKeyboardHidden()
            }
        }
        if (imeInserts.bottom > 0) {
            keyboardHeight = imeInserts.bottom
        }
    }

    private var inMultiWindowMode = false

    fun onMultiWindowModeChanged(inMultiWindowMode: Boolean) {
        this.inMultiWindowMode = inMultiWindowMode
        Timber.e("Multi-window mode changed: $inMultiWindowMode")
    }

    interface OnKeyboardHiddenListener {
        fun onKeyboardHidden()
    }

    interface OnKeyboardShownListener {
        fun onKeyboardShown(height: Int)
    }
}
