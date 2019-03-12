package one.mixin.android.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.view_verification_code.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import org.jetbrains.anko.backgroundColor

class VerificationCodeView : LinearLayout {

    companion object {
        const val DEFAULT_COUNT = 4
    }

    private val spaces = ArrayList<View>()
    private val codes = ArrayList<TextView>()
    private val containers = ArrayList<View>()

    private var spaceColor: Int = 0

    private var listener: OnCodeEnteredListener? = null
    private var index = 0
    var count = DEFAULT_COUNT
    private var isError = false

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context, attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
        : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeView)
        try {
            count = typedArray.getInteger(R.styleable.VerificationCodeView_vcv_count, DEFAULT_COUNT)
            for (i in 0 until count) {
                val item = View.inflate(context, R.layout.view_verification_code, null)
                codes.add(item.code)
                spaces.add(item.space)
                containers.add(item)
                addView(item)
            }

            spaceColor = typedArray.getColor(R.styleable.VerificationCodeView_vcv_inputColor, Color.BLACK)
            spaces.forEach { view ->
                view.updateLayoutParams<LinearLayout.LayoutParams> {
                    height = typedArray.getDimensionPixelSize(
                        R.styleable.VerificationCodeView_vcv_inputHeight, context.dpToPx(1f))
                }
            }
            codes.forEach { textView ->
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    typedArray.getDimension(R.styleable.VerificationCodeView_vcv_textSize, 30f))
                textView.setTextColor(
                    typedArray.getColor(R.styleable.VerificationCodeView_vcv_textColor, Color.GRAY))
            }
            containers.forEach { view ->
                view.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeView_vcv_inputWidth, context.dpToPx(20f))
                }
            }
        } finally {
            typedArray?.recycle()
        }
        updateSpace(0, true)
    }

    @MainThread
    fun setOnCodeEnteredListener(listener: OnCodeEnteredListener) {
        this.listener = listener
    }

    @MainThread
    fun append(value: String) {
        if (index >= codes.size) return
        if (isError) {
            setColor(resources.getColor(R.color.verification_code_line, null))
        }

        updateSpace(index, false)
        val codeView = codes[index++]

        val translateIn = TranslateAnimation(0f, 0f, codeView.height.toFloat(), 0f)
        translateIn.interpolator = OvershootInterpolator()
        translateIn.duration = 500

        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 200

        val animationSet = AnimationSet(false)
        animationSet.addAnimation(fadeIn)
        animationSet.addAnimation(translateIn)
        animationSet.reset()
        animationSet.startTime = 0

        codeView.text = value
        codeView.clearAnimation()
        codeView.startAnimation(animationSet)

        listener?.onCodeEntered(code())
    }

    private fun updateSpace(index: Int, isPre: Boolean) {
        var nextIndex = index
        if (!isPre && index < codes.size - 1) {
            nextIndex = index + 1
        } else if (isPre && index > 0) {
            nextIndex = index - 1
        }
        spaces.forEachIndexed { i, view ->
            if (i != nextIndex) {
                view.setBackgroundColor(resources.getColor(R.color.verification_code_line, null))
            }
        }
        if (nextIndex < codes.size && nextIndex >= 0) {
            val nextSpace = spaces[nextIndex]
            nextSpace.backgroundColor = spaceColor
        }
    }

    fun code(): String {
        val sb = StringBuilder()
        for (i in codes) {
            sb.append(i.text)
        }
        return sb.toString()
    }

    @MainThread
    fun delete() {
        if (index <= 0) return
        updateSpace(index, true)
        codes[--index].text = ""

        listener?.onCodeEntered(code())
    }

    @MainThread
    fun clear() {
        if (index != 0) {
            codes.forEach { code -> code.text = "" }
            index = 0

//            updateSpace(0, true)
        }

        listener?.onCodeEntered(code())
    }

    fun error() {
        setColor(resources.getColor(android.R.color.holo_red_light, null))
        clear()
        isError = true
    }

    private fun setColor(color: Int) {
        for (i in 0 until count) {
            codes[i].text = ""
            codes[i].backgroundTintList = ColorStateList.valueOf(color)
            spaces[i].backgroundColor = color
        }
        isError = false
    }

    interface OnCodeEnteredListener {
        fun onCodeEntered(code: String)
    }
}
