package one.mixin.android.util.mention

import com.discord.simpleast.core.node.Node
import com.discord.simpleast.core.parser.Parser
import one.mixin.android.vo.User
import java.util.regex.Pattern

fun mentionDisplay(string: CharSequence): Boolean {
    val matcher = mentionEndPattern.matcher(string)
    return matcher.find()
}

fun mentionEnd(string: String): String? {
    val matcher = mentionEndPattern.matcher(string)
    return if (matcher.find()) {
        matcher.group().replace(" ", "").replace("@", "")
    } else {
        null
    }
}

fun mentionReplace(source: String, user: User): String {
    return when (val index = source.lastIndexOf("@")) {
        -1 -> {
            source
        }
        0 -> {
            "@${user.identityNumber} "
        }
        else -> {
            "${source.substring(0, index)} @${user.identityNumber} "
        }
    }
}

private val mentionEndPattern by lazy {
    Pattern.compile("(?:\\s|^)@.*\$")
}

val mentionParser = Parser<MentionRenderContext, Node<MentionRenderContext>>()
    .addRule(UserRule())