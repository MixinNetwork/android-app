package one.mixin.android.ui.common.profile.holder

import one.mixin.android.databinding.ItemSharedAppBinding
import one.mixin.android.extension.highLight
import one.mixin.android.vo.ExploreApp

class SharedAppHolder(private val itemBinding: ItemSharedAppBinding) : ItemViewHolder(itemBinding.root) {
    override fun bind(
        app: ExploreApp,
        target: String?,
        appAction: (app: ExploreApp) -> Unit,
    ) {
        itemBinding.apply {
            avatar.setInfo(app.name, app.iconUrl, app.appId)
            name.setName(app)
            mixinIdTv.text = app.appNumber
            name.highLight(target)
            mixinIdTv.highLight(target)
            icon.setOnClickListener {
                appAction(app)
            }
        }
    }
}
