package one.mixin.android.ui.common.recyclerview

import androidx.recyclerview.widget.ListUpdateCallback

class HeaderListUpdateCallback<T>(private val adapter: HeaderAdapter<T>, private val headerCount: Int = 1) : ListUpdateCallback {
    override fun onChanged(
        position: Int,
        count: Int,
        payload: Any?,
    ) {
        adapter.notifyItemRangeChanged(position + headerCount, count, payload)
    }

    override fun onMoved(
        fromPosition: Int,
        toPosition: Int,
    ) {
        adapter.notifyItemMoved(fromPosition + headerCount, toPosition + headerCount)
    }

    override fun onInserted(
        position: Int,
        count: Int,
    ) {
        adapter.notifyItemRangeInserted(position + headerCount, count)
    }

    override fun onRemoved(
        position: Int,
        count: Int,
    ) {
        adapter.notifyItemRangeRemoved(position + headerCount, count)
    }
}
