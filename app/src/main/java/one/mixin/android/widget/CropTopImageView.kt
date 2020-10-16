package one.mixin.android.widget

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CropTopImageView : AppCompatImageView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        if (drawable == null) {
            return super.setFrame(l, t, r, b)
        }
        val matrix: Matrix = imageMatrix
        val scaleFactor = (r - l) / drawable.intrinsicWidth.toFloat()
        matrix.setScale(scaleFactor, scaleFactor, 0f, 0f)
        imageMatrix = matrix
        return super.setFrame(l, t, r, b)
    }
}
