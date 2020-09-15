package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import one.mixin.android.R
import one.mixin.android.websocket.ContactMessagePayload

open class ShareContactRenderer(context: Context) : ShareMessageRenderer {

    val contentView: View = LayoutInflater.from(context).inflate(R.layout.item_chat_contact_card, null)

    fun render(data: ContactMessagePayload) {

    }
}