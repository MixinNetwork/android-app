package one.mixin.android.ui.common.recyclerview

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.timehop.stickyheadersrecyclerview.caching.HeaderViewCache
import com.timehop.stickyheadersrecyclerview.util.LinearLayoutOrientationProvider
import kotlin.math.max

class StickyRecyclerHeadersDecorationForGrid<VH : RecyclerView.ViewHolder>(
    private val adapter: StickyRecyclerHeadersAdapter<VH>,
    private val spanCount: Int,
) : StickyRecyclerHeadersDecoration(adapter) {
    private val tempRect = Rect()

    private val orientationProvider = LinearLayoutOrientationProvider()
    private val headerProvider = HeaderViewCache(adapter, orientationProvider)

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val childCount = parent.childCount
        if (childCount <= 0 || adapter.itemCount <= 0) return

        var highest = Int.MAX_VALUE
        for (i in childCount - 1 downTo 0) {
            val itemView = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(itemView)
            if (pos == RecyclerView.NO_POSITION) continue

            if (i == 0 || isFirstUnderHeader(pos)) {
                val header = headerProvider.getHeader(parent, pos)
                val transX = parent.left
                val transY = max(itemView.top - header.height, 0)
                tempRect.set(transX, transY, transX + header.width, transY + header.height)
                if (tempRect.bottom > highest) {
                    tempRect.offset(0, highest - tempRect.bottom)
                }
                drawHeader(c, header, tempRect)
                highest = tempRect.top
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val itemPos = parent.getChildAdapterPosition(view)
        if (itemPos == RecyclerView.NO_POSITION) {
            return
        }
        if (isUnderHeader(itemPos)) {
            val header = getHeaderView(parent, itemPos)
            outRect.top = header.height
        }
    }

    private fun isUnderHeader(pos: Int): Boolean {
        if (pos == 0) return true

        val curId = adapter.getHeaderId(pos)
        for (i in 1..spanCount) {
            var prevId = -1L
            val prevPos = pos - 1
            if (prevPos >= 0 && prevPos < adapter.itemCount) {
                prevId = adapter.getHeaderId(prevPos)
            }
            if (prevId != curId) {
                return true
            }
        }
        return false
    }

    private fun isFirstUnderHeader(pos: Int) = pos == 0 || adapter.getHeaderId(pos) != adapter.getHeaderId(pos - 1)

    private fun drawHeader(
        c: Canvas,
        header: View,
        rect: Rect,
    ) {
        c.save()
        c.translate(rect.left.toFloat(), rect.top.toFloat())
        header.draw(c)
        c.restore()
    }
}
