package one.mixin.android.ui.conversation.chat

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.dp
import one.mixin.android.extension.translationX
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import kotlin.math.max

class ChatItemCallback(private val context: Context, private val listener: ItemCallbackListener) :
    ItemTouchHelper.Callback() {

    private fun vibrate() {
        context.clickVibrate()
    }

    private fun rootLayout(itemView: View): View {
        return itemView.findViewById(R.id.chat_layout)
    }

    private fun chatReply(itemView: View): View? {
        return itemView.findViewById(R.id.chat_reply)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val swipeFlags = if (viewHolder is BaseViewHolder && !viewHolder.canNotReply) {
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

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            listener.onSwiped(hold)
        }
    }

    private var hold = -1
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (dX < -TRIGGER) {
            if (hold != viewHolder.absoluteAdapterPosition) {
                hold = viewHolder.absoluteAdapterPosition
                vibrate()
                popUp(viewHolder.itemView)
            }
        } else {
            if (hold != -1) {
                popDown(viewHolder.itemView)
                hold = -1
            }
        }
        rootLayout(viewHolder.itemView).apply {
            translationX = max(-(DISPLAY).toFloat(), dX)
        }
        chatReply(viewHolder.itemView)?.apply {
            translationX = max(-(DISPLAY).toFloat(), dX)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        rootLayout(viewHolder.itemView).apply {
            translationX(0f, 100)
        }
        popDown(viewHolder.itemView)
    }

    private fun popUp(view: View) {
        chatReply(view)?.let { target ->
            ViewCompat.animate(target)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(100)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()
        }
    }

    private fun popDown(view: View) {
        chatReply(view)?.let { target ->
            ViewCompat.animate(target)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(100)
                .start()
        }
    }

    interface ItemCallbackListener {
        fun onSwiped(position: Int)
    }

    companion object {
        val SWAP_SLOT: Int = 128.dp
        val TRIGGER: Int = 48.dp
        val DISPLAY: Int = 64.dp
    }
}
