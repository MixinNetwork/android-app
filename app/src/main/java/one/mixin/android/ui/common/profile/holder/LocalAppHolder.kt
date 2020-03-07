package one.mixin.android.ui.common.profile.holder

import android.view.View
import kotlinx.android.synthetic.main.item_shared_local_app.view.*
import one.mixin.android.vo.App

class LocalAppHolder(itemView: View) :
    ItemViewHolder(itemView) {

    override fun bind(app: App, appAction: (app: App) -> Unit) {
        itemView.avatar.setInfo(app.name, app.iconUrl, app.appId)
        itemView.name.text = app.name
        itemView.icon.setOnClickListener { appAction(app) }
    }
}
