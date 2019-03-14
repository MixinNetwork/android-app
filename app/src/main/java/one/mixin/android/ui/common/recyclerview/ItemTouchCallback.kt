package one.mixin.android.ui.common.recyclerview

import android.graphics.Canvas
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R

open class ItemTouchCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            val background = it.itemView.findViewById<View>(R.id.background_rl)
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && background.visibility != VISIBLE) {
                background.visibility = VISIBLE
            }
            val foreground = it.itemView.findViewById<View>(R.id.foreground_rl)
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(foreground)
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
                .onDrawOver(c, recyclerView, it.itemView.findViewById<View>(R.id.foreground_rl), dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(viewHolder.itemView.findViewById<View>(R.id.foreground_rl))
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
            .onDraw(c, recyclerView, viewHolder.itemView.findViewById<View>(R.id.foreground_rl), dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        viewHolder.itemView.findViewById<View>(R.id.background_rl).visibility = GONE
        listener.onSwiped(viewHolder)
    }

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder)
    }
}