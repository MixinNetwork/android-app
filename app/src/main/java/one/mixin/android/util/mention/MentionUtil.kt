package one.mixin.android.util.mention

import one.mixin.android.db.UserDao
import one.mixin.android.vo.User
import org.jetbrains.anko.collections.forEachReversedByIndex
import timber.log.Timber
import java.util.regex.Pattern

fun parseMention(text: String?, userDao: UserDao): String? {
    var result = text ?: return null
    val matcher = mentionPattern.matcher(text)
    val users = mutableListOf<User>()
    val mentions = mutableListOf<MentionItem>()
    Timber.d("$text ${matcher.groupCount()}")
    while (matcher.find()) {
        val name = matcher.group().substring(1).replace("_", " ")
        val user = userDao.findUSerByFullNameSuspend(name)
        user?.let { u ->
            Timber.d(u.userId)
            mentions.add(MentionItem(matcher.start(), matcher.end(), u.identityNumber))
        }
    }

    mentions.forEachReversedByIndex { item ->
        Timber.d("${item.start}")
        Timber.d("${item.end}")
        Timber.d(item.content)
        result = result.replaceRange(item.start, item.end, item.content)
    }
    return result
}

class MentionItem(val start: Int, val end: Int, val content: String)

private val mentionPattern by lazy {
    Pattern.compile("(?:^|\\s|\$)@[\\S]+(?:\\s|\$)")
}
