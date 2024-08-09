package one.mixin.android.widget

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter

interface MixinStickyRecyclerHeadersAdapter<VH : RecyclerView.ViewHolder> : StickyRecyclerHeadersAdapter<VH> {
    fun onCreateAttach(parent: ViewGroup): View

    fun hasAttachView(position: Int): Boolean

    fun onBindAttachView(view: View)

    fun isLast(position: Int): Boolean

    fun isButtonGroup(position: Int): Boolean

    fun isListLast(position: Int): Boolean
}
