package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageStatus

abstract class BaseViewHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    companion object {
        val colors: IntArray = MixinApplication.appContext.resources.getIntArray(R.array.name_colors)
        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
    }

    protected val dp10 by lazy {
        MixinApplication.appContext.dpToPx(10f)
    }
    private val dp12 by lazy {
        MixinApplication.appContext.dpToPx(12f)
    }

    abstract fun chatLayout(isMe: Boolean, isLast: Boolean)

    protected val botIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_bot)?.also {
            it.setBounds(0, 0, dp12, dp12)
        }
    }

    val meId by lazy {
        Session.getAccountId()
    }

    protected fun setStatusIcon(isMe: Boolean, status: String, setIcon: (Drawable?) -> Unit,
        hideIcon: () -> Unit, isWihte: Boolean = false) {
        if (isMe) {
            val drawable: Drawable? =
                when (status) {
                    MessageStatus.SENDING.name ->
                        AppCompatResources.getDrawable(itemView.context,
                            if (isWihte) {
                                R.drawable.ic_status_sending_white
                            } else {
                                R.drawable.ic_status_sending
                            })
                    MessageStatus.SENT.name ->
                        AppCompatResources.getDrawable(itemView.context,
                            if (isWihte) {
                                R.drawable.ic_status_sent_white
                            } else {
                                R.drawable.ic_status_sent
                            })
                    MessageStatus.DELIVERED.name ->
                        AppCompatResources.getDrawable(itemView.context, if (isWihte) {
                            R.drawable.ic_status_delivered_white
                        } else {
                            R.drawable.ic_status_delivered
                        })
                    MessageStatus.READ.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_read)
                    else -> null
                }
            drawable.also {
                it?.setBounds(0, 0, dp10, dp10)
            }
            setIcon(drawable)
        } else {
            hideIcon()
        }
    }
}