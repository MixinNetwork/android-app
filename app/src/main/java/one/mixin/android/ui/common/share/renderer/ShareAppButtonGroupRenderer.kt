package one.mixin.android.ui.common.share.renderer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_chat_action.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppButtonData
import one.mixin.android.widget.ActionButton
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.topPadding
import one.mixin.android.extension.dp

open class ShareAppButtonGroupRenderer(val context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_action, null)

    @SuppressLint("RestrictedApi")
    fun render(buttons: Array<AppButtonData>) {
        contentView.flow_layout.removeAllViews()
        for (b in buttons) {
            val button = ActionButton(context)
            button.setTextColor(
                try {
                    ColorUtil.parseColor(b.color.trim())
                } catch (e: Throwable) {
                    Color.BLACK
                }
            )
            button.setTypeface(null, Typeface.BOLD)
            button.text = b.label
            button.supportBackgroundTintList = ColorStateList.valueOf(contentView.context.colorFromAttribute(R.attr.bg_bubble))
            contentView.flow_layout.addView(button)
            (button.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 8.dp
            button.topPadding = 8.dp
            button.bottomPadding = 8.dp
            button.leftPadding = 12.dp
            button.rightPadding = 12.dp
        }
    }
}