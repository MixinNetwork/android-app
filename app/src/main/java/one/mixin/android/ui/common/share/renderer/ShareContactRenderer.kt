package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_contact_card.view.*
import one.mixin.android.R
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

open class ShareContactRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_contact_card, null)

    init {
        (contentView.out_ll.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        contentView.chat_name.isVisible = false
    }

    fun render(user: User, isNightMode: Boolean) {
        contentView.avatar_iv.setInfo(
            user.fullName,
            user.avatarUrl,
            user.userId
        )
        contentView.name_tv.text = user.fullName
        contentView.id_tv.text = user.identityNumber
        contentView.chat_time.timeAgoClock(nowInUtc())
        user.showVerifiedOrBot(contentView.verified_iv, contentView.bot_iv)

        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            contentView.chat_flag.isVisible = statusIcon != null
            contentView.chat_flag.setImageDrawable(statusIcon)
            contentView.chat_secret.isVisible = secretIcon != null
        }

        contentView.chat_layout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.bill_bubble_me_last
            } else {
                R.drawable.bill_bubble_me_last_night
            }
        )
    }
}
