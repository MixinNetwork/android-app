package one.mixin.android.widget.surprise

import android.content.Context
import android.graphics.Canvas
import android.icu.util.ChineseCalendar
import android.util.AttributeSet
import android.widget.LinearLayout
import java.util.Calendar

class SurpriseLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    init {
        setWillNotDraw(false)
    }

    private val snowflakesEffect = SnowflakesEffect()
    private val fireworksEffect = FireworksEffect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentTimeMillis = System.currentTimeMillis()
        when {
            isChristmas(currentTimeMillis) -> snowflakesEffect.onDraw(this, canvas)
            isChineseNewYear(currentTimeMillis) -> fireworksEffect.onDraw(this, canvas)
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

        private fun isChristmas(timeInMillis: Long): Boolean {
            val calendar = Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
            }
            val year = calendar.get(Calendar.YEAR)
            val christmasBase = Calendar.getInstance().apply {
                clear()
                set(year, Calendar.DECEMBER, 25, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = christmasBase.timeInMillis - MILLIS_PER_DAY
            val end = christmasBase.timeInMillis + MILLIS_PER_DAY * 10
            return timeInMillis in start..end
        }

        private fun isChineseNewYear(timeInMillis: Long): Boolean {
            val current = ChineseCalendar().apply {
                this.timeInMillis = timeInMillis
            }
            val year = current.get(ChineseCalendar.EXTENDED_YEAR)
            val month = current.get(ChineseCalendar.MONTH)

            return when (month) {
                0 -> {
                    val newYearDay = ChineseCalendar().apply {
                        clear()
                        set(ChineseCalendar.EXTENDED_YEAR, year)
                        set(ChineseCalendar.MONTH, 0)
                        set(ChineseCalendar.DAY_OF_MONTH, 1)
                        set(ChineseCalendar.HOUR_OF_DAY, 0)
                        set(ChineseCalendar.MINUTE, 0)
                        set(ChineseCalendar.SECOND, 0)
                        set(ChineseCalendar.MILLISECOND, 0)
                    }
                    val start = newYearDay.timeInMillis
                    val end = start + 6 * MILLIS_PER_DAY
                    timeInMillis in start..end
                }
                11 -> {
                    val nextYearDay = ChineseCalendar().apply {
                        clear()
                        set(ChineseCalendar.EXTENDED_YEAR, year + 1)
                        set(ChineseCalendar.MONTH, 0)
                        set(ChineseCalendar.DAY_OF_MONTH, 1)
                        set(ChineseCalendar.HOUR_OF_DAY, 0)
                        set(ChineseCalendar.MINUTE, 0)
                        set(ChineseCalendar.SECOND, 0)
                        set(ChineseCalendar.MILLISECOND, 0)
                    }
                    val start = nextYearDay.timeInMillis - MILLIS_PER_DAY
                    val end = nextYearDay.timeInMillis
                    timeInMillis in start..end
                }
                else -> false
            }
        }
    }
}
