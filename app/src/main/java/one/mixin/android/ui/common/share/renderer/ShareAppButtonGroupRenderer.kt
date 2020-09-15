package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import one.mixin.android.R
import one.mixin.android.vo.AppButtonData

open class ShareAppButtonGroupRenderer(context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_action, null)

    fun render(data: Array<AppButtonData>) {

    }
}