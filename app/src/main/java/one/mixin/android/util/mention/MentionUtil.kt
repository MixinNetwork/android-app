package one.mixin.android.util.mention

import one.mixin.android.db.UserDao
import org.jetbrains.anko.collections.forEachReversedByIndex
import java.util.regex.Pattern

fun parseMention(text: String?, userDao: UserDao): String? {
    var result = text ?: return null
    val matcher = mentionPattern.matcher(text)
    val mentions = mutableListOf<MentionItem>()
    while (matcher.find()) {
        val name = matcher.group().replace(" ", "").replace("_", " ").replace("@", "")
        val user = userDao.findUSerByFullName(name)
        user?.let { u ->
            mentions.add(MentionItem(matcher.start(), matcher.end(), " @${u.identityNumber} "))
        }
    }

    mentions.forEachReversedByIndex { item ->
        result = result.replaceRange(item.start, item.end, item.content)
    }
    return result
}
fun processMentionMessageMention(text: String, userDao: UserDao): String {
    var result = text
    val matcher = mentionNumberPattern.matcher(text)
    val mentions = mutableListOf<MentionItem>()
    while (matcher.find()) {
        val identityNumber = matcher.group().replace(" ", "").replace("_", " ").replace("@", "")
        val user = userDao.findUSerByIdentityNumber(identityNumber)
        user?.let { u ->
            mentions.add(MentionItem(matcher.start(), matcher.end(), " @${u.fullName} "))
        }
    }

    mentions.forEachReversedByIndex { item ->
        result = result.replaceRange(item.start, item.end, item.content)
    }
    return result
}

class MentionItem(val start: Int, val end: Int, val content: String)

private val mentionPattern by lazy {
    Pattern.compile("(?:^|\\s|\$)@[\\S]+(?:\\s|\$)")
}

private val mentionNumberPattern by lazy {
    Pattern.compile("(?:^|\\s|\$)@[0-9]+(?:\\s|\$)")
}
