package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import com.walletconnect.util.bytesToHex
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.hexStringToByteArray

class BarcodeView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val paint = Paint(ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width
        val height = height
        val itemWith = width / 93f
        val rectWidth = (itemWith * 5).toInt()
        val offset = (itemWith * 3).toInt()

        var left = 0
        for (color in colors) {
            paint.color = color
            canvas.drawRect(left.toFloat(), 0f, (left + rectWidth).toFloat(), height.toFloat(), paint)
            left += rectWidth + offset
        }
    }

    private var colors = mutableListOf<Int>()
    fun setData(hash: String) {
        val bytes = hash.hexStringToByteArray()
        val data = hash + bytes.sha3Sum256().slice(IntRange(0, 3)).toByteArray().bytesToHex()
        colors.clear()
        data.chunked(6).forEach { colorString ->
            val color = (colorString.toLong(16) or 0x00000000ff000000L).toInt()
            colors.add(color)
        }
        invalidate()
    }
}