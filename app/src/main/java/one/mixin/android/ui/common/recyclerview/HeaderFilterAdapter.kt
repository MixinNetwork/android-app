package one.mixin.android.ui.common.recyclerview

import one.mixin.android.extension.notNullWithElse

abstract class HeaderFilterAdapter<T> : HeaderAdapter<T>() {

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null && !filtered()) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = data.notNullWithElse(
        {
            if (filtered()) it.size else it.size + 1
        },
        if (filtered()) 0 else 1
    )

    abstract fun filtered(): Boolean
}
