package one.mixin.android.ui.common.profile.holder

import one.mixin.android.databinding.ItemSharedLocalAppBinding
import one.mixin.android.vo.App

class LocalAppHolder(private val itemBinding: ItemSharedLocalAppBinding) :
    ItemViewHolder(itemBinding.root) {

    override fun bind(app: App, appAction: (app: App) -> Unit) {
        itemBinding.apply {
            avatar.setInfo(app.name, app.iconUrl, app.appId)
            name.text = app.name
            icon.setOnClickListener { appAction(app) }
        }
    }
}
