package one.mixin.android.util

import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.widget.TextView
import java.text.Normalizer
import java.util.regex.Pattern

class QueryHighlighter(
    private var highlightStyle: CharacterStyle = StyleSpan(Typeface.BOLD),
    private var queryNormalizer: QueryNormalizer = QueryNormalizer.FOR_SEARCH,
    private var mode: Mode = Mode.CHARACTERS
) {
    enum class Mode { CHARACTERS, WORDS }

    class QueryNormalizer(private var normalizer: (source: CharSequence) -> CharSequence = { s -> s }) {
        operator fun invoke(source: CharSequence) = normalizer(source)

        companion object {
            val NONE: QueryNormalizer = QueryNormalizer()

            val CASE: QueryNormalizer = QueryNormalizer { source ->
                if (TextUtils.isEmpty(source)) {
                    source
                } else source.toString().uppercase()
            }

            private val PATTERN_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}")

            private val PATTERN_NON_LETTER_DIGIT_TO_SPACES = Pattern.compile("[^\\p{L}\\p{Nd}]")
            val FOR_SEARCH: QueryNormalizer = QueryNormalizer { searchTerm ->
                var result = Normalizer.normalize(searchTerm, Normalizer.Form.NFD)
                result = PATTERN_DIACRITICS.matcher(result).replaceAll("")
                result = PATTERN_NON_LETTER_DIGIT_TO_SPACES.matcher(result).replaceAll(" ")
                result.lowercase()
            }
        }
    }

    private fun apply(text: CharSequence?, wordPrefix: CharSequence?): CharSequence? {
        if (text == null || wordPrefix == null) {
            return text
        }
        val normalizedText = queryNormalizer(text)
        val normalizeWordPrefix = queryNormalizer(wordPrefix)
        val index = indexOfQuery(normalizedText, normalizeWordPrefix)
        return if (index != -1 && normalizeWordPrefix.isNotEmpty()) {
            SpannableString(text).apply {
                setSpan(highlightStyle, index, index + normalizeWordPrefix.length, 0)
            }
        } else {
            text
        }
    }

    fun apply(view: TextView, text: CharSequence?, query: CharSequence?) {
        view.text = apply(text, query)
    }

    private fun indexOfQuery(text: CharSequence?, query: CharSequence?): Int {
        if (query == null || text == null) {
            return -1
        }
        val textLength = text.length
        val queryLength = query.length
        if (queryLength == 0 || textLength < queryLength) {
            return -1
        }
        for (i in 0..textLength - queryLength) {
            if (mode == Mode.WORDS && i > 0 && text[i - 1] != ' ') {
                continue
            }
            var j = 0
            while (j < queryLength) {
                if (text[i + j] != query[j]) {
                    break
                }
                j++
            }
            if (j == queryLength) {
                return i
            }
        }
        return -1
    }
}
