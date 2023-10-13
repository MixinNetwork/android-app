package one.mixin.android.util.mention

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.util.markdown.MarkwonUtil.Companion.simpleMarkwon
import org.commonmark.node.Node

class MentionTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {
    var mentionRenderContext: MentionRenderContext? = null

    override fun setText(text: CharSequence?, type: BufferType) {
        if (text.isNullOrBlank()) {
            super.setText(text, type)
            return
        } else {
            val sp = SpannableStringBuilder()
            sp.append(SpannableString(text))
            val ctx = mentionRenderContext
            if (ctx != null) {
                super.setText(renderMention(sp), type)
            } else {
                super.setText(sp, type)
            }
        }
    }

    private fun renderMarkdown(sp: SpannableStringBuilder, node: Node) {
        sp.append(simpleMarkwon.render(node))
        if (node.next != null) {
            renderMarkdown(sp, node.next)
        }
    }

    private fun renderMention(
        text: CharSequence,
    ): CharSequence {
        val str = SpannableStringBuilder(text)
        val mentionRenderContext = this.mentionRenderContext ?: return text
        val matcher = mentionNumberPattern.matcher(text)
        var offset = 0
        while (matcher.find()) {
            val name = mentionRenderContext.userMap[matcher.group().substring(1)] ?: continue
            str.replace(matcher.start() + 1 - offset, matcher.end() - offset, name)
            offset += ((matcher.group().length - 1) - name.length)
        }
        return str
    }
}
