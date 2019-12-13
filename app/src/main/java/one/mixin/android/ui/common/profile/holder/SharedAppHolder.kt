package one.mixin.android.ui.common.profile.holder

import android.view.View
import kotlinx.android.synthetic.main.item_shared_app.view.*
import one.mixin.android.vo.App

class SharedAppHolder(itemView: View) : ItemViewHolder(itemView) {
    override fun bind(app: App, removeAction: (app: App) -> Unit) {
        itemView.avatar.setInfo(app.name, app.icon_url, app.appId)
        itemView.name.text = app.name
        itemView.icon.setOnClickListener {
            removeAction(app)
        }
    }
}
