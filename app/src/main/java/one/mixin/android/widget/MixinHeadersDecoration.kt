package one.mixin.android.widget

import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.HeaderPositionCalculator
import com.timehop.stickyheadersrecyclerview.caching.HeaderProvider
import com.timehop.stickyheadersrecyclerview.caching.HeaderViewCache
import com.timehop.stickyheadersrecyclerview.calculation.DimensionCalculator
import com.timehop.stickyheadersrecyclerview.rendering.HeaderRenderer
import com.timehop.stickyheadersrecyclerview.util.LinearLayoutOrientationProvider
import com.timehop.stickyheadersrecyclerview.util.OrientationProvider
import one.mixin.android.extension.dpToPx

class MixinHeadersDecoration private constructor(
    private val mAdapter: MixinStickyRecyclerHeadersAdapter<*>,
    private val mRenderer: HeaderRenderer,
    private val mOrientationProvider: OrientationProvider,
    private val mDimensionCalculator: DimensionCalculator,
    private val mHeaderProvider: HeaderProvider,
    private val mHeaderPositionCalculator: HeaderPositionCalculator,
) : RecyclerView.ItemDecoration() {
    private val mHeaderRects = SparseArray<Rect>()

    private val mTempRect = Rect()

    constructor(adapter: MixinStickyRecyclerHeadersAdapter<*>) : this(
        adapter,
        LinearLayoutOrientationProvider(),
        DimensionCalculator(),
    )

    private constructor(
        adapter: MixinStickyRecyclerHeadersAdapter<*>,
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
        adapter: MixinStickyRecyclerHeadersAdapter<*>,
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
        if (itemPosition == RecyclerView.NO_POSITION) {
            return
        }
        if (mHeaderPositionCalculator.hasNewHeader(itemPosition, mOrientationProvider.isReverseLayout(parent))) {
            val header = getHeaderView(parent, itemPosition)
            setItemOffsetsForHeader(outRect, header, mOrientationProvider.getOrientation(parent))
        }
        if (mAdapter.hasAttachView(itemPosition)) {
            getAttachView(parent).let {
                outRect.top += it.measuredHeight
                mAdapter.onBindAttachView(it)
            }
        }
        if (!mAdapter.isListLast(itemPosition)) {
            if (mAdapter.isLast(itemPosition) || mAdapter.isButtonGroup(itemPosition)) {
                outRect.bottom = view.context.dpToPx(6f)
            }
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
            if (position == RecyclerView.NO_POSITION) {
                continue
            }
            val hasStickyHeader =
                mHeaderPositionCalculator.hasStickyHeader(
                    itemView,
                    mOrientationProvider.getOrientation(parent),
                    position,
                )
            if (hasStickyHeader || mHeaderPositionCalculator.hasNewHeader(position, mOrientationProvider.isReverseLayout(parent))) {
                val header = mHeaderProvider.getHeader(parent, position)
                val headerOffset: Rect? = mHeaderRects.get(position)
                if (headerOffset != null) {
                    mRenderer.drawHeader(parent, canvas, header, headerOffset)
                }
            }
        }
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.onDraw(canvas, parent, state)

        val childCount = parent.childCount
        if (childCount <= 0 || mAdapter.itemCount <= 0) {
            return
        }

        for (i in 0 until childCount) {
            val itemView = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(itemView)
            if (position == RecyclerView.NO_POSITION) {
                continue
            }
            val hasStickyHeader =
                mHeaderPositionCalculator.hasStickyHeader(
                    itemView,
                    mOrientationProvider.getOrientation(parent),
                    position,
                )
            if (hasStickyHeader || mHeaderPositionCalculator.hasNewHeader(position, mOrientationProvider.isReverseLayout(parent))) {
                val header = mHeaderProvider.getHeader(parent, position)
                var headerOffset: Rect? = mHeaderRects.get(position)
                if (headerOffset == null) {
                    headerOffset = Rect()
                    mHeaderRects.put(position, headerOffset)
                }

                mHeaderPositionCalculator.initHeaderBounds(headerOffset, parent, header, itemView, hasStickyHeader)
                mRenderer.drawHeader(parent, canvas, header, headerOffset)
                if (mAdapter.hasAttachView(position)) {
                    getAttachView(parent).let { view ->
                        val top = (headerOffset.top - view.measuredHeight)
                        canvas.save()
                        canvas.translate(0f, top.toFloat())
                        view.draw(canvas)
                        canvas.restore()
                    }
                }
            } else if (mAdapter.hasAttachView(position)) {
                getAttachView(parent).let { view ->
                    val top = (itemView.y - view.measuredHeight).toInt()
                    canvas.save()
                    canvas.translate(0f, top.toFloat())
                    view.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }

    private var attachView: View? = null

    private fun getAttachView(parent: ViewGroup): View {
        return if (this.attachView == null) {
            val attachView = mAdapter.onCreateAttach(parent)
            if (attachView.layoutParams == null) {
                attachView.layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
            val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
            val childWidth =
                ViewGroup.getChildMeasureSpec(
                    widthSpec,
                    parent.paddingLeft + parent.paddingRight,
                    attachView.layoutParams.width,
                )
            val childHeight =
                ViewGroup.getChildMeasureSpec(
                    heightSpec,
                    parent.paddingTop + parent.paddingBottom,
                    attachView.layoutParams.height,
                )
            attachView.measure(childWidth, childHeight)
            attachView.layout(0, 0, attachView.measuredWidth, attachView.measuredHeight)
            this.attachView = attachView
            attachView
        } else {
            this.attachView!!
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
