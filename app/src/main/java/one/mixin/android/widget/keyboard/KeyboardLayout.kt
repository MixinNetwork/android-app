package one.mixin.android.widget.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.fragment_conversation.view.*
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.DragReleaseEvent
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenHeight
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_UP
import kotlin.math.ceil
import kotlin.math.max

class KeyboardLayout : LinearLayout {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val defaultCustomKeyboardSize =
        resources.getDimensionPixelSize(R.dimen.default_custom_keyboard_size)
    private var systemBottom = 0
    private var systemTop = 0

    private enum class STATUS {
        EXPANDED, OPENED, KEYBOARD_OPENED, CLOSED
    }

    private var status = STATUS.CLOSED

    fun keyboardOpened() = status == STATUS.KEYBOARD_OPENED

    var keyboardHeight: Int = PreferenceManager.getDefaultSharedPreferences(context)
        .getInt("keyboard_height_portrait", defaultCustomKeyboardSize)
        private set(value) {
            if (field != value) {
                field = value
                PreferenceManager.getDefaultSharedPreferences(context)
                    .putInt("keyboard_height_portrait", value)
            }
        }

    private var inputAreaHeight: Int = 0
        set(value) {
            if (value != field || input_area.layoutParams.height != value) {
                field = value
                input_area.layoutParams.height = value
                TransitionManager.beginDelayedTransition(
                    this,
                    AutoTransition()
                        .setInterpolator(
                            LinearInterpolator()
                        ).setDuration(
                            (
                                context.resources.getInteger(android.R.integer.config_shortAnimTime)
                                    .toLong() * 0.6f
                                ).toLong()
                        )
                )
                requestLayout()
            }
        }

    fun displayInputArea(inputTarget: EditText) {
        inputAreaHeight = keyboardHeight - systemBottom
        status = STATUS.OPENED
        hideSoftKey(inputTarget)
    }

    fun hideInputArea(inputTarget: EditText?) {
        inputAreaHeight = 0
        status = STATUS.CLOSED
        if (inputTarget != null) {
            hideSoftKey(inputTarget)
        }
    }

    init {
        setWillNotDraw(false)
        orientation = VERTICAL
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemInserts ->
                systemBottom = systemInserts.bottom
                systemTop = systemInserts.top
                updatePadding(
                    top = systemTop,
                    bottom = systemBottom
                )
            }
            insets.getInsets(WindowInsetsCompat.Type.ime())
                .let { imeInserts ->
                    max(imeInserts.bottom - systemBottom, 0).let { value ->
                        if (value > 0) {
                            if (status != STATUS.KEYBOARD_OPENED) {
                                status = STATUS.KEYBOARD_OPENED
                                if (inputAreaHeight != value)
                                    onKeyboardShownListener?.onKeyboardShown(imeInserts.bottom)
                            }
                            inputAreaHeight = value
                        } else {
                            if (status == STATUS.KEYBOARD_OPENED) {
                                status = STATUS.CLOSED
                                onKeyboardHiddenListener?.onKeyboardHidden()
                            }
                        }
                    }
                    if (imeInserts.bottom > 0) {
                        keyboardHeight = imeInserts.bottom
                    }
                }
            WindowInsetsCompat.CONSUMED
        }
    }

    var backgroundImage: Drawable? = null
        set(bitmap) {
            field = bitmap
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        backgroundImage.notNullWithElse(
            { backgroundImage ->
                val actionBarHeight = context.appCompatActionBarHeight()
                val viewHeight = measuredHeight - actionBarHeight - systemBottom
                val scaleX = measuredWidth.toFloat() / backgroundImage.intrinsicWidth.toFloat()
                val scaleY = (viewHeight).toFloat() / backgroundImage.intrinsicHeight.toFloat()
                val scale = if (scaleX < scaleY) scaleY else scaleX
                val width = ceil((backgroundImage.intrinsicWidth * scale).toDouble()).toInt()
                val height =
                    ceil((backgroundImage.intrinsicHeight * scale).toDouble()).toInt()
                val x = (measuredWidth - width) / 2
                val y = (viewHeight - height) / 2
                if (systemBottom != 0) {
                    canvas.save()
                    canvas.clipRect(0, actionBarHeight, width, measuredHeight - systemBottom)
                }
                backgroundImage.setBounds(x, y, x + width, y + height)
                backgroundImage.draw(canvas)
                if (systemBottom != 0) {
                    canvas.restore()
                }
            },
            {
                super.onDraw(canvas)
            }
        )
    }

    private var onKeyboardHiddenListener: OnKeyboardHiddenListener? = null
    fun setOnKeyBoardHiddenListener(onKeyboardHiddenListener: OnKeyboardHiddenListener?) {
        this.onKeyboardHiddenListener = onKeyboardHiddenListener
    }

    private var onKeyboardShownListener: OnKeyboardShownListener? = null
    fun setOnKeyboardShownListener(onKeyboardShownListener: OnKeyboardShownListener?) {
        this.onKeyboardShownListener = onKeyboardShownListener
    }

    fun showSoftKey(inputTarget: ContentEditText) {
        post {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                inputTarget,
                0
            )
        }
    }

    private fun hideSoftKey(inputTarget: EditText) {
        (inputTarget.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            inputTarget.windowToken,
            0
        )
    }

    fun drag(dis: Float) {
        val params = input_area.layoutParams
        val targetH = params.height - dis.toInt()
        val total = context.screenHeight() * 2 / 3
        if (targetH <= 0 || targetH >= total) return

        params.height = targetH
        input_area.layoutParams = params
    }

    fun releaseDrag(fling: Int, resetCallback: () -> Unit) {
        val curH = input_area.height
        val max = (context.screenHeight() * 2) / 3
        val maxMid = keyboardHeight + (max - keyboardHeight) / 2
        val minMid = keyboardHeight / 2
        val targetH = if (curH > input_layout.keyboardHeight) {
            if (fling == FLING_UP) {
                max
            } else if (fling == FLING_DOWN) {
                input_layout.keyboardHeight
            } else {
                if (curH <= maxMid) {
                    input_layout.keyboardHeight
                } else {
                    max
                }
            }
        } else if (curH < input_layout.keyboardHeight) {
            if (fling == FLING_UP) {
                input_layout.keyboardHeight
            } else if (fling == FLING_DOWN) {
                0
            } else {
                if (curH > minMid) {
                    input_layout.keyboardHeight
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
        input_area.animateHeight(curH, targetH)
        RxBus.publish(DragReleaseEvent(targetH == max))
    }

    interface OnKeyboardHiddenListener {
        fun onKeyboardHidden()
    }

    interface OnKeyboardShownListener {
        fun onKeyboardShown(height: Int)
    }
}
