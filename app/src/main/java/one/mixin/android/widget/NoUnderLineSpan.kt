package one.mixin.android.widget

import android.text.TextPaint
import android.text.style.URLSpan
import android.view.View
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.conversation.adapter.MessageAdapter

class NoUnderLineSpan(url: String, private val onItemListener: MessageAdapter.OnItemListener? = null) :
    URLSpan(url) {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }

    override fun onClick(widget: View) {
        if (onItemListener == null) {
            widget.context.openUrl(url)
        } else {
            onItemListener.onUrlClick(url)
        }
    }
}
