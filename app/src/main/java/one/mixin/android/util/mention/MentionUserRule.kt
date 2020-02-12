package one.mixin.android.util.mention

import one.mixin.android.util.mention.core.node.Node
import one.mixin.android.util.mention.core.parser.ParseSpec
import one.mixin.android.util.mention.core.parser.Parser
import one.mixin.android.util.mention.core.parser.Rule
import java.util.regex.Matcher

class MentionUserRule<S> : Rule<MentionRenderContext, Node<MentionRenderContext>, S>(mentionNumberPattern) {

    override fun parse(
        matcher: Matcher,
        parser: Parser<MentionRenderContext, in Node<MentionRenderContext>, S>,
        state: S
    ): ParseSpec<MentionRenderContext, Node<MentionRenderContext>, S> {
        return ParseSpec.createTerminal(MentionClickNode(matcher.group()), state)
    }
}