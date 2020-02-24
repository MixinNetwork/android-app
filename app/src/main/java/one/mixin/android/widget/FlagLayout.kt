package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_flag.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.translationY

class FlagLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {
    var bottomFlag = false
        set(value) {
            if (field != value) {
                down_flag_layout.isVisible = value
                field = value
                update()
            }
        }
    var bottomCountFlag = false
        set(value) {
            if (field != value) {
                down_unread.isVisible = value
                field = value
                update()
            }
        }
    var mentionCount = 0
        set(value) {
            if (field != value) {
                mention_flag_layout.isVisible = value != 0
                field = value
                mention_count.text = "$field"
                update()
            }
        }

    private fun update() {
        if (!bottomCountFlag && !bottomFlag && mentionCount == 0) {
            hide()
        } else {
            show(100)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_flag, this, true)
    }

    private fun show(duration: Long = 100) {
        if (this.translationY != 0f)
            translationY(0f, duration)
    }

    private fun hide() {
        if (this.translationY == 0f)
            translationY(context.dpToPx(130f).toFloat(), 100)
    }
}
