package one.mixin.android.widget.viewpager2

import android.view.View
import androidx.viewpager2.widget.ViewPager2

private const val DEFAULT_CENTER = .5f

class ScaleTransformer : ViewPager2.PageTransformer {
    private val minScale = .85f

    override fun transformPage(view: View, position: Float) {
        val pageWidth: Int = view.width
        val pageHeight: Int = view.height
        view.pivotY = (pageHeight shr 1).toFloat()
        view.pivotX = (pageWidth shr 1).toFloat()
        if (position < -1) {
            view.scaleX = minScale
            view.scaleY = minScale
            view.pivotX = pageWidth.toFloat()
        } else if (position <= 1) {
            if (position < 0) {
                val scaleFactor: Float = (1 + position) * (1 - minScale) + minScale
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                view.pivotX = pageWidth * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
            } else {
                val scaleFactor: Float = (1 - position) * (1 - minScale) + minScale
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                view.pivotX = pageWidth * ((1 - position) * DEFAULT_CENTER)
            }
        } else {
            view.pivotX = 0f
            view.scaleX = minScale
            view.scaleY = minScale
        }
    }
}
