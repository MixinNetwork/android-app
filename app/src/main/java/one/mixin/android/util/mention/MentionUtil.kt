package one.mixin.android.util.mention

import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import androidx.collection.arraySetOf
import java.util.Stack
import java.util.regex.Pattern
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.UserDao
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.util.mention.syntax.parser.Parser
import one.mixin.android.vo.MentionUser
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.User

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

fun deleteMentionEnd(editText: EditText) {
    val text = editText.text
    val matcher = mentionEndPattern.matcher(text)
    if (matcher.find()) {
        editText.setText(text.removeRange(matcher.start(), matcher.end()))
        editText.setSelection(editText.text.length)
    }
}

fun mentionReplace(source: Editable, user: User): CharSequence {

    return when (val index = source.lastIndexOf("@")) {
        -1 -> {
            source
        }
        else -> {
            source.apply {
                source.replace(index, length, "@${user.identityNumber} ")
                setSpan(ForegroundColorSpan(MENTION_COLOR), index, length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

fun parseMentionData(
    text: String,
    messageId: String,
    conversationId: String,
    userDao: UserDao,
    messageMentionDao: MessageMentionDao,
    userId: String
): Pair<List<MentionUser>?, Boolean> {
    val matcher = mentionNumberPattern.matcher(text)
    val numbers = arraySetOf<String>()
    while (matcher.find()) {
        val identityNumber = matcher.group().replace("@", "").replace(" ", "")
        numbers.add(identityNumber)
    }
    val account = Session.getAccount()
    val mentions = userDao.findUserByIdentityNumbers(numbers)
    if (mentions.isEmpty()) {
        return Pair(null, false)
    }
    val mentionData = GsonHelper.customGson.toJson(mentions)
    val mentionMe = userId != account?.userId && numbers.contains(account?.identity_number)
    messageMentionDao.insert(MessageMention(messageId, conversationId, mentionData, !mentionMe))
    return Pair(mentions, mentionMe)
}

fun rendMentionContent(
    text: String?,
    userMap: Map<String, String>?
): String? {
    if (text == null || userMap == null) return text
    val matcher = mentionNumberPattern.matcher(text)
    val textStack = Stack<ReplaceData>()
    while (matcher.find()) {
        val number = matcher.group().substring(1)
        val name = userMap[number] ?: continue
        textStack.push(ReplaceData(matcher.start(), matcher.end(), "@$name"))
    }
    var result = text
    while (!textStack.empty()) {
        val replaceData = textStack.pop()
        result = result?.replaceRange(replaceData.start, replaceData.end, replaceData.replace)
    }
    return result
}

class ReplaceData(val start: Int, val end: Int, val replace: String)

private val mentionEndPattern by lazy {
    Pattern.compile("(?:\\s|^)@\\S*\$")
}

val mentionNumberPattern: Pattern by lazy {
    Pattern.compile("@[\\d]{4,}")
}

val mentionMessageParser = Parser<MentionRenderContext, Node<MentionRenderContext>>()
    .addRule(MentionRule())
    .addRule(NormalRule())

val mentionConversationParser = Parser<MentionRenderContext, Node<MentionRenderContext>>()
    .addRule(MentionConversationRule())
    .addRule(NormalRule())

val MENTION_PRESS_COLOR by lazy { Color.parseColor("#665FA7E4") }
val MENTION_COLOR by lazy { Color.parseColor("#5FA7E4") }
