package one.mixin.android.ui.common.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.extension.notNullWithElse

abstract class HeaderFooterAdapter<T> : HeaderAdapter<T>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
        const val TYPE_FOOTER = 2
    }

    var footerView: View? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null) {
            TYPE_HEADER
        } else if (position == itemCount - 1 && footerView != null) {
            TYPE_FOOTER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = data.notNullWithElse(
        {
            if (headerView != null && footerView != null) {
                it.size + 2
            } else if (headerView != null || footerView != null) {
                it.size + 1
            } else {
                it.size
            }
        },
        0
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            getHeaderViewHolder()
        } else if (viewType == TYPE_FOOTER) {
            getFooterViewHolder()
        } else {
            getNormalViewHolder(parent.context, parent)
        }
    }

    open fun getFooterViewHolder() = FootHolder(footerView!!)
    open class FootHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
