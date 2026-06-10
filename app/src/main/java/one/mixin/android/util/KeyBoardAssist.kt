package one.mixin.android.util

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.statusBarHeight

class KeyBoardAssist constructor(content: ViewGroup, private val isFull: Boolean = false) {
    private val mChildOfContent: View = content.getChildAt(0)
    private var usableHeightPrevious: Int = 0
    private val layoutParams: ViewGroup.LayoutParams
    private var firstIn = true

    init {
        mChildOfContent.viewTreeObserver.addOnGlobalLayoutListener { possiblyResizeChildOfContent() }
        layoutParams = mChildOfContent.layoutParams as ViewGroup.LayoutParams
    }

    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow != usableHeightPrevious) {
            if (!firstIn) {
                val usableHeightSansKeyboard = mChildOfContent.rootView.height
                val heightDifference = usableHeightSansKeyboard - usableHeightNow
                if (heightDifference > usableHeightSansKeyboard / 4) {
                    layoutParams.height = usableHeightSansKeyboard - heightDifference
                } else {
                    layoutParams.height = usableHeightSansKeyboard -
                        if (isFull) {
                            0
                        } else {
                            mChildOfContent.context.statusBarHeight() +
                                if (mChildOfContent.context.hasNavigationBar()) {
                                    mChildOfContent.context.navigationBarHeight()
                                } else {
                                    0
                                }
                        }
                }
                mChildOfContent.requestLayout()
            }
            firstIn = false
            usableHeightPrevious = usableHeightNow
        }
    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        mChildOfContent.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    companion object {
        fun assistContent(
            contentView: ViewGroup,
            isFull: Boolean,
        ): KeyBoardAssist {
            return KeyBoardAssist(contentView, isFull)
        }
    }
}
