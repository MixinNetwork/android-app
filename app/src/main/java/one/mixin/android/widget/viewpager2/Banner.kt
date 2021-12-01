package one.mixin.android.widget.viewpager2

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import java.lang.reflect.InvocationTargetException
import kotlin.math.abs

class Banner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var changeCallback: OnPageChangeCallback? = null
    private var compositePageTransformer: CompositePageTransformer? = null
    private var adapterWrapper: BannerAdapterWrapper? = null
    lateinit var viewPager2: ViewPager2
    private var isAutoPlay = true
    private var isBeginPagerChange = true
    private var isTaskPostDelayed = false
    private var autoTurningTime = DEFAULT_AUTO_TIME
    private var pagerScrollDuration = DEFAULT_PAGER_DURATION
    private var needPage = NORMAL_COUNT
    private var sidePage = needPage / NORMAL_COUNT
    private var tempPosition = 0
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val scaledTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop shr 1

    init {
        viewPager2 = ViewPager2(context)
        viewPager2.layoutParams =
            ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        viewPager2.setPageTransformer(
            CompositePageTransformer().also {
                compositePageTransformer = it
            }
        )
        viewPager2.registerOnPageChangeCallback(OnPageChangeCallback())
        viewPager2.adapter = BannerAdapterWrapper().also { adapterWrapper = it }
        setOffscreenPageLimit(1)
        initViewPagerScrollProxy()
        addView(viewPager2)
    }

    private fun startPager(startPosition: Int) {
        if (sidePage == NORMAL_COUNT) {
            viewPager2.adapter = adapterWrapper
        } else {
            adapterWrapper!!.notifyDataSetChanged()
        }
        setCurrentItem(startPosition, false)
        if (isAutoPlay()) {
            startTurning()
        }
    }

    private val realCount: Int
        private get() = adapterWrapper!!.realCount

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAutoPlay()) {
            startTurning()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isAutoPlay()) {
            stopTurning()
        }
    }

    private val task: Runnable = object : Runnable {
        override fun run() {
            if (isAutoPlay()) {
                tempPosition++
                if (tempPosition == realCount + sidePage + 1) {
                    isBeginPagerChange = false
                    viewPager2.setCurrentItem(sidePage, false)
                    post(this)
                } else {
                    isBeginPagerChange = true
                    viewPager2.currentItem = tempPosition
                    postDelayed(this, autoTurningTime)
                }
            }
        }
    }

    private fun toRealPosition(position: Int): Int {
        var realPosition = 0
        if (realCount > 1) {
            realPosition = (position - sidePage) % realCount
        }
        if (realPosition < 0) {
            realPosition += realCount
        }
        return realPosition
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isAutoPlay() && viewPager2.isUserInputEnabled) {
            val action = ev.action
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                startTurning()
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopTurning()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN) {
            lastX = ev.rawX
            startX = lastX
            lastY = ev.rawY
            startY = lastY
        } else if (action == MotionEvent.ACTION_MOVE) {
            lastX = ev.rawX
            lastY = ev.rawY
            if (viewPager2.isUserInputEnabled) {
                val distanceX = abs(lastX - startX)
                val distanceY = abs(lastY - startY)
                val disallowIntercept: Boolean =
                    if (viewPager2.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                        distanceX > scaledTouchSlop && distanceX > distanceY
                    } else {
                        distanceY > scaledTouchSlop && distanceY > distanceX
                    }
                parent.requestDisallowInterceptTouchEvent(disallowIntercept)
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return abs(lastX - startX) > scaledTouchSlop || abs(lastY - startY) > scaledTouchSlop
        }
        return super.onInterceptTouchEvent(ev)
    }

    inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val realPosition = toRealPosition(position)
            if (changeCallback != null) {
                changeCallback!!.onPageScrolled(realPosition, positionOffset, positionOffsetPixels)
            }
        }

        override fun onPageSelected(position: Int) {
            if (realCount > 1) {
                tempPosition = position
            }
            if (isBeginPagerChange) {
                val realPosition = toRealPosition(position)
                if (changeCallback != null) {
                    changeCallback!!.onPageSelected(realPosition)
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                if (tempPosition == sidePage - 1) {
                    isBeginPagerChange = false
                    viewPager2.setCurrentItem(realCount + tempPosition, false)
                } else if (tempPosition == realCount + sidePage) {
                    isBeginPagerChange = false
                    viewPager2.setCurrentItem(sidePage, false)
                } else {
                    isBeginPagerChange = true
                }
            }
            if (changeCallback != null) {
                changeCallback!!.onPageScrollStateChanged(state)
            }
        }
    }

    private inner class BannerAdapterWrapper : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return adapter!!.onCreateViewHolder(parent, viewType)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            adapter!!.onBindViewHolder(holder, toRealPosition(position))
        }

        override fun getItemViewType(position: Int): Int {
            return adapter!!.getItemViewType(toRealPosition(position))
        }

        override fun getItemCount(): Int {
            return if (realCount > 1) realCount + needPage else realCount
        }

        override fun getItemId(position: Int): Long {
            return adapter!!.getItemId(toRealPosition(position))
        }

        val realCount: Int
            get() = if (adapter == null) 0 else adapter!!.itemCount

        fun registerAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?) {
            if (this.adapter != null) {
                this.adapter!!.unregisterAdapterDataObserver(itemDataSetChangeObserver)
            }
            this.adapter = adapter
            if (this.adapter != null) {
                this.adapter!!.registerAdapterDataObserver(itemDataSetChangeObserver)
            }
        }
    }

    private val itemDataSetChangeObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            onChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (positionStart > 1) onChanged()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            onChanged()
        }

        override fun onChanged() {
            startPager(currentPager)
        }
    }

    private fun initViewPagerScrollProxy() {
        try {
            val recyclerView = viewPager2.getChildAt(0) as RecyclerView
            recyclerView.overScrollMode = OVER_SCROLL_NEVER
            val o = recyclerView.layoutManager as LinearLayoutManager
            val proxyLayoutManger = ProxyLayoutManger(context, o)
            recyclerView.layoutManager = proxyLayoutManger
            val mRecyclerView =
                RecyclerView.LayoutManager::class.java.getDeclaredField("mRecyclerView")
            mRecyclerView.isAccessible = true
            mRecyclerView[o] = recyclerView
            val LayoutMangerField = ViewPager2::class.java.getDeclaredField("mLayoutManager")
            LayoutMangerField.isAccessible = true
            LayoutMangerField[viewPager2] = proxyLayoutManger
            val pageTransformerAdapterField =
                ViewPager2::class.java.getDeclaredField("mPageTransformerAdapter")
            pageTransformerAdapterField.isAccessible = true
            val mPageTransformerAdapter = pageTransformerAdapterField[viewPager2]
            if (mPageTransformerAdapter != null) {
                val aClass: Class<*> = mPageTransformerAdapter.javaClass
                val layoutManager = aClass.getDeclaredField("mLayoutManager")
                layoutManager.isAccessible = true
                layoutManager[mPageTransformerAdapter] = proxyLayoutManger
            }
            val scrollEventAdapterField =
                ViewPager2::class.java.getDeclaredField("mScrollEventAdapter")
            scrollEventAdapterField.isAccessible = true
            val mScrollEventAdapter = scrollEventAdapterField[viewPager2]
            if (mScrollEventAdapter != null) {
                val aClass: Class<*> = mScrollEventAdapter.javaClass
                val layoutManager = aClass.getDeclaredField("mLayoutManager")
                layoutManager.isAccessible = true
                layoutManager[mScrollEventAdapter] = proxyLayoutManger
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private inner class ProxyLayoutManger(
        context: Context?,
        layoutManager: LinearLayoutManager
    ) :
        LinearLayoutManager(context, layoutManager.orientation, false) {
        private val layoutManager: RecyclerView.LayoutManager
        override fun performAccessibilityAction(
            recycler: Recycler,
            state: RecyclerView.State,
            action: Int,
            args: Bundle?
        ): Boolean {
            return layoutManager.performAccessibilityAction(recycler, state, action, args)
        }

        override fun onInitializeAccessibilityNodeInfo(
            recycler: Recycler,
            state: RecyclerView.State,
            info: AccessibilityNodeInfoCompat
        ) {
            layoutManager.onInitializeAccessibilityNodeInfo(recycler, state, info)
        }

        override fun requestChildRectangleOnScreen(
            parent: RecyclerView,
            child: View,
            rect: Rect,
            immediate: Boolean,
            focusedChildVisible: Boolean
        ): Boolean {
            return layoutManager.requestChildRectangleOnScreen(
                parent,
                child,
                rect,
                immediate,
                focusedChildVisible
            )
        }

        override fun calculateExtraLayoutSpace(
            state: RecyclerView.State,
            extraLayoutSpace: IntArray
        ) {
            try {
                val method = layoutManager.javaClass.getDeclaredMethod(
                    "calculateExtraLayoutSpace",
                    state.javaClass,
                    extraLayoutSpace.javaClass
                )
                method.isAccessible = true
                method.invoke(layoutManager, state, extraLayoutSpace)
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }

        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
        ) {
            val linearSmoothScroller: LinearSmoothScroller =
                object : LinearSmoothScroller(recyclerView.context) {
                    override fun calculateTimeForDeceleration(dx: Int): Int {
                        return (pagerScrollDuration * (1 - .3356)).toInt()
                    }
                }
            linearSmoothScroller.targetPosition = position
            startSmoothScroll(linearSmoothScroller)
        }

        init {
            this.layoutManager = layoutManager
        }
    }

    fun setPageMargin(multiWidth: Int, pageMargin: Int): Banner {
        return setPageMargin(multiWidth, multiWidth, pageMargin)
    }

    fun setPageMargin(tlWidth: Int, brWidth: Int, pageMargin: Int): Banner {
        var pageMargin = pageMargin
        if (pageMargin < 0) pageMargin = 0
        addPageTransformer(MarginPageTransformer(pageMargin))
        val recyclerView = viewPager2.getChildAt(0) as RecyclerView
        if (viewPager2.orientation == ViewPager2.ORIENTATION_VERTICAL) {
            recyclerView.setPadding(
                viewPager2.paddingLeft,
                tlWidth + Math.abs(pageMargin),
                viewPager2.paddingRight,
                brWidth + Math.abs(pageMargin)
            )
        } else {
            recyclerView.setPadding(
                tlWidth + Math.abs(pageMargin),
                viewPager2.paddingTop,
                brWidth + Math.abs(pageMargin),
                viewPager2.paddingBottom
            )
        }
        recyclerView.clipToPadding = false
        needPage = NORMAL_COUNT + NORMAL_COUNT
        sidePage = NORMAL_COUNT
        return this
    }

    fun addPageTransformer(transformer: ViewPager2.PageTransformer?): Banner {
        compositePageTransformer!!.addTransformer(transformer!!)
        return this
    }

    fun setAutoTurningTime(autoTurningTime: Long): Banner {
        this.autoTurningTime = autoTurningTime
        return this
    }

    fun setOuterPageChangeListener(listener: OnPageChangeCallback?): Banner {
        changeCallback = listener
        return this
    }

    fun setOffscreenPageLimit(limit: Int): Banner {
        viewPager2.offscreenPageLimit = limit
        return this
    }

    fun setPagerScrollDuration(pagerScrollDuration: Long): Banner {
        this.pagerScrollDuration = pagerScrollDuration
        return this
    }

    fun setOrientation(@ViewPager2.Orientation orientation: Int): Banner {
        viewPager2.orientation = orientation
        return this
    }

    fun addItemDecoration(decor: ItemDecoration): Banner {
        viewPager2.addItemDecoration(decor)
        return this
    }

    fun addItemDecoration(decor: ItemDecoration, index: Int): Banner {
        viewPager2.addItemDecoration(decor, index)
        return this
    }

    fun setAutoPlay(autoPlay: Boolean): Banner {
        isAutoPlay = autoPlay
        if (isAutoPlay && realCount > 1) {
            startTurning()
        }
        return this
    }

    fun isAutoPlay(): Boolean {
        return isAutoPlay && realCount > 1
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun setRoundCorners(radius: Float): Banner {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        clipToOutline = true
        return this
    }

    fun setCurrentItem(position: Int) {
        setCurrentItem(position, true)
    }

    fun setCurrentItem(position: Int, smoothScroll: Boolean) {
        tempPosition = position + sidePage
        viewPager2.setCurrentItem(tempPosition, smoothScroll)
    }

    val currentPager: Int
        get() {
            val position = toRealPosition(tempPosition)
            return Math.max(position, 0)
        }
    var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?
        get() = adapterWrapper!!.adapter
        set(adapter) {
            adapterWrapper!!.registerAdapter(adapter)
        }

    fun startTurning() {
        stopTurning()
        postDelayed(task, autoTurningTime)
        isTaskPostDelayed = true
    }

    fun stopTurning() {
        if (isTaskPostDelayed) {
            removeCallbacks(task)
            isTaskPostDelayed = false
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, startPosition: Int = 0) {
        adapterWrapper!!.registerAdapter(adapter)
        startPager(startPosition)
    }

    companion object {
        private const val DEFAULT_AUTO_TIME: Long = 2500
        private const val DEFAULT_PAGER_DURATION: Long = 800
        private const val NORMAL_COUNT = 2
    }
}
