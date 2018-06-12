package one.mixin.android.ui.conversation.holder

import android.view.View
import one.mixin.android.extension.displayHeight
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.dpToPx

abstract class MediaHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    protected val dp6 by lazy {
        itemView.context.dpToPx(6f)
    }

    protected val mediaWidth by lazy {
        (itemView.context.displaySize().x * 0.6).toInt()
    }

}
