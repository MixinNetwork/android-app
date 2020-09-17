package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_text.view.*
import one.mixin.android.R
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.linktext.AutoLinkMode

open class ShareTextRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_text, null)

    init {
        contentView.chat_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
    }

    fun render(content: String, isNightMode: Boolean) {
        contentView.chat_name.isVisible = false
        contentView.chat_time.timeAgoClock(nowInUtc())
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = false) { statusIcon, secretIcon ->
            contentView.chat_flag.isVisible = statusIcon != null
            contentView.chat_flag.setImageDrawable(statusIcon)
            contentView.chat_secret.isVisible = secretIcon != null
        }
        contentView.chat_tv.text = content
        contentView.chat_layout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.chat_bubble_me_last
            } else {
                R.drawable.chat_bubble_me_last_night
            }
        )
    }
}
