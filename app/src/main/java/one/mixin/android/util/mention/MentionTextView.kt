package one.mixin.android.util.mention

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MentionTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {
    var mentionRenderContext: MentionRenderContext? = null

    override fun setText(text: CharSequence?, type: BufferType) {
        if (text.isNullOrBlank() || mentionRenderContext == null) {
            super.setText(text, type)
            return
        } else {
            super.setText(renderMention(text), type)
        }
    }

    private fun renderMention(
        text: CharSequence
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
