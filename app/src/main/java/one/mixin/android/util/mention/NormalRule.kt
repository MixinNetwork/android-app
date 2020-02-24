package one.mixin.android.util.mention

import java.util.regex.Matcher
import java.util.regex.Pattern
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.util.mention.syntax.node.TextNode
import one.mixin.android.util.mention.syntax.parser.ParseSpec
import one.mixin.android.util.mention.syntax.parser.Parser
import one.mixin.android.util.mention.syntax.parser.Rule

class NormalRule :
        Rule<MentionRenderContext, Node<MentionRenderContext>>(Pattern.compile("^[\\s\\S]+?(?=[^0-9A-Za-z\\s\\u00c0-\\uffff]|\\n| {2,}\\n|\\w+:\\S|$)")) {

        override fun parse(
            matcher: Matcher,
            parser: Parser<MentionRenderContext, in Node<MentionRenderContext>>
        ): ParseSpec<MentionRenderContext, Node<MentionRenderContext>> {
            return ParseSpec.createTerminal(TextNode(matcher.group()))
        }
    }
