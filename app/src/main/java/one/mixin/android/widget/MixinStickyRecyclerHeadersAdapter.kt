package one.mixin.android.widget

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter

interface MixinStickyRecyclerHeadersAdapter<VH : RecyclerView.ViewHolder> : StickyRecyclerHeadersAdapter<VH> {
    fun onCreateAttach(parent: ViewGroup): View
    fun getAttachIndex(): Int?
    fun onBindAttachView(view: View)
    fun isLast(position: Int): Boolean
    fun isListLast(position: Int): Boolean
}
