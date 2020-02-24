package one.mixin.android.util.mention

import android.text.SpannableStringBuilder
import one.mixin.android.util.mention.syntax.node.Node

class MentionConversationNode(val content: String) : Node<MentionRenderContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MentionRenderContext) {
        val number = content.substring(1)
        val name = renderContext.userMap[number] ?: number
        builder.append("@$name")
    }

    override fun toString() = "${javaClass.simpleName}[${getChildren()?.size}]: $content"
}
