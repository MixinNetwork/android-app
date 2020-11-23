package one.mixin.android.ui.conversation.chat

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.translationX
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import kotlin.math.max

class ChatItemCallback(private val listener: ItemCallbackListener) :
    ItemTouchHelper.Callback() {

    private fun rootLayout(itemView: View): View {
        return itemView.findViewById(R.id.chat_layout)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val swipeFlags = if (viewHolder is BaseViewHolder) {
            ItemTouchHelper.LEFT
        } else {
            0
        }
        return makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
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
        if (!isCurrentlyActive) return

        rootLayout(viewHolder.itemView).apply {
            translationX = max(-SWAP_SLOT.toFloat(), dX)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwiped(viewHolder)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        rootLayout(viewHolder.itemView).apply {
            translationX(0f, 100)
        }
    }

    interface ItemCallbackListener {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder)
    }

    companion object {
        val SWAP_SLOT: Int = 48.dp
    }
}
