package one.mixin.android.ui.common.recyclerview

import androidx.recyclerview.widget.RecyclerView

/**
 * Fix PagedListAdapter with header auto scroll bug.
 */
class PagedHeaderAdapterDataObserver(
    private val dataObserver: RecyclerView.AdapterDataObserver,
    private val headerCount: Int,
) : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        dataObserver.onChanged()
    }

    override fun onItemRangeChanged(
        positionStart: Int,
        itemCount: Int,
    ) {
        dataObserver.onItemRangeChanged(positionStart + headerCount, itemCount)
    }

    override fun onItemRangeChanged(
        positionStart: Int,
        itemCount: Int,
        payload: Any?,
    ) {
        dataObserver.onItemRangeChanged(positionStart + headerCount, itemCount, payload)
    }

    override fun onItemRangeInserted(
        positionStart: Int,
        itemCount: Int,
    ) {
        dataObserver.onItemRangeInserted(positionStart + headerCount, itemCount)
    }

    override fun onItemRangeMoved(
        fromPosition: Int,
        toPosition: Int,
        itemCount: Int,
    ) {
        dataObserver.onItemRangeMoved(fromPosition + headerCount, toPosition + headerCount, itemCount)
    }

    override fun onItemRangeRemoved(
        positionStart: Int,
        itemCount: Int,
    ) {
        dataObserver.onItemRangeRemoved(positionStart + headerCount, itemCount)
    }
}
