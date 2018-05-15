package one.mixin.android.ui.common.itemdecoration

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.view.View
import one.mixin.android.extension.dpToPx

class SpaceItemDecoration(val position: Int = 0) : RecyclerView.ItemDecoration() {

    private val divider = ColorDrawable(Color.parseColor("#ECECEC"))

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) >=position ) {
            outRect.top = parent.context.dpToPx(.5f)
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val itemCount = parent.childCount
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until itemCount) {
            val child = parent.getChildAt(i)
            if (child != null && parent.getChildAdapterPosition(child) >= position) {
                val lp = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + lp.bottomMargin
                val bottom = top + parent.context.dpToPx(.5f)
                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }
}