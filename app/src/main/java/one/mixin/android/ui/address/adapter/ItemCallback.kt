package one.mixin.android.ui.address.adapter

import android.graphics.Canvas
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.android.synthetic.main.item_address.view.*

class ItemCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(it.itemView.foreground_rl)
        }
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil()
                .onDrawOver(c, recyclerView, it.itemView.foreground_rl, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(viewHolder.itemView.foreground_rl)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        ItemTouchHelper.Callback.getDefaultUIUtil()
            .onDraw(c, recyclerView, viewHolder.itemView.foreground_rl, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwiped(viewHolder)
    }

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder)
    }
}