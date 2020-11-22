package one.mixin.android.ui.setting.diagnosis

import java.io.Serializable

class ResultBean(var title: String, var param: Any) : Serializable

fun ResultBean.formatString(): String {
    val json = StringBuilder()
    var indentString = ""

    if (param is String) {
        val text: String = param as String
        for (i in text.indices) {
            when (val letter: Char = text[i]) {
                '{', '[' -> {
                    json.append(
                        """
                    $indentString$letter
                    
                        """.trimIndent()
                    )
                    indentString += "\t"
                    json.append(indentString)
                }
                '}', ']' -> {
                    indentString = indentString.replaceFirst("\t".toRegex(), "")
                    json.append(
                        """
                    
                    $indentString$letter
                        """.trimIndent()
                    )
                }
                ',' -> json.append(
                    """
                $letter
                $indentString
                    """.trimIndent()
                )
                else -> json.append(letter)
            }
        }
    }
    return json.toString()
}
