package one.mixin.android.ui.common.recyclerview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.extension.notNullWithElse

abstract class FooterAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_FOOTER = 0
        const val TYPE_NORMAL = 1
    }

    var footerView: View? = null
    open var data: List<T>? = null

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1 && footerView != null) {
            TYPE_FOOTER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = data.notNullWithElse(
        {
            if (footerView != null) it.size + 1 else it.size
        },
        0,
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FOOTER) {
            getFootViewHolder()
        } else {
            getNormalViewHolder(parent.context, parent)
        }
    }

    open fun getFootViewHolder() = FootHolder(footerView!!)
    abstract fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder

    open class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    open class FootHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
