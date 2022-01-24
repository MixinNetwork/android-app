package one.mixin.android.extension

import java.util.UUID
import java.util.regex.Pattern

private val endAtPatter: Pattern by lazy { Pattern.compile("@(\\S*)(?<!\\s)\$") }
private val urlPatter: Pattern by lazy { Pattern.compile("[a-zA-z]+://[^\\s]*") }

fun String.endAt(): String? {
    val matcher = endAtPatter.matcher(this)
    return when {
        matcher.find() -> matcher.group(matcher.groupCount() - 1).substring(1)
        endsWith("@") -> ""
        else -> null
    }
}

fun String.removeEnd(remove: String?): String {
    if (remove != null) {
        return this.substring(0, length - remove.length)
    }
    return this
}

fun String.isUUID(): Boolean {
    return try {
        return UUID.fromString(this) != null
    } catch (exception: IllegalArgumentException) {
        false
    }
}

fun Long.fileSize(unit: Long = 0): String {
    var count = 0 / 1024
    var num = this.toFloat()
    while (count > 3 || num > 1024) {
        num /= 1024f
        count++
    }
    val unit = when (count) {
        1 -> "KB"
        2 -> "MB"
        3 -> "GB"
        else -> "Byte"
    }
    return String.format("%.2f %s", num, unit)
}

fun Long.fileUnit(): String {
    var count = 0
    var num = this.toFloat()
    while (count > 3 || num > 1024) {
        num /= 1024f
        count++
    }
    return when (count) {
        1 -> "KB"
        2 -> "MB"
        3 -> "GB"
        else -> "Byte"
    }
}

fun String.findLastUrl(): String? {
    val m = urlPatter.matcher(this)
    if (m.find()) {
        return m.group(0)
    }
    return null
}
