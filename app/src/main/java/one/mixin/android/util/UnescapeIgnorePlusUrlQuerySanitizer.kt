package one.mixin.android.util

import android.net.UrlQuerySanitizer

class UnescapeIgnorePlusUrlQuerySanitizer : UrlQuerySanitizer() {
    override fun unescape(string: String): String {
        // Early exit if no escaped characters.
        val firstEscape = string.indexOf('%')
        if (firstEscape < 0) {
            return string
        }

        val length = string.length

        val stringBuilder = StringBuilder(length)
        stringBuilder.append(string.substring(0, firstEscape))
        var i = firstEscape
        while (i < length) {
            var c = string[i]
            if (c == '%' && i + 2 < length) {
                val c1 = string[i + 1]
                val c2 = string[i + 2]
                if (isHexDigit(c1) && isHexDigit(c2)) {
                    c = (decodeHexDigit(c1) * 16 + decodeHexDigit(c2)).toChar()
                    i += 2
                }
            }
            stringBuilder.append(c)
            i++
        }
        return stringBuilder.toString()
    }
}
