package one.mixin.android.widget.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import kotlin.math.max

class KeyboardLayout : LinearLayout {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var keyboardHeight: Int = 0
        set(value) {
            if (value != field) {
                field = value
                (getChildAt(1).layoutParams as MarginLayoutParams).bottomMargin = value
                isInputOpen = value > 0
                if (isInputOpen) {
                    onKeyboardShownListener?.onKeyboardShown(value)
                } else {
                    onKeyboardHiddenListener?.onKeyboardHidden()
                }
                TransitionManager.beginDelayedTransition(
                    this, AutoTransition()
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

    private var systemBottom = 0
    var isInputOpen = false
        private set

    init {
        orientation = VERTICAL
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemInserts ->
                systemBottom = systemInserts.bottom
                updatePadding(
                    top = systemInserts.top,
                    bottom = systemBottom
                )
            }
            insets.getInsets(WindowInsetsCompat.Type.ime())
                .let { imeInserts -> keyboardHeight = max(imeInserts.bottom - systemBottom, 0) }
            WindowInsetsCompat.CONSUMED
        }
    }

    private var onKeyboardHiddenListener: OnKeyboardHiddenListener? = null
    fun setOnKeyBoardHiddenListener(onKeyboardHiddenListener: OnKeyboardHiddenListener?) {
        this.onKeyboardHiddenListener = onKeyboardHiddenListener
    }

    private var onKeyboardShownListener: OnKeyboardShownListener? = null
    fun setOnKeyboardShownListener(onKeyboardShownListener: OnKeyboardShownListener?) {
        this.onKeyboardShownListener = onKeyboardShownListener
    }

    interface OnKeyboardHiddenListener {
        fun onKeyboardHidden()
    }

    interface OnKeyboardShownListener {
        fun onKeyboardShown(height: Int)
    }
}