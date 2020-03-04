package one.mixin.android.ui.common.recyclerview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class PagedHeaderAdapter<T>(diffCallback: DiffUtil.ItemCallback<T>) :
    PagedListAdapter<T, RecyclerView.ViewHolder>(diffCallback) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }

    var headerView: View? = null

    private var showHeader: Boolean = false

    var onItemListener: OnItemListener<T>? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && isShowHeader()) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount() =
        if (isShowHeader()) super.getItemCount() + 1 else super.getItemCount()

    protected fun getPos(position: Int): Int {
        return if (isShowHeader()) {
            position - 1
        } else {
            position
        }
    }

    private fun isShowHeader() = headerView != null && showHeader

    fun setShowHeader(show: Boolean, rv: RecyclerView) {
        if (show != showHeader) {
            showHeader = show
            rv.swapAdapter(this, false)
            notifyDataSetChanged()
        }
    }

    private var headerObserver: PagedHeaderAdapterDataObserver? = null

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        headerObserver = PagedHeaderAdapterDataObserver(observer, if (isShowHeader()) 1 else 0)
        super.registerAdapterDataObserver(headerObserver!!)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(headerObserver!!)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (if (viewType == TYPE_HEADER) {
            getHeaderViewHolder(parent.context, parent)
        } else {
            getNormalViewHolder(parent.context, parent)
        })
    }

    open fun getHeaderViewHolder(context: Context, parent: ViewGroup) = HeadHolder(headerView!!)
    abstract fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder

    open class HeadHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnItemListener<in T> {
        fun onNormalItemClick(item: T)
        fun onNormalLongClick(item: T): Boolean
    }
}
