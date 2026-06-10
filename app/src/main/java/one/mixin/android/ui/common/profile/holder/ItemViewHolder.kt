package one.mixin.android.ui.common.profile.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.vo.ExploreApp

abstract class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(
        app: ExploreApp,
        target: String?,
        appAction: (app: ExploreApp) -> Unit,
    )
}
