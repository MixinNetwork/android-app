package one.mixin.android.extension

import android.content.Context
import android.text.format.DateUtils
import one.mixin.android.R
import one.mixin.android.util.TimeCache
import one.mixin.android.util.isCurrChinese
import one.mixin.android.util.reportException
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import timber.log.Timber
import java.util.Date

private const val weekPatternEn = "E, d MMM"
private const val weekPatternCn = "MM 月 d 日 E"
private const val yearPatternEn = "E, d MMM, yyyy"
private const val yearPatternCn = "yyyy 年 MM 月 d 日 E"

fun nowInUtc() = Instant.now().toString()

fun nowInUtcNano(): Long {
    val inst = Instant.now()
    var time = inst.epochSecond
    time *= 1000000000L
    time += inst.nano.toLong()
    return time
}

fun oneWeekAgo() =
    Instant.ofEpochMilli(System.currentTimeMillis() - 60 * 60 * 1000 * 24 * 7).toString()

fun thirtyDaysAgo(): String {
    return ZonedDateTime.now().minusDays(30).toInstant().toString()
}

private const val DAY_DURATION = 24 * 3600 * 1000

private fun localeZone() = ZoneId.systemDefault()

fun String.date(): String {
    var date = TimeCache.singleton.getDate(this)
    if (date == null) {
        val time = ZonedDateTime.parse(this).toOffsetDateTime()
        date = time.format(DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(localeZone()))
        TimeCache.singleton.putDate(this, date)
    }
    return date as String
}

fun localDateString(time: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(localeZone()).format(Instant.ofEpochMilli(time))

fun String.within24Hours() = withinTime((60 * 60 * 1000 * 24).toLong())

fun String.within6Hours() = withinTime((60 * 60 * 1000 * 6).toLong())

fun String.withinTime(hours: Long): Boolean {
    val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone())
    val offset = System.currentTimeMillis() - date.toInstant().toEpochMilli()
    return offset < hours
}

fun String.timeAgo(context: Context): String {
    val localeZone = localeZone()
    val today =
        ZonedDateTime.of(
            ZonedDateTime.now().toLocalDate(),
            LocalTime.MIN,
            localeZone.normalized(),
        )
    var timeAgo = TimeCache.singleton.getTimeAgo(this + today)
    if (timeAgo == null) {
        val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone)
        val todayMilli = today.toInstant().toEpochMilli()
        val offset = todayMilli - date.toInstant().toEpochMilli()
        timeAgo =
            when {
                (todayMilli <= date.toInstant().toEpochMilli()) -> date.format(DateTimeFormatter.ofPattern("HH:mm").withZone(localeZone))
                (offset < 7 * DAY_DURATION) -> {
                    when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> context.getString(R.string.Monday)
                        DayOfWeek.TUESDAY -> context.getString(R.string.Tuesday)
                        DayOfWeek.WEDNESDAY -> context.getString(R.string.Wednesday)
                        DayOfWeek.THURSDAY -> context.getString(R.string.Thursday)
                        DayOfWeek.FRIDAY -> context.getString(R.string.Friday)
                        DayOfWeek.SATURDAY -> context.getString(R.string.Saturday)
                        else -> context.getString(R.string.Sunday)
                    }
                }
                else -> {
                    date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(localeZone))
                }
            }
        TimeCache.singleton.putTimeAgo(this, timeAgo)
    }
    return timeAgo as String
}

fun String.timeAgoDate(context: Context): String {
    val localeZone = localeZone()
    val today =
        ZonedDateTime.of(
            ZonedDateTime.now().toLocalDate(),
            LocalTime.MIN,
            localeZone.normalized(),
        )
    val todayMilli = today.toInstant().toEpochMilli()
    var timeAgoDate = TimeCache.singleton.getTimeAgoDate(this + today)
    if (timeAgoDate == null) {
        val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone)
        timeAgoDate =
            when {
                (todayMilli <= date.toInstant().toEpochMilli()) -> context.getString(R.string.Today)
                (today.year == date.year) -> {
                    date.format(
                        DateTimeFormatter.ofPattern(
                            if (isCurrChinese()) {
                                weekPatternCn
                            } else {
                                weekPatternEn
                            },
                        ).withZone(localeZone),
                    )
                }
                else -> {
                    date.format(
                        DateTimeFormatter.ofPattern(
                            if (isCurrChinese()) {
                                yearPatternCn
                            } else {
                                yearPatternEn
                            },
                        ).withZone(localeZone),
                    )
                }
            }
        TimeCache.singleton.putTimeAgoDate(this + today, timeAgoDate)
    }
    return timeAgoDate as String
}

fun String.timeAgoDay(patten: String = "dd/MM/yyyy"): String {
    val localeZone = localeZone()
    val today =
        ZonedDateTime.of(
            ZonedDateTime.now().toLocalDate(),
            LocalTime.MIN,
            localeZone.normalized(),
        ).toInstant().toEpochMilli()
    var timeAgoDate = TimeCache.singleton.getTimeAgoDate(this + today)
    if (timeAgoDate == null) {
        val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone)
        try {
            timeAgoDate = date.format(DateTimeFormatter.ofPattern(patten).withZone(localeZone))
            TimeCache.singleton.putTimeAgoDate(this + today, timeAgoDate)
        } catch (e: IllegalArgumentException) {
            reportException("TimeExtension timeAgoDay()", e)
            Timber.w(e)
        }
    }
    return timeAgoDate as? String ?: ""
}

private val dateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm")
}

fun String.timeFormat(): String {
    val localeZone = localeZone()
    val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone)
    return requireNotNull(date.format(dateTimeFormatter.withZone(localeZone)))
}

fun String.toUtcTime(): String {
    return ZonedDateTime.parse(this, dateTimeFormatter.withZone(localeZone())).toString()
}

fun String.lateOneHours(): Boolean {
    val offset = ZonedDateTime.now().toInstant().toEpochMilli() - ZonedDateTime.parse(this).withZoneSameInstant(localeZone()).toInstant().toEpochMilli()
    return offset > 3600000L
}

fun String.hashForDate(): Long {
    var hashForDate = TimeCache.singleton.getHashForDate(this)
    if (hashForDate == null) {
        val date = ZonedDateTime.parse(this).toOffsetDateTime()
        val time = date.format(DateTimeFormatter.ofPattern("yyyMMdd").withZone(localeZone()))
        hashForDate = time.hashCode().toLong()
        TimeCache.singleton.putHashForDate(this, hashForDate)
    }

    return hashForDate as Long
}

fun String.timeAgoClock(): String {
    var timeAgoClock = TimeCache.singleton.getTimeAgoClock(this)
    if (timeAgoClock == null) {
        val date = ZonedDateTime.parse(this).toOffsetDateTime()
        val time = date.format(DateTimeFormatter.ofPattern("HH:mm").withZone(localeZone()))
        timeAgoClock =
            if (time.startsWith("0")) {
                time.substring(1)
            } else {
                time
            }
        TimeCache.singleton.putTimeAgoClock(this, timeAgoClock)
    }
    return timeAgoClock as String
}

fun isSameDay(
    time: String?,
    otherTime: String?,
): Boolean {
    if (time == null || otherTime == null) {
        return false
    }
    val date = time.hashForDate()
    val otherDate = otherTime.hashForDate()
    return date == otherDate
}

fun String.fullDate(): String {
    val date = ZonedDateTime.parse(this).toOffsetDateTime()
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(localeZone())) as String
}

fun Long.fullTime(): String {
    return Instant.ofEpochSecond(this).atZone(localeZone()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}

fun Long.toUtcTime(): String {
    return Instant.ofEpochMilli(this).toString()
}

fun String.localTime(): String {
    val date = ZonedDateTime.parse(this).toOffsetDateTime()
    return date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd, hh:mm a").withZone(localeZone())) as String
}

fun String.dayTime(): String {
    val date = ZonedDateTime.parse(this).toOffsetDateTime()
    return date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(localeZone())) as String
}

fun String.createAtToLong(): Long {
    val date = ZonedDateTime.parse(this).withZoneSameInstant(localeZone())
    return date.toInstant().toEpochMilli()
}

fun String.getRFC3339Nano(): String {
    val date = ZonedDateTime.parse(this).toOffsetDateTime()
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(localeZone()))
}

fun Long.getRelativeTimeSpan(): String {
    val now = Date().time
    val time =
        DateUtils.getRelativeTimeSpanString(
            this,
            now,
            when {
                ((now - this) < 60000L) -> DateUtils.SECOND_IN_MILLIS
                ((now - this) < 3600000L) -> DateUtils.MINUTE_IN_MILLIS
                ((now - this) < 86400000L) -> DateUtils.HOUR_IN_MILLIS
                else -> DateUtils.DAY_IN_MILLIS
            },
        )
    return time.toString()
}

fun String.getRelativeTimeSpan(): String {
    val createTime = ZonedDateTime.parse(this).toOffsetDateTime().toEpochSecond() * 1000L
    return createTime.getRelativeTimeSpan()
}

fun currentTimeSeconds() = System.currentTimeMillis() / 1000

fun String.toSeconds() = ZonedDateTime.parse(this).toOffsetDateTime().toEpochSecond()

fun getTimeMonthsAgo(x: Int): Instant {
    val startOfDay =
        ZonedDateTime.now(ZoneId.systemDefault())
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
    return startOfDay.minusMonths(x.toLong()).toInstant()
}
