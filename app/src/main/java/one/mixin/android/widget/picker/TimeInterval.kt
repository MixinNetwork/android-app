package one.mixin.android.widget.picker

import android.content.res.Resources
import one.mixin.android.MixinApplication
import one.mixin.android.R

val timeIntervalUnits by lazy {
    listOf(
        R.plurals.time_interval_unit_second,
        R.plurals.time_interval_unit_minute,
        R.plurals.time_interval_unit_hour,
        R.plurals.time_interval_unit_day,
        R.plurals.time_interval_unit_week,
    )
}

private fun timeString(
    resources: Resources,
    count: Long,
    index: Int,
) =
    resources.getQuantityString(
        timeIntervalUnits[index],
        count.toInt(),
    )

private fun intervalString(
    resources: Resources,
    interval: Long,
    unit: Long,
    index: Int,
): String {
    val count = interval / unit
    return "$count ${timeString(resources, count, index)}"
}

val numberList by lazy {
    listOf(
        (1..59).map { it.toString() }.toList(),
        (1..59).map { it.toString() }.toList(),
        (1..23).map { it.toString() }.toList(),
        (1..6).map { it.toString() }.toList(),
        (1..12).map { it.toString() }.toList(),
    )
}

internal fun toTimeInterval(
    resources: Resources,
    interval: Long,
): String =
    when {
        interval < 60L -> intervalString(resources, interval, 1L, 0)
        interval < 3600L -> intervalString(resources, interval, 60L, 1)
        interval < 86400L -> intervalString(resources, interval, 3600L, 2)
        interval < 604800L -> intervalString(resources, interval, 86400L, 3)
        else -> intervalString(resources, interval, 604800L, 4)
    }

fun toTimeInterval(interval: Long): String =
    toTimeInterval(MixinApplication.get().resources, interval)

fun toTimeIntervalIndex(interval: Long): Pair<Int, Int> =
    when {
        interval < 60L -> Pair(0, (interval / 1L).toInt() - 1)
        interval < 3600L -> Pair(1, (interval / 60L).toInt() - 1)
        interval < 86400L -> Pair(2, (interval / 3600L).toInt() - 1)
        interval < 604800L -> Pair(3, (interval / 86400L).toInt() - 1)
        else -> Pair(4, (interval / 604800L).toInt() - 1)
    }

fun Long?.getTimeInterval(): String {
    return when {
        this == null || this <= 0L -> MixinApplication.appContext.getString(R.string.Off)
        else -> toTimeInterval(this)
    }
}
