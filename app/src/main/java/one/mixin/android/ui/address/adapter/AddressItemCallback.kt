package one.mixin.android.ui.address.adapter

import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kotlinx.android.synthetic.main.item_address.view.*

class AddressItemCallback(private val listener: AddressAdapter.AddressListener? = null) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView?,
        viewHolder: RecyclerView.ViewHolder?,
        target: RecyclerView.ViewHolder?
    ):
        Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(it.itemView.foreground_rl)
        }
    }

    override fun onChildDrawOver(
        c: Canvas?,
        recyclerView: RecyclerView?,
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

    override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil().clearView(it.itemView.foreground_rl)
        }
    }

    override fun onChildDraw(
        c: Canvas?,
        recyclerView: RecyclerView?,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        viewHolder?.let {
            ItemTouchHelper.Callback.getDefaultUIUtil()
                .onDraw(c, recyclerView, it.itemView.foreground_rl, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener?.onAddrDelete(viewHolder)
    }
}