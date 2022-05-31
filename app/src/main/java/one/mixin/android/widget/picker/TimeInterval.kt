package one.mixin.android.widget.picker

import one.mixin.android.MixinApplication
import one.mixin.android.R

val timeIntervalUnits by lazy {
    listOf(
        R.plurals.time_interval_unit_second,
        R.plurals.time_interval_unit_minute,
        R.plurals.time_interval_unit_hour,
        R.plurals.time_interval_unit_day,
        R.plurals.time_interval_unit_week
    )
}

private fun timeString(isPlurals: Boolean, index: Int) =
    MixinApplication.get().resources.getQuantityString(
        timeIntervalUnits[index],
        if (isPlurals) 2 else 1
    )

val numberList by lazy {
    listOf(
        (1..59).map { it.toString() }.toList(),
        (1..59).map { it.toString() }.toList(),
        (1..23).map { it.toString() }.toList(),
        (1..6).map { it.toString() }.toList(),
        (1..12).map { it.toString() }.toList()
    )
}

fun toTimeInterval(interval: Long): String = when {
    interval < 60L -> "${interval / 1L} ${timeString(interval != 1L, 0)}"
    interval < 3600L -> "${interval / 60L} ${timeString(interval != 60L, 1)}"
    interval < 86400L -> "${interval / 3600L} ${timeString(interval != 3600L, 2)}"
    interval < 604800L -> "${interval / 86400L} ${timeString(interval != 86400L, 3)}"
    else -> "${interval / 604800L} ${timeString(interval != 604800L, 4)}"
}

fun toTimeIntervalIndex(interval: Long): Pair<Int, Int> = when {
    interval < 60L -> Pair(0, (interval / 1L).toInt() - 1)
    interval < 3600L -> Pair(1, (interval / 60L).toInt() - 1)
    interval < 86400L -> Pair(2, (interval / 3600L).toInt() - 1)
    interval < 604800L -> Pair(3, (interval / 86400L).toInt() - 1)
    else -> Pair(4, (interval / 604800L).toInt() - 1)
}

fun Long?.getTimeInterval(): String {
    return when {
        this == null || this <= 0L -> "off"
        else -> toTimeInterval(this)
    }
}
