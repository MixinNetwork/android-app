package one.mixin.android.util.mention

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.View
import com.discord.simpleast.core.node.Node
import one.mixin.android.widget.linktext.TouchableSpan

class ClickNode(val content: String) : Node<MentionRenderContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MentionRenderContext) {
        val number = content.substring(1)
        var name = renderContext.userMap[number] ?: return
        name = "@$name"
        val sp = SpannableString(name)
        val clickableSpan = object : TouchableSpan(Color.RED, Color.BLUE, false) {
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