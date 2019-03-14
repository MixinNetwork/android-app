package one.mixin.android.ui.setting

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.ui.common.recyclerview.ItemTouchCallback

class AuthItemCallback(listener: ItemCallbackListener) : ItemTouchCallback(listener) {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val swipeFlags = if (viewHolder is AuthenticationsFragment.ItemHolder) {
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            0
        }
        return makeMovementFlags(0, swipeFlags)
    }
}