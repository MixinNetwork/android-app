package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.SparseArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import one.mixin.android.R
import one.mixin.android.databinding.ViewPinBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.hintTextColor
import one.mixin.android.extension.textColor

class PinView : LinearLayout {
    companion object {
        const val DEFAULT_COUNT = 6
    }

    private var color = context.colorFromAttribute(R.attr.text_primary)
    private var count = DEFAULT_COUNT

    // control tip_tv and line visibility
    private var tipVisible = true

    private val views = ArrayList<View>()
    private val codes = SparseArray<String>()
    private var index = 0
    private var listener: OnPinListener? = null
    private var finishListener: OnPinFinishListener? = null
    private val textSize = 26f
    private val starSize = 18f
    private val dotSize = 14.dp
    private val dotMargin = 12.dp

    private val binding = ViewPinBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PinView)
        if (ta.hasValue(R.styleable.PinView_pin_color)) {
            color = ta.getColor(R.styleable.PinView_pin_color, Color.BLACK)
        }
        if (ta.hasValue(R.styleable.PinView_pin_count)) {
            count = ta.getInt(R.styleable.PinView_pin_count, DEFAULT_COUNT)
        }
        if (ta.hasValue(R.styleable.PinView_pin_tipVisible)) {
            tipVisible = ta.getBoolean(R.styleable.PinView_pin_tipVisible, true)
            if (!tipVisible) {
                binding.tipTv.visibility = View.GONE
            }
        }
        ta.recycle()
        orientation = VERTICAL
        for (i in 0 until count) {
            val item = createDotView()
            views.add(item)
            binding.containerLl.addView(item)
        }
        binding.containerLl.requestLayout()
    }

    private fun createDotView() = View(context).apply {
        layoutParams = LayoutParams(dotSize, dotSize).apply {
            setMargins(dotMargin, 0, dotMargin, 0)
            gravity = Gravity.CENTER
        }
        background = context.getDrawable(R.drawable.pin_circle_empty)
    }

    fun append(s: String) {
        if (!isEnabled) return
        if (index >= views.size) return
        if (tipVisible && binding.tipTv.visibility == View.VISIBLE) {
            binding.tipTv.visibility = View.INVISIBLE
        }

        val curItem = views[index]
        curItem.background = context.getDrawable(R.drawable.pin_circle_filled)
        codes.append(index, s)
        index++

        listener?.onUpdate(index)
        if (index == count) {
            finishListener?.onPinFinish()
        }
    }

    fun set(s: String) {
        if (!isEnabled) return
        if (s.length != count) return
        for (i in 0 until count) {
            val c = s[i]
            val v = views[i]
            v.background = context.getDrawable(R.drawable.pin_circle_filled)
            codes.append(i, c.toString())
        }
        listener?.onUpdate(count)
    }

    fun delete() {
        if (!isEnabled) return
        if (index <= 0) return
        index--
        val codeView = views[index]
        codeView.background = context.getDrawable(R.drawable.pin_circle_empty)

        listener?.onUpdate(index)
    }

    fun clear() {
        if (index != 0) {
            views.forEach { code -> code.background = context.getDrawable(R.drawable.pin_circle_empty) }
            codes.clear()
            index = 0
        }

        listener?.onUpdate(index)
    }

    fun code(): String {
        val sb = StringBuilder()
        for (i in 0 until codes.size()) {
            sb.append(codes[i])
        }
        return sb.toString()
    }

    fun error(tip: String) {
        if (!tipVisible) return

        binding.tipTv.text = tip
        binding.tipTv.visibility = View.VISIBLE
        clear()
    }

    fun setListener(listener: OnPinListener) {
        this.listener = listener
    }

    fun getCount() = count

    interface OnPinListener {
        fun onUpdate(index: Int)
    }

    fun setOnPinFinishListener(listener: OnPinFinishListener) {
        this.finishListener = listener
    }

    interface OnPinFinishListener {
        fun onPinFinish()
    }
}
