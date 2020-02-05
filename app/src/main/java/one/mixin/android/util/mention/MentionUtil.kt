package one.mixin.android.util.mention

import java.util.regex.Pattern
import one.mixin.android.db.MentionMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.vo.MentionMessage
import one.mixin.android.vo.User
import org.jetbrains.anko.collections.forEachReversedByIndex
import timber.log.Timber

fun parseMention(
    text: String?,
    messageId: String,
    conversationId: String,
    userDao: UserDao,
    mentionMessageDao: MentionMessageDao
): String? {
    var result = text ?: return null
    val matcher = mentionPattern.matcher(text)
    val mentions = mutableListOf<MentionItem>()
    while (matcher.find()) {
        val name = matcher.group().replace(" ", "").replace("\b", " ").replace("@", "")
        Timber.d(name)
        val user = userDao.findUSerByFullName(name)
        user?.let { u ->
            mentions.add(MentionItem(matcher.start(), matcher.end(), " @${u.identityNumber} "))
        }
        mentionMessageDao.insert(MentionMessage(messageId, conversationId, user?.userId, user?.fullName, true))
    }

    mentions.forEachReversedByIndex { item ->
        result = result.replaceRange(item.start, item.end, item.content)
    }
    return result
}

fun processMentionMessageMention(
    text: String,
    messageId: String,
    conversationId: String,
    userDao: UserDao,
    mentionMessageDao: MentionMessageDao,
    handlerMessage: (String) -> Unit
) {
    var result = text
    val matcher = mentionNumberPattern.matcher(text)
    val users = mutableListOf<User?>()
    val mentions = mutableListOf<MentionItem>()
    while (matcher.find()) {
        val identityNumber = matcher.group().replace("@", "").replace(" ", "")
        val user = userDao.findUSerByIdentityNumber(identityNumber)
        user?.let { u ->
            mentions.add(MentionItem(matcher.start(), matcher.end(), " @${u.fullName?.replace(" ", "\b")} "))
        }
        users.add(user)
    }

    mentions.forEachReversedByIndex { item ->
        result = result.replaceRange(item.start, item.end, item.content)
    }
    handlerMessage(result)
    users.forEach { u ->
        mentionMessageDao.insert(MentionMessage(messageId, conversationId, u?.userId, u?.fullName))
    }
}

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

fun mentionReplace(source: String, fullName: String): String {
    return when (val index = source.lastIndexOf("@")) {
        -1 -> source
        0 -> "@${fullName.replace(" ", "\b")}"
        else -> "${source.substring(0, index)} @${fullName.replace(" ", "\b")} "
    }
}

class MentionItem(val start: Int, val end: Int, val content: String)

private val mentionPattern by lazy {
    Pattern.compile("@(\\S|\\b)+(?:\\s|\$)")
}

private val mentionNumberPattern by lazy {
    Pattern.compile("@[0-9]+(?:\\s|\$)")
}

private val mentionEndPattern by lazy {
    Pattern.compile("(?:\\s|^)@(\\S)*\$")
}
