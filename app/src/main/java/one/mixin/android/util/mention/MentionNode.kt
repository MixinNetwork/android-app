package one.mixin.android.util.mention

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.View
import one.mixin.android.util.mention.syntax.node.Node
import one.mixin.android.widget.linktext.TouchableSpan

class MentionNode(val content: String, val normalTextColor: Int, val pressedTextColor: Int) : Node<MentionRenderContext>() {
        override fun render(builder: SpannableStringBuilder, renderContext: MentionRenderContext) {
            val number = content.substring(1)
            var name = renderContext.userMap[number]
            if (name == null) {
                builder.append("@$number")
                return
            }
            name = "@$name"
            val sp = SpannableString(name)
            val clickableSpan = object : TouchableSpan(normalTextColor, pressedTextColor, false) {
                override fun onClick(widget: View) {
                    renderContext.action(number)
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
