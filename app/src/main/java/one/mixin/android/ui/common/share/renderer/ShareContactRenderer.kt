package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.date_wrapper.view.chat_time
import kotlinx.android.synthetic.main.item_chat_contact_card.view.*
import kotlinx.android.synthetic.main.item_chat_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

open class ShareContactRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_contact_card, null)

    fun render(user: User) {
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
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(contentView.chat_time, secretIcon, null, statusIcon, null)
        }
    }
}