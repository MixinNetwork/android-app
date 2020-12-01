package one.mixin.android.ui.address.adapter

import android.graphics.Canvas
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R

class ItemCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {

    override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(findView(viewHolder.itemView, R.id.foreground_rl))
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
                .onDrawOver(c, recyclerView, findView(viewHolder.itemView, R.id.foreground_rl), dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        direction = 0
        ItemTouchHelper.Callback.getDefaultUIUtil()
            .clearView(findView(viewHolder.itemView, R.id.foreground_rl))
    }

    private fun findView(view: View, @IdRes id: Int): View = view.findViewById(id)

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (dX > 0 && direction != ItemTouchHelper.START) {
            direction = ItemTouchHelper.START
            findView(viewHolder.itemView, R.id.background_rl).setBackgroundResource(R.color.colorRed)
            findView(viewHolder.itemView, R.id.delete_icon).isVisible = true
            findView(viewHolder.itemView, R.id.delete_tv).isVisible = true
            findView(viewHolder.itemView, R.id.delete_icon_copy).isGone = true
            findView(viewHolder.itemView, R.id.delete_tv_copy).isGone = true
        } else if (dX < 0 && direction != ItemTouchHelper.END) {
            direction = ItemTouchHelper.END
            findView(viewHolder.itemView, R.id.background_rl).setBackgroundResource(R.color.colorRed)
            findView(viewHolder.itemView, R.id.delete_tv_copy).isVisible = true
            findView(viewHolder.itemView, R.id.delete_icon_copy).isVisible = true
            findView(viewHolder.itemView, R.id.delete_icon).isGone = true
            findView(viewHolder.itemView, R.id.delete_tv).isGone = true
        }
        ItemTouchHelper.Callback.getDefaultUIUtil()
            .onDraw(c, recyclerView, findView(viewHolder.itemView, R.id.foreground_rl), dX, dY, actionState, isCurrentlyActive)
    }

    private var direction: Int = 0

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwiped(viewHolder, direction)
    }

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }
}
