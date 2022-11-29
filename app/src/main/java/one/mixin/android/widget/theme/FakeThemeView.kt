package one.mixin.android.widget.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

internal class FakeThemeView(context: Context) : View(context) {
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val bitmap = this.bitmap
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }
}
