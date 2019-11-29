package one.mixin.android.ui.common.info

import android.widget.ScrollView
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.DraggableViewHelper
import one.mixin.android.util.DraggableViewHelper.Companion.FLING_DOWN
import one.mixin.android.util.DraggableViewHelper.Companion.FLING_UP
import one.mixin.android.widget.BottomSheet

abstract class ScrollableBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    protected var min = 0
    protected var max = 0

    protected var expand = false
        set(value) {
            field = value
            whenSetExpand()
        }

    open fun whenSetExpand() {}

    protected fun setDraggableHelper(
        dialog: BottomSheet,
        scrollView: ScrollView
    ) {
        DraggableViewHelper(scrollView).apply {
            direction = DraggableViewHelper.DIRECTION_BOTH
            callback = object : DraggableViewHelper.Callback {
                override fun onScroll(dis: Float) {
                    dialog.updateTransitionY(dis, false)
                }

                override fun onRelease(fling: Int) {
                    val max = if (expand) max else min
                    val curH =
                        if (expand) max - dialog.getTransitionY() else min - dialog.getTransitionY()
                    val minMid = min / 2f
                    val maxMid = if (expand) {
                        min + (max - min) / 2f
                    } else minMid
                    val targetY = if (curH > min) {
                        if (fling == FLING_UP) {
                            max
                        } else if (fling == FLING_DOWN) {
                            min
                        } else {
                            if (curH <= maxMid) {
                                min
                            } else {
                                max
                            }
                        }
                    } else if (curH < min) {
                        if (fling == FLING_UP) {
                            min
                        } else if (fling == FLING_DOWN) {
                            0
                        } else {
                            if (curH > minMid) {
                                min
                            } else {
                                0
                            }
                        }
                    } else {
                        if (fling == FLING_UP) {
                            max
                        } else if (fling == FLING_DOWN) {
                            0
                        } else {
                            min
                        }
                    }
                    if (targetY == 0) {
                        dismiss()
                    } else {
                        dialog.updateTransitionY(max - targetY.toFloat(), true)
                    }
                    expand = targetY == max
                    isParentBottom2TopEnable = targetY != max
                }
            }
        }
    }
}
