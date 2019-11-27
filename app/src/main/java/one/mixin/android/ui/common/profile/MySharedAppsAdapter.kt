package one.mixin.android.ui.common.profile

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_shared_app.view.*
import one.mixin.android.R
import one.mixin.android.vo.App
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.ItemTouchHelperViewHolder
import one.mixin.android.widget.recyclerview.OnStartDragListener
import java.util.Collections

class MySharedAppsAdapter(
    private val dragStartListener: OnStartDragListener
) : RecyclerView.Adapter<MySharedAppsAdapter.ItemViewHolder>(),
    ItemTouchHelperAdapter {
    private val mItems = mutableListOf<App>()

    fun setData(apps: List<App>) {
        mItems.clear()
        mItems.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_shared_app, parent, false)
        return ItemViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position], dragStartListener)
    }

    override fun onItemDismiss(position: Int) {
        mItems.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(mItems, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    class ItemViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {
        override fun onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }

        fun bind(app: App, dragStartListener: OnStartDragListener) {
            itemView.avatar.setInfo(app.name, app.icon_url, app.appId)
            itemView.name.text = app.name
            itemView.handle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragStartListener.onStartDrag(this)
                }
                false
            }
        }
    }
}