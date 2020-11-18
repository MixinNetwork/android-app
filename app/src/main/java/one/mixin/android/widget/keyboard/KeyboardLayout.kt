package one.mixin.android.widget.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
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
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.putInt
import one.mixin.android.widget.ContentEditText
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

    var keyboardHeight: Int = PreferenceManager.getDefaultSharedPreferences(context)
        .getInt("keyboard_height_portrait", defaultCustomKeyboardSize)
        private set(value) {
            if (field != value) {
                field = value
                PreferenceManager.getDefaultSharedPreferences(context)
                    .putInt("keyboard_height_portrait", value)
            }
        }

    var isInputOpen = false
        private set

    private var inputAreaHeight: Int = 0
        set(value) {
            if (value != field || input_area.layoutParams.height != value) {
                field = value
                input_area.layoutParams.height = value
                TransitionManager.beginDelayedTransition(
                    this,
                    AutoTransition()
                        .setInterpolator(
                            DecelerateInterpolator()
                        ).setDuration(
                            context.resources.getInteger(android.R.integer.config_shortAnimTime)
                                .toLong() - if (isInputOpen) {
                                0
                            } else {
                                30
                            }
                        )
                )
                requestLayout()
            }
        }

    fun displayInputArea(inputTarget: EditText) {
        inputAreaHeight = keyboardHeight - systemBottom
        displayInput = true
        if (isInputOpen) {
            hideSoftKey(inputTarget)
        }
    }

    fun hideInputArea(inputTarget: EditText?) {
        inputAreaHeight = 0
        displayInput = false
        if (inputTarget != null) {
            hideSoftKey(inputTarget)
        }
    }

    private var displayInput = false

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
                        isInputOpen = value > 0
                        if (isInputOpen) {
                            onKeyboardShownListener?.onKeyboardShown(value)
                        } else {
                            onKeyboardHiddenListener?.onKeyboardHidden()
                        }
                        if (!displayInput) {
                            inputAreaHeight = value
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

    fun hideCurrentInput(inputTarget: ContentEditText) {
        if (isInputOpen) {
            hideSoftKey(inputTarget)
        } else {
            inputAreaHeight = 0
        }
    }

    fun showSoftKey(inputTarget: ContentEditText) {
        inputTarget.post {
            inputTarget.requestFocus()
            (
                inputTarget.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                ).showSoftInput(
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

    interface OnKeyboardHiddenListener {
        fun onKeyboardHidden()
    }

    interface OnKeyboardShownListener {
        fun onKeyboardShown(height: Int)
    }
}
