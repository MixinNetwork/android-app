package one.mixin.android.ui.home.inscription

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class InscriptionSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount

        outRect.bottom = spacing

    }
}
