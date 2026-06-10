package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import kotlin.math.max

open class FlowLayout
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ViewGroup(context, attrs, defStyle) {
        protected var mAllViews: MutableList<List<View>> = ArrayList()
        protected var mLineHeight: MutableList<Int> = ArrayList()
        protected var mLineWidth: MutableList<Int> = ArrayList()
        private val mGravity: Int
        private var lineViews: MutableList<View> = ArrayList()

        var singleLine = false

        var maxWidth: Int = 0
            set(value) {
                if (field != value) {
                    field = value
                    requestLayout()
                }
            }

        private var itemOffset = 0

        init {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)
            mGravity = ta.getInt(R.styleable.FlowLayout_tag_gravity, LEFT)
            singleLine = ta.getBoolean(R.styleable.FlowLayout_singleLine, false)
            maxWidth = ta.getDimensionPixelSize(R.styleable.FlowLayout_flow_max_width, 300.dp)
            itemOffset = ta.getDimensionPixelSize(R.styleable.FlowLayout_item_offset, 0)
            round(8.dp)
            ta.recycle()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            var sizeWidth = MeasureSpec.getSize(widthMeasureSpec)
            val modeWidth = MeasureSpec.getMode(widthMeasureSpec)
            val newWidthMeasureSpec: Int
            if (maxWidth in 1 until sizeWidth) {
                newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, modeWidth)
                super.onMeasure(newWidthMeasureSpec, heightMeasureSpec)
                sizeWidth = MeasureSpec.getSize(newWidthMeasureSpec)
            } else {
                newWidthMeasureSpec = widthMeasureSpec
            }
            val sizeHeight = MeasureSpec.getSize(heightMeasureSpec)
            val modeHeight = MeasureSpec.getMode(heightMeasureSpec)

            var width = 0
            var height = 0

            var lineWidth = 0
            var lineHeight = 0

            val cCount = childCount

            for (i in 0 until cCount) {
                val child = getChildAt(i)
                if (child.visibility == View.GONE) {
                    if (i == cCount - 1) {
                        width = max(lineWidth, width)
                        height += lineHeight
                    }
                    continue
                }
                measureChild(child, newWidthMeasureSpec, heightMeasureSpec)
                val lp = child.layoutParams as MarginLayoutParams

                val childWidth = (child.measuredWidth + lp.leftMargin + lp.rightMargin + if (lineWidth == 0) 0 else itemOffset)

                val childHeight = (child.measuredHeight + lp.topMargin + lp.bottomMargin)

                if (!singleLine && lineWidth + childWidth > sizeWidth - paddingLeft - paddingRight) {
                    width = max(width, lineWidth)
                    lineWidth = childWidth
                    height += lineHeight
                    lineHeight = childHeight
                } else {
                    lineWidth += childWidth
                    lineHeight = max(lineHeight, childHeight)
                }
                if (i == cCount - 1) {
                    width = max(lineWidth, width)
                    height += lineHeight
                }
            }
            setMeasuredDimension(
                if (modeWidth == MeasureSpec.EXACTLY) sizeWidth else width + paddingLeft + paddingRight,
                if (modeHeight == MeasureSpec.EXACTLY) sizeHeight else height + paddingTop + paddingBottom,
            )
        }

        override fun onLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int,
        ) {
            mAllViews.clear()
            mLineHeight.clear()
            mLineWidth.clear()
            lineViews.clear()

            val width = width

            var lineWidth = 0
            var lineHeight = 0

            val cCount = childCount

            for (i in 0 until cCount) {
                val child = getChildAt(i)
                if (child.visibility == View.GONE) continue
                val lp = child.layoutParams as MarginLayoutParams

                val childWidth =
                    child.measuredWidth +
                        if (lineWidth == 0) {
                            0
                        } else {
                            itemOffset
                        }
                val childHeight = child.measuredHeight

                if (childWidth + lineWidth + lp.leftMargin + lp.rightMargin > width - paddingLeft - paddingRight) {
                    mLineHeight.add(lineHeight)
                    mAllViews.add(lineViews)
                    mLineWidth.add(lineWidth)

                    lineWidth = 0
                    lineHeight = childHeight + lp.topMargin + lp.bottomMargin
                    lineViews = ArrayList()
                }
                lineWidth += childWidth + lp.leftMargin + lp.rightMargin
                lineHeight = max(lineHeight, childHeight + lp.topMargin + lp.bottomMargin)
                lineViews.add(child)
            }
            mLineHeight.add(lineHeight)
            mLineWidth.add(lineWidth)
            mAllViews.add(lineViews)

            var left = paddingLeft
            var top = paddingTop

            val lineNum = mAllViews.size

            for (i in 0 until lineNum) {
                lineViews = mAllViews[i] as MutableList<View>
                lineHeight = mLineHeight[i]

                // set gravity
                val currentLineWidth = this.mLineWidth[i]
                when (this.mGravity) {
                    LEFT -> left = paddingLeft
                    CENTER -> left = (width - currentLineWidth) / 2 + paddingLeft
                    RIGHT -> left = width - currentLineWidth + paddingLeft
                }

                var marginBottom = 0
                for (j in lineViews.indices) {
                    val child = lineViews[j]
                    if (child.visibility == View.GONE) {
                        continue
                    }

                    val lp =
                        child
                            .layoutParams as MarginLayoutParams

                    val lc =
                        left + lp.leftMargin +
                            if (j != 0) {
                                itemOffset
                            } else {
                                0
                            }
                    val tc = top + lp.topMargin
                    val rc = lc + child.measuredWidth
                    val bc = tc + child.measuredHeight

                    child.layout(lc, tc, rc, bc)

                    left += (child.measuredWidth + lp.leftMargin + lp.rightMargin)
                    marginBottom = max(marginBottom, lp.bottomMargin)
                }
                top += lineHeight + marginBottom
            }
        }

        override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
            return MarginLayoutParams(context, attrs)
        }

        override fun generateDefaultLayoutParams(): LayoutParams {
            return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        override fun generateLayoutParams(p: LayoutParams): LayoutParams {
            return MarginLayoutParams(p)
        }

        companion object {
            private val TAG = "FlowLayout"
            private val LEFT = -1
            private val CENTER = 0
            private val RIGHT = 1
        }
    }
