package one.mixin.android.widget

import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.HeaderPositionCalculator
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import com.timehop.stickyheadersrecyclerview.caching.HeaderProvider
import com.timehop.stickyheadersrecyclerview.caching.HeaderViewCache
import com.timehop.stickyheadersrecyclerview.calculation.DimensionCalculator
import com.timehop.stickyheadersrecyclerview.rendering.HeaderRenderer
import com.timehop.stickyheadersrecyclerview.util.LinearLayoutOrientationProvider
import com.timehop.stickyheadersrecyclerview.util.OrientationProvider

class ConcatHeadersDecoration private constructor(
    private val mAdapter: StickyRecyclerHeadersAdapter<*>,
    private val mRenderer: HeaderRenderer,
    private val mOrientationProvider: OrientationProvider,
    private val mDimensionCalculator: DimensionCalculator,
    private val mHeaderProvider: HeaderProvider,
    private val mHeaderPositionCalculator: HeaderPositionCalculator,
) : RecyclerView.ItemDecoration() {
    private val mHeaderRects = SparseArray<Rect>()

    private val mTempRect = Rect()

    var headerCount = 0

    constructor(adapter: StickyRecyclerHeadersAdapter<*>) : this(
        adapter,
        LinearLayoutOrientationProvider(),
        DimensionCalculator(),
    )

    private constructor(
        adapter: StickyRecyclerHeadersAdapter<*>,
        orientationProvider: OrientationProvider,
        dimensionCalculator: DimensionCalculator,
    ) : this(
        adapter,
        orientationProvider,
        dimensionCalculator,
        HeaderRenderer(orientationProvider),
        HeaderViewCache(adapter, orientationProvider),
    )

    private constructor(
        adapter: StickyRecyclerHeadersAdapter<*>,
        orientationProvider: OrientationProvider,
        dimensionCalculator: DimensionCalculator,
        headerRenderer: HeaderRenderer,
        headerProvider: HeaderProvider,
    ) : this(
        adapter,
        headerRenderer,
        orientationProvider,
        dimensionCalculator,
        headerProvider,
        HeaderPositionCalculator(
            adapter,
            headerProvider,
            orientationProvider,
            dimensionCalculator,
        ),
    )

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemPosition = parent.getChildAdapterPosition(view)
        if (itemPosition < headerCount) {
            return
        }
        val headerPos = itemPosition - headerCount
        if (mHeaderPositionCalculator.hasNewHeader(headerPos, mOrientationProvider.isReverseLayout(parent))) {
            val header = getHeaderView(parent, headerPos)
            setItemOffsetsForHeader(outRect, header, mOrientationProvider.getOrientation(parent))
        }
    }

    private fun setItemOffsetsForHeader(
        itemOffsets: Rect,
        header: View,
        orientation: Int,
    ) {
        mDimensionCalculator.initMargins(mTempRect, header)
        if (orientation == LinearLayoutManager.VERTICAL) {
            itemOffsets.top = header.height + mTempRect.top + mTempRect.bottom
        } else {
            itemOffsets.left = header.width + mTempRect.left + mTempRect.right
        }
    }

    override fun onDrawOver(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.onDrawOver(canvas, parent, state)

        val childCount = parent.childCount
        if (childCount <= 0 || mAdapter.itemCount <= 0) {
            return
        }

        for (i in 0 until childCount) {
            val itemView = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(itemView)
            if (position < headerCount) {
                continue
            }
            val headerPosition = position - headerCount
            val hasStickyHeader = mHeaderPositionCalculator.hasStickyHeader(itemView, mOrientationProvider.getOrientation(parent), position)
            if (hasStickyHeader || mHeaderPositionCalculator.hasNewHeader(headerPosition, mOrientationProvider.isReverseLayout(parent))) {
                val header = mHeaderProvider.getHeader(parent, headerPosition)
                var headerOffset = mHeaderRects[headerPosition]
                if (headerOffset == null) {
                    headerOffset = Rect()
                    mHeaderRects.put(headerPosition, headerOffset)
                }
                mHeaderPositionCalculator.initHeaderBounds(headerOffset, parent, header, itemView, hasStickyHeader)
                mRenderer.drawHeader(parent, canvas, header, headerOffset)
            }
        }
    }

    @Suppress("unused")
    fun findHeaderPositionUnder(
        x: Int,
        y: Int,
    ): Int {
        for (i in 0 until mHeaderRects.size()) {
            val rect = mHeaderRects.get(mHeaderRects.keyAt(i))
            if (rect.contains(x, y)) {
                return mHeaderRects.keyAt(i)
            }
        }
        return -1
    }

    private fun getHeaderView(
        parent: RecyclerView,
        position: Int,
    ): View =
        mHeaderProvider.getHeader(parent, position)

    @Suppress("unused")
    fun invalidateHeaders() {
        mHeaderProvider.invalidate()
    }
}
