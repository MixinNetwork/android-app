package one.mixin.android.ui.landing

private const val MAX_SET_PIN_ERROR_MESSAGE_LENGTH = 320
private const val MAX_SET_PIN_ERROR_MESSAGE_LINES = 3

private val STACK_TRACE_LINE_PATTERN = Regex("""^\s*at\s+[\w.$]+\(""")
private val INLINE_STACK_TRACE_PATTERN = Regex("""\s+at\s+[\w.$]+\(.*$""")
private val R8_MAP_LOCATION_PATTERN = Regex("""\(r8-map-id-[^)]+\)""")

internal fun String.toSetPinErrorMessage(fallbackMessage: String): String {
    val message =
        lineSequence()
            .map { line ->
                line
                    .replace(INLINE_STACK_TRACE_PATTERN, "")
                    .replace(R8_MAP_LOCATION_PATTERN, "")
                    .trim()
            }
            .filter { line ->
                line.isNotBlank() && !STACK_TRACE_LINE_PATTERN.containsMatchIn(line)
            }
            .take(MAX_SET_PIN_ERROR_MESSAGE_LINES)
            .joinToString("\n")
            .trim()
            .takeWithEllipsis(MAX_SET_PIN_ERROR_MESSAGE_LENGTH)
    return message.ifBlank { fallbackMessage }
}

private fun String.takeWithEllipsis(maxLength: Int): String {
    if (length <= maxLength) return this
    return take(maxLength - 3).trimEnd() + "..."
}
