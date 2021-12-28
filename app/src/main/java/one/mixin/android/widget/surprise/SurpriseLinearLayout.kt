package one.mixin.android.widget.surprise

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout

class SurpriseLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        setWillNotDraw(false)
    }

    private val snowflakesEffect = SnowflakesEffect()
    private val fireworksEffect = FireworksEffect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!christmas && !lunarNewYear) {
            return
        } else if (christmas) {
            snowflakesEffect.onDraw(this, canvas)
        } else if (lunarNewYear) {
            fireworksEffect.onDraw(this, canvas)
        }
    }

    companion object {
        // Christmas
        val christmas = System.currentTimeMillis() in 1640275200000..1641225600000

        // Lunar New Year
        val lunarNewYear = System.currentTimeMillis() in 1643558400000..1644163200000
    }
}
