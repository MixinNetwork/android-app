package one.mixin.android.ui.common.profile.holder

import android.view.View
import one.mixin.android.vo.ExploreApp

class FooterHolder(itemView: View) :
    ItemViewHolder(itemView) {
    override fun bind(
        app: ExploreApp,
        target: String?,
        appAction: (app: ExploreApp) -> Unit,
    ) {
    }
}
