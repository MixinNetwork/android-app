package one.mixin.android.ui.common.profile.holder

import androidx.core.view.isVisible
import one.mixin.android.databinding.ItemSharedAppBinding
import one.mixin.android.vo.ExploreApp

class SharedAppHolder(private val itemBinding: ItemSharedAppBinding) : ItemViewHolder(itemBinding.root) {
    override fun bind(
        app: ExploreApp,
        appAction: (app: ExploreApp) -> Unit,
    ) {
        itemBinding.apply {
            avatar.setInfo(app.name, app.iconUrl, app.appId)
            name.text = app.name
            mixinIdTv.text = app.appNumber
            verifiedIv.isVisible = app.isVerified ?: false
            icon.setOnClickListener {
                appAction(app)
            }
        }
    }
}
