package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.isCompactMessageWindow
import one.mixin.android.extension.messageAreaWidth
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.windowHeight

abstract class MediaHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    protected val dp6 by lazy {
        itemView.context.dpToPx(6f)
    }

    protected val mediaWidth: Int by lazy {
        (itemView.context.messageAreaWidth() * 0.6).toInt().let { width ->
            if (itemView.context.isCompactMessageWindow()) width else width.coerceAtMost(itemView.context.dpToPx(480f))
        }
    }

    protected val mediaHeight: Int by lazy {
        (if (itemView.context.isCompactMessageWindow()) itemView.context.screenHeight() else itemView.context.windowHeight()) * 2 / 3
    }
}
