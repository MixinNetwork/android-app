@file:Suppress("unused")

package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import one.mixin.android.R
import org.jetbrains.anko.dip

class PasswordView : View {

    companion object {
        private const val RING_WIDTH = 2
        private const val CIRCLE_RADIUS = 6
        private const val CIRCLE_COUNT = 6
        private const val COLOR = android.R.color.white
    }

    private var circleColor = resources.getColor(COLOR, null)
    private var ringWidth = dip(RING_WIDTH).toFloat()
    private var circleRadius: Float = dip(CIRCLE_RADIUS).toFloat()
    var count = CIRCLE_COUNT

    private var listener: PasswordViewListener? = null
    private var passwordList: CharArray
    private var pos = 0

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = circleColor
        strokeWidth = ringWidth
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PasswordView)
        if (ta.hasValue(R.styleable.PasswordView_circleColor)) {
            circleColor = ta.getColor(R.styleable.PasswordView_circleColor, ContextCompat.getColor(context, COLOR))
            ringPaint.color = circleColor
            circlePaint.color = circleColor
        }
        ta.recycle()

        attrs?.let {
            val bgValue = it.getAttributeValue("http://schemas.android.com/apk/res/android", "background")
            if (bgValue == null) {
                background = ResourcesCompat.getDrawable(resources, R.drawable.bg_view_password, context.theme)
            }
        }

        passwordList = CharArray(count)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val y = height.toFloat() / 2
        // (3/5-width - 6-circle-width) / 5-interval
        val interval = (width * 3 / 5 - (6 * circleRadius * 2)) / (count - 1)
        var start = width / 5f + circleRadius

        (0 until count)
            .map { passwordList[it] }
            .forEach {
                canvas.drawCircle(start, y, circleRadius, if (it.isDigit()) circlePaint else ringPaint)
                start += interval + circleRadius * 2
            }
    }

    fun setAndNext(c: Char) {
        if (pos >= passwordList.size || !c.isDigit()) {
            return
        }
        passwordList[pos] = c
        invalidate()
        pos++

        listener?.onChange(pos)
    }

    fun clearAndPre() {
        if (pos <= 0) {
            return
        }
        pos--
        passwordList[pos] = ' '
        invalidate()

        listener?.onChange(pos)
    }

    fun clear() {
        pos = 0
        (0 until count).map { passwordList[it] = ' ' }
        invalidate()

        listener?.onChange(pos)
    }

    fun password(): String {
        val string = StringBuffer()
        for (char in passwordList) {
            string.append(char)
        }
        return string.toString()
    }

    fun setPasswordListener(listener: PasswordViewListener) {
        this.listener = listener
    }

    interface PasswordViewListener {
        fun onChange(pos: Int)
    }
}
