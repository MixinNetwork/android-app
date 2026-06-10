package one.mixin.android.widget.linktext

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity

class GroupTextView(context: Context, attrs: AttributeSet?) : AutoLinkTextView(context, attrs) {
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        gravity =
            if (lineCount > 1) {
                Gravity.START
            } else {
                Gravity.CENTER
            }
    }

    private var expand = true

    fun expand() {
        if (!expand) {
            maxLines = Int.MAX_VALUE
            expand = true
        }
    }

    fun collapse() {
        if (expand) {
            maxLines = 2
            expand = false
            super.scrollTo(0, 0)
        }
    }

    init {
        maxLines = 2
        minLines = 2
    }

    override fun scrollTo(
        x: Int,
        y: Int,
    ) {
        if (expand) {
            super.scrollTo(x, y)
        }
    }
}
