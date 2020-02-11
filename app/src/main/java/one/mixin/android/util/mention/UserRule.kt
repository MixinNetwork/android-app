package one.mixin.android.util.mention

import com.discord.simpleast.core.node.Node
import com.discord.simpleast.core.parser.ParseSpec
import com.discord.simpleast.core.parser.Parser
import com.discord.simpleast.core.parser.Rule
import java.util.regex.Matcher
import java.util.regex.Pattern

class UserRule : Rule<MentionRenderContext, Node<MentionRenderContext>>(Pattern.compile("^@\\d+")) {

    override fun parse(
        matcher: Matcher,
        parser: Parser<MentionRenderContext, in Node<MentionRenderContext>>
    ): ParseSpec<MentionRenderContext, Node<MentionRenderContext>> {
        return ParseSpec.createTerminal(ClickNode(matcher.group()))
    }
}