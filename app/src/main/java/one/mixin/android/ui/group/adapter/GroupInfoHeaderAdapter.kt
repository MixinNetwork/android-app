package one.mixin.android.ui.group.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class GroupInfoHeaderAdapter : RecyclerView.Adapter<GroupInfoHeaderAdapter.HeaderHolder>() {
    var headerView: View? = null
    private var visible = true

    fun show(show: Boolean) {
        if (visible == show) return
        visible = show
        if (show) {
            notifyItemInserted(0)
        } else {
            notifyItemRemoved(0)
        }
    }

    override fun getItemCount(): Int = if (visible && headerView != null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder =
        HeaderHolder(headerView!!)

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        // static header, nothing to bind
    }

    class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
