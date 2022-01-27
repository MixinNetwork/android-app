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

enum class FileSizeUnit(val value: Int) {
    BYTE(3), KB(2), MB(1), GB(0);

    companion object {
        fun fromValue(value: Int): FileSizeUnit = FileSizeUnit.values().first { value == it.value }
    }
}

fun Long.fileSize(unit: FileSizeUnit = FileSizeUnit.BYTE): String {
    var count = 0
    var num = this.toFloat()
    while (count > unit.value || num >= 1024) {
        num /= 1024f
        count++
    }
    return String.format("%.2f %s", num, FileSizeUnit.fromValue(unit.value - count).name)
}

fun Long.fileUnit(unit: FileSizeUnit = FileSizeUnit.BYTE): String {
    var count = 0
    var num = this.toFloat()
    while (count > unit.value || num >= 1024) {
        num /= 1024f
        count++
    }
    return FileSizeUnit.fromValue(unit.value - count).name
}

fun String.findLastUrl(): String? {
    val m = urlPatter.matcher(this)
    if (m.find()) {
        return m.group(0)
    }
    return null
}
