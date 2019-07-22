package one.mixin.android.ui.conversation.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_chat_app.view.*
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.vo.App
import org.jetbrains.anko.dip

class AppHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {

    private val dp16 by lazy {
        itemView.context.dip(16)
    }

    private val dp24 by lazy {
        itemView.context.dip(24)
    }

    fun bind(app: App, position: Int, isEnd: Boolean) {
        when {
            position == 0 -> itemView.setPadding(dp16, 0, 0, 0)
            isEnd -> itemView.setPadding(dp24, 0, dp16, 0)
            else -> itemView.setPadding(dp24, 0, 0, 0)
        }
        itemView.item_icon.loadCircleImage(app.icon_url)
        itemView.item_name.text = app.name
    }
}
