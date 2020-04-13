package one.mixin.android.ui.home.bot

import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R

class BotManagerDragListener : OnDragListener {
    private var isDropped = false
    override fun onDrag(v: View, event: DragEvent): Boolean {
        if (event.action == DragEvent.ACTION_DROP) {
            isDropped = true
            var positionTarget = -1
            val viewSource = event.localState as View
            val viewId = v.id
            val flItem = R.id.item
            val rvBottom = R.id.bot_rv
            when (viewId) {
                flItem, rvBottom -> {
                    val target: RecyclerView
                    when (viewId) {
                        rvBottom -> target = v.rootView.findViewById(rvBottom)
                        else -> {
                            target = v.parent as RecyclerView
                            positionTarget = v.tag as Int
                        }
                    }
                    // if (viewSource != null) {
                    //     val source = viewSource.parent as RecyclerView
                    //     val adapterSource = source.adapter as BotManagerAdapter?
                    //     val positionSource = viewSource.tag as Int
                    //     val sourceId = source.id
                    //     val list = adapterSource!!.list!![positionSource]
                    //     val listSource = adapterSource.list
                    //     listSource.removeAt(positionSource)
                    //     adapterSource.updateList(listSource!!)
                    //     adapterSource.notifyDataSetChanged()
                    //     val adapterTarget = target.adapter as BotManagerAdapter?
                    //     val customListTarget = adapterTarget!!.list
                    //     if (positionTarget >= 0) {
                    //         customListTarget.add(positionTarget, list)
                    //     } else {
                    //         customListTarget.add(list)
                    //     }
                    //     adapterTarget.updateList(customListTarget!!)
                    //     adapterTarget.notifyDataSetChanged()
                    // }
                }
            }
        }

        // if (event.localState != null) {
        //     (event.localState as View).alpha = if (!isDropped) {
        //         1f
        //     } else {
        //         0.5f
        //     }
        // }
        return true
    }
}