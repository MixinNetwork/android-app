package one.mixin.android.widget.recyclerview

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView

class OffsetListUpdateCallback(
    private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
    private val offset: Int,
) : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        adapter.notifyItemRangeChanged(position + offset, count, payload)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter.notifyItemMoved(fromPosition + offset, toPosition + offset)
    }

    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeInserted(position + offset, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position + offset, count)
    }
}
