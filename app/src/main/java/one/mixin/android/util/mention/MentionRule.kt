package one.mixin.android.util.mention

import android.graphics.Color
import java.util.regex.Matcher
import java.util.regex.Pattern
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.util.mention.syntax.parser.ParseSpec
import one.mixin.android.util.mention.syntax.parser.Parser
import one.mixin.android.util.mention.syntax.parser.Rule

class MentionRule :
    Rule<MentionRenderContext, Node<MentionRenderContext>>(Pattern.compile("^@[\\d]{4,}")) {

    companion object {
        val PRESS_COLOR = Color.parseColor("#0D94FC")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
    }

    override fun parse(
        matcher: Matcher,
        parser: Parser<MentionRenderContext, in Node<MentionRenderContext>>
    ): ParseSpec<MentionRenderContext, Node<MentionRenderContext>> {
        return ParseSpec.createTerminal(MentionNode(matcher.group(), LINK_COLOR, PRESS_COLOR))
    }
}
