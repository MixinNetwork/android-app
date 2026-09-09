package one.mixin.android.widget.picker

import android.content.res.Resources
import one.mixin.android.MixinApplication
import one.mixin.android.R

const val INTERVAL_SECOND = 1L
const val INTERVAL_MINUTE = 60L
const val INTERVAL_HOUR = 3_600L
const val INTERVAL_DAY = 86_400L
const val INTERVAL_WEEK = 604_800L
const val INTERVAL_MONTH = 2_592_000L
const val INTERVAL_YEAR = 31_536_000L

val timeIntervalUnits by lazy {
    listOf(
        R.plurals.time_interval_unit_day,
        R.plurals.time_interval_unit_week,
        R.plurals.time_interval_unit_month,
        R.plurals.time_interval_unit_year,
    )
}

private val displayTimeIntervalUnits by lazy {
    listOf(
        R.plurals.time_interval_unit_second,
        R.plurals.time_interval_unit_minute,
        R.plurals.time_interval_unit_hour,
        R.plurals.time_interval_unit_day,
        R.plurals.time_interval_unit_week,
        R.plurals.time_interval_unit_month,
        R.plurals.time_interval_unit_year,
    )
}

private val pickerIntervals = listOf(INTERVAL_DAY, INTERVAL_WEEK, INTERVAL_MONTH, INTERVAL_YEAR)

private fun timeString(
    resources: Resources,
    count: Long,
    unitRes: Int,
) =
    resources.getQuantityString(
        unitRes,
        count.toInt(),
    )

private fun intervalString(
    resources: Resources,
    interval: Long,
    unit: Long,
    unitRes: Int,
): String {
    val count = interval / unit
    return "$count ${timeString(resources, count, unitRes)}"
}

val numberList by lazy {
    listOf(
        (1..6).map { it.toString() },
        (1..3).map { it.toString() },
        (1..6).map { it.toString() },
        listOf("1"),
    )
}

internal fun toTimeInterval(
    resources: Resources,
    interval: Long,
): String =
    when {
        interval > 0 && interval % INTERVAL_YEAR == 0L ->
            intervalString(resources, interval, INTERVAL_YEAR, displayTimeIntervalUnits[6])
        interval >= INTERVAL_MONTH && interval % INTERVAL_MONTH == 0L ->
            intervalString(resources, interval, INTERVAL_MONTH, displayTimeIntervalUnits[5])
        interval >= INTERVAL_WEEK && interval % INTERVAL_WEEK == 0L ->
            intervalString(resources, interval, INTERVAL_WEEK, displayTimeIntervalUnits[4])
        interval >= INTERVAL_DAY && interval % INTERVAL_DAY == 0L ->
            intervalString(resources, interval, INTERVAL_DAY, displayTimeIntervalUnits[3])
        interval >= INTERVAL_HOUR && interval % INTERVAL_HOUR == 0L ->
            intervalString(resources, interval, INTERVAL_HOUR, displayTimeIntervalUnits[2])
        interval >= INTERVAL_MINUTE && interval % INTERVAL_MINUTE == 0L ->
            intervalString(resources, interval, INTERVAL_MINUTE, displayTimeIntervalUnits[1])
        else ->
            intervalString(resources, interval, INTERVAL_SECOND, displayTimeIntervalUnits[0])
    }

fun toTimeInterval(interval: Long): String =
    toTimeInterval(MixinApplication.get().resources, interval)

fun toTimeIntervalIndex(interval: Long): Pair<Int, Int> {
    pickerIntervals.forEachIndexed { index, unit ->
        if (interval >= unit && interval % unit == 0L) {
            val count = (interval / unit).toInt()
            val max = numberList[index].size
            if (count in 1..max) {
                return Pair(index, count - 1)
            }
        }
    }
    return Pair(0, 0)
}

fun pickerIntervalSeconds(unitIndex: Int): Long =
    pickerIntervals.getOrElse(unitIndex) { INTERVAL_DAY }

fun Long?.getTimeInterval(): String {
    return when {
        this == null || this <= 0L -> MixinApplication.appContext.getString(R.string.Off)
        else -> toTimeInterval(this)
    }
}
