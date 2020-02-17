package one.mixin.android.util.mention

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.View
import com.discord.simpleast.core.node.Node
import one.mixin.android.ui.conversation.holder.BaseViewHolder.Companion.HIGHLIGHTED
import one.mixin.android.ui.conversation.holder.BaseViewHolder.Companion.LINK_COLOR
import one.mixin.android.widget.linktext.TouchableSpan

class MentionClickNode(val content: String) : Node<MentionRenderContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MentionRenderContext) {
        val number = content.substring(1)
        val data = renderContext.userMap[number]
        if (data == null || data.fullName.isNullOrBlank()) {
            builder.append(content)
            return
        }
        val name = "@${data.fullName}"
        val sp = SpannableString(name)
        val clickableSpan = object : TouchableSpan(LINK_COLOR, HIGHLIGHTED, false) {
            override fun onClick(widget: View) {
                // renderContext.action(data.userId)
            }
        }

        sp.setSpan(
            clickableSpan,
            0, name.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(sp)
    }

    override fun toString() = "${javaClass.simpleName}[${getChildren()?.size}]: $content"
}