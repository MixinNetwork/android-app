package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.item_chat_action_card.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.vo.AppCardData

open class ShareAppCardRenderer(context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_action_card, null)

    init {
        (contentView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
    }

    fun render(actionCard: AppCardData) {
        contentView.chat_icon.loadRoundImage(actionCard.iconUrl, 4.dp, R.drawable.holder_bot)
        contentView.chat_title.text = actionCard.title
        contentView.chat_description.text = actionCard.description
    }
}
