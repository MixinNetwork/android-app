package one.mixin.android.util

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class KeyBoardAssist  constructor(content: ViewGroup) {

    private val mChildOfContent: View = content.getChildAt(0)
    private var usableHeightPrevious: Int = 0
    private val frameLayoutParams: FrameLayout.LayoutParams
    var keyBoardShow = false

    init {
        mChildOfContent.viewTreeObserver.addOnGlobalLayoutListener { possiblyResizeChildOfContent() }
        frameLayoutParams = mChildOfContent.layoutParams as FrameLayout.LayoutParams
    }

    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow != usableHeightPrevious) {
            val usableHeightSansKeyboard = mChildOfContent.rootView.height
            val heightDifference = usableHeightSansKeyboard - usableHeightNow
            if (heightDifference > usableHeightSansKeyboard / 4) {
                keyBoardShow = true
                frameLayoutParams.height = usableHeightSansKeyboard - heightDifference
            } else {
                keyBoardShow = false
                frameLayoutParams.height = usableHeightSansKeyboard
            }
            mChildOfContent.requestLayout()
            usableHeightPrevious = usableHeightNow
        }
    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        mChildOfContent.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    companion object {
        fun assistContent(contentView: ViewGroup): KeyBoardAssist {
            return KeyBoardAssist(contentView)
        }
    }
}