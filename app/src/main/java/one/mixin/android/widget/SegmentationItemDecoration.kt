package one.mixin.android.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import one.mixin.android.extension.dp

class SegmentationItemDecoration(val offset: Int = 8.dp) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (view.tag != null && view.tag as Boolean) {
            outRect.top = offset
        }
    }
}
