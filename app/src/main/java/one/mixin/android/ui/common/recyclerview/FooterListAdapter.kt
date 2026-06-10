package one.mixin.android.ui.common.recyclerview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class FooterListAdapter<T, VH : RecyclerView.ViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback) {
    companion object {
        const val TYPE_FOOTER = 0
        const val TYPE_NORMAL = 1
    }

    var footerView: View? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1 && footerView != null) {
            TYPE_FOOTER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int {
        return if (footerView != null) {
            super.getItemCount() + 1
        } else {
            super.getItemCount()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH {
        return if (viewType == TYPE_FOOTER) {
            getFootViewHolder() as VH
        } else {
            getNormalViewHolder(parent.context, parent) as VH
        }
    }

    open fun getFootViewHolder() = FootHolder(footerView!!)

    abstract fun getNormalViewHolder(
        context: Context,
        parent: ViewGroup,
    ): NormalHolder

    open class FootHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
