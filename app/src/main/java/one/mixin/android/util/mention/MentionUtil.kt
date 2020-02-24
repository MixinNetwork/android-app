package one.mixin.android.util.mention

import android.widget.EditText
import java.util.regex.Pattern
import one.mixin.android.db.MentionMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.util.mention.syntax.parser.Parser
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
            "${source.substring(0, index)} @${user.identityNumber} "
        }
    }
}

fun getMentionData(
    text: String,
    messageId: String,
    conversationId: String,
    userDao: UserDao,
    mentionMessageDao: MentionMessageDao,
    send: Boolean = true
): String? {
    val matcher = mentionNumberPattern.matcher(text)
    val mentions = mutableListOf<MentionData>()
    var hasRead = true
    while (matcher.find()) {
        val identityNumber = matcher.group().replace("@", "").replace(" ", "")
        if (!send && identityNumber.isNotBlank() && identityNumber == Session.getAccount()?.identity_number) {
            hasRead = false
        }
        val user = userDao.findUSerByIdentityNumber(identityNumber)
        mentions.add(MentionData(identityNumber, user?.fullName))
    }
    if (mentions.isEmpty()) return null
    val mentionData = GsonHelper.customGson.toJson(mentions)
    mentionMessageDao.insert(MessageMention(messageId, conversationId, mentionData, hasRead))
    return mentionData
}

private val mentionEndPattern by lazy {
    Pattern.compile("(?:\\s|^)@\\S*\$")
}

val mentionNumberPattern: Pattern by lazy {
    Pattern.compile("@\\d+")
}

val mentionMessageParser = Parser<MentionRenderContext, Node<MentionRenderContext>>()
    .addRule(MentionRule())
    .addRule(NormalRule())

val mentionConversationParser = Parser<MentionRenderContext, Node<MentionRenderContext>>()
    .addRule(MentionConversationRule())
    .addRule(NormalRule())
