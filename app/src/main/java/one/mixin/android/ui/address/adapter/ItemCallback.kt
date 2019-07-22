package one.mixin.android.ui.address.adapter

import android.graphics.Canvas
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_address.view.*
import one.mixin.android.R

class ItemCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {

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
        direction = 0
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
        if (dX > 0 && direction != ItemTouchHelper.START) {
            direction = ItemTouchHelper.START
            viewHolder.itemView.background_rl.setBackgroundResource(R.color.colorRed)
            viewHolder.itemView.delete_icon.isVisible = true
            viewHolder.itemView.delete_tv.isVisible = true
            viewHolder.itemView.edit_icon.isGone = true
            viewHolder.itemView.edit_tv.isGone = true
        } else if (dX < 0 && direction != ItemTouchHelper.END) {
            direction = ItemTouchHelper.END
            viewHolder.itemView.background_rl.setBackgroundResource(R.color.colorBlue)
            viewHolder.itemView.edit_icon.isVisible = true
            viewHolder.itemView.edit_tv.isVisible = true
            viewHolder.itemView.delete_icon.isGone = true
            viewHolder.itemView.delete_tv.isGone = true
        }
        ItemTouchHelper.Callback.getDefaultUIUtil()
            .onDraw(c, recyclerView, viewHolder.itemView.foreground_rl, dX, dY, actionState, isCurrentlyActive)
    }

    private var direction: Int = 0

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwiped(viewHolder, direction)
    }

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }
}
