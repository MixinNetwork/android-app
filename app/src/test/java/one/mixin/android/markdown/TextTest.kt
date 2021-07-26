package one.mixin.android.markdown

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TextTest {

    private val MARKDOWN_BOLD = Regex("(\\*\\*|__)(.*?)\\1")
    private val MARKDOWN_ITALIC = Regex("(\\*|_)(.*?)\\1")
    private val MARKDOWN_STRIKETHROUGH = Regex("(\\~\\~)(.*?)\\~\\~")
    private val MARKDOWN_INLINE = Regex("`(.*?)`")

    @Test
    fun testReplaceText() {
        val source = "123 __粗体__ ~~删除~~ *斜* **粗** 123 `code` _斜体_ mixin"
        val content = replace(source)
        assertEquals(content, "123 粗体 删除 斜 粗 123 code 斜体 mixin")
    }

    private fun replace(source: String): String {
        var content = source
        for (
        regex in arrayOf(
            MARKDOWN_BOLD,
            MARKDOWN_ITALIC,
            MARKDOWN_STRIKETHROUGH,
            MARKDOWN_INLINE
        )
        ) {
            content = content.replace(regex) { matchResult ->
                val str = matchResult.value
                val replaceSize = replaceSize(matchResult.value)
                if (str.length <= replaceSize) {
                    str
                } else {
                    str.substring(replaceSize, str.length - replaceSize)
                }
            }
        }
        println(content)
        return content
    }

    private fun replaceSize(str: String): Int {
        return if (str.startsWith("**") && str.endsWith("**")) {
            2
        } else if (str.startsWith("__") && str.endsWith("__")) {
            2
        } else if (str.startsWith("~~") && str.endsWith("~~")) {
            2
        } else {
            1
        }
    }
}
