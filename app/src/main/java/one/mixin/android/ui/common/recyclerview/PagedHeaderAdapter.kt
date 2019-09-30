package one.mixin.android.ui.common.recyclerview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class PagedHeaderAdapter<T, VH : RecyclerView.ViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) :
    PagedListAdapter<T, VH>(diffCallback) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }

    var headerView: View? = null

    var onItemListener: OnItemListener? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount() = if (headerView != null) super.getItemCount() + 1 else super.getItemCount()

    protected fun getPos(position: Int): Int {
        return if (headerView != null) {
            position - 1
        } else {
            position
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return (if (viewType == TYPE_HEADER) {
            getHeaderViewHolder()
        } else {
            getNormalViewHolder(parent.context, parent)
        }) as VH
    }

    open fun getHeaderViewHolder() = HeadHolder(headerView!!)
    abstract fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder

    open class HeadHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnItemListener {
        fun <T> onNormalItemClick(item: T)
    }
}
