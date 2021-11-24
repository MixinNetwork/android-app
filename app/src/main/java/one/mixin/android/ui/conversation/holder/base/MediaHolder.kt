package one.mixin.android.ui.conversation.holder.base

import android.view.View
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.realSize
import one.mixin.android.extension.screenHeight

abstract class MediaHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    protected val dp6 by lazy {
        itemView.context.dpToPx(6f)
    }

    protected val mediaWidth by lazy {
        (itemView.context.realSize().x * 0.6).toInt()
    }

    protected val mediaHeight by lazy {
        (itemView.context.screenHeight() * 2 / 3)
    }
}
