package one.mixin.android.widget

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.viewpager.widget.PagerAdapter
import java.util.SortedSet
import java.util.TreeSet

abstract class PagedListPagerAdapter<T> : PagerAdapter() {
    private var pagedList: PagedList<T>? = null
    private var callback = PagerCallback()

    private var visiblePositions: SortedSet<Int> = TreeSet()

    override fun isViewFromObject(view: View, obj: Any) = view == obj

    override fun getCount() = pagedList?.size ?: 0

    fun getItem(position: Int): T? {
        return if (position >= 0 && position < pagedList?.size ?: 0) {
            pagedList?.get(position)
        } else {
            null
        }
    }

    var submitAction: (() -> Unit)? = null

    final override fun instantiateItem(container: ViewGroup, position: Int): Any {
        visiblePositions.add(position)
        pagedList?.loadAround(position)
        return createItem(container, position)
    }

    final override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        visiblePositions.remove(position)
        removeItem(container, position, obj)
    }

    override fun getItemPosition(obj: Any) = POSITION_NONE

    abstract fun createItem(container: ViewGroup, position: Int): Any
    abstract fun removeItem(container: ViewGroup, position: Int, obj: Any)

    fun submitList(pagedList: PagedList<T>?) {
        this.pagedList?.removeWeakCallback(callback)
        this.pagedList = pagedList
        pagedList?.addWeakCallback(null, callback)
        notifyDataSetChanged()
        submitAction?.let {
            it.invoke()
            submitAction = null
        }
    }

    private inner class PagerCallback : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) =
                analyzeCount(position, count)

        override fun onInserted(position: Int, count: Int) =
                analyzeCount(position, count)

        override fun onRemoved(position: Int, count: Int) =
                analyzeCount(position, count)

        private fun analyzeCount(start: Int, count: Int) = analyzeRange(start, start + count)

        private fun analyzeRange(start: Int, end: Int) {
            if (isInterleave(start, end)) {
                notifyDataSetChanged()
            }
        }

        private fun isInterleave(start: Int, end: Int) =
            try {
                start <= visiblePositions.last() && visiblePositions.first() <= end
            } catch (e: Exception) {
                false
            }
    }
}