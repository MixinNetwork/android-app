package one.mixin.android.ui.common.profile.holder

import one.mixin.android.R
import one.mixin.android.databinding.ItemSharedLocalAppBinding
import one.mixin.android.extension.highLight
import one.mixin.android.vo.ExploreApp

class LocalAppHolder(private val itemBinding: ItemSharedLocalAppBinding) :
    ItemViewHolder(itemBinding.root) {
    override fun bind(
        app: ExploreApp,
        target: String?,
        appAction: (app: ExploreApp) -> Unit,
    ) {
        itemBinding.apply {
            avatar.setInfo(app.name, app.iconUrl, app.appId)
            name.text = app.name
            mixinIdTv.text = app.appNumber
            name.highLight(target)
            mixinIdTv.highLight(target)
            verifiedIv.setImageResource(if (app.isMembership()) R.drawable.ic_membership_advance else if (app.isVerified == true) R.drawable.ic_bot else R.drawable.ic_user_verified)
            icon.setOnClickListener { appAction(app) }
        }
    }
}
