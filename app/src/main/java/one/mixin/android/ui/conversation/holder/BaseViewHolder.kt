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

abstract class BaseViewHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    companion object {
        val colors: IntArray = MixinApplication.appContext.resources.getIntArray(R.array.name_colors)
        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
    }

    abstract fun chatLayout(isMe: Boolean, isLast: Boolean)

    protected val botIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_bot)?.also {
            it.setBounds(0, 0, dp12, dp12)
        }
    }
    private val dp12 by lazy {
        itemView.context.dpToPx(12f)
    }

    val meId by lazy {
        Session.getAccountId()
    }
}