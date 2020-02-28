package one.mixin.android.ui.conversation.holder

import android.view.View

abstract class BaseMentionHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    protected var attachAction: (() -> Unit)? = null

    fun onViewAttachedToWindow() {
        attachAction?.invoke()
        attachAction = null
    }
}
