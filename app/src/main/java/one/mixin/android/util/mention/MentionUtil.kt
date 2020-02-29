package one.mixin.android.util.mention

import android.graphics.Color
import android.widget.EditText
import androidx.collection.arraySetOf
import java.util.Stack
import java.util.regex.Pattern
import one.mixin.android.db.MentionMessageDao
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

fun mentionReplace(source: String, user: User): String {
    return when (val index = source.lastIndexOf("@")) {
        -1 -> {
            source
        }
        0 -> {
            "@${user.identityNumber} "
        }
        else -> {
            "${source.substring(0, index)}@${user.identityNumber} "
        }
    }
}

fun parseMentionData(
    text: String,
    messageId: String,
    conversationId: String,
    userDao: UserDao,
    mentionMessageDao: MentionMessageDao,
    ignore: Boolean = true
): List<MentionUser>? {
    val matcher = mentionNumberPattern.matcher(text)
    val numbers = arraySetOf<String>()
    var hasRead = true
    while (matcher.find()) {
        val identityNumber = matcher.group().replace("@", "").replace(" ", "")
        if (!ignore && identityNumber.isNotBlank() && identityNumber == Session.getAccount()?.identity_number) {
            hasRead = false
        }
        numbers.add(identityNumber)
    }
    val mentions = userDao.findUserByIdentityNumbers(numbers)
    if (mentions.isEmpty()) return null
    val mentionData = GsonHelper.customGson.toJson(mentions)
    mentionMessageDao.insert(MessageMention(messageId, conversationId, mentionData, hasRead))
    return mentions
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
