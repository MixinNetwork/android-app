package one.mixin.android.ui.wallet.adapter

import android.graphics.Canvas
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.NormalHolder

class AssetItemCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val swipeFlags = if (viewHolder is NormalHolder) {
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            0
        }
        return makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && findBackground(it.itemView).visibility != VISIBLE) {
                findBackground(it.itemView).visibility = VISIBLE
            }
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(findBackground(it.itemView))
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
                .onDrawOver(c, recyclerView, findBackground(it.itemView), dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(findBackground(viewHolder.itemView))
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
            .onDraw(c, recyclerView, findBackground(viewHolder.itemView), dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        findBackground(viewHolder.itemView).visibility = GONE
        listener.onSwiped(viewHolder)
    }

    private fun findBackground(view: View): View = view.findViewById(R.id.background_rl)

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder)
    }
}
