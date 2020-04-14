package one.mixin.android.ui.home.bot

import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.vo.App
import one.mixin.android.widget.bot.BotDock
import timber.log.Timber

class BotManagerDragListener : OnDragListener {
    private var isDropped = false
    override fun onDrag(v: View, event: DragEvent): Boolean {
        Timber.d("${event.action}")
        val viewSource = event.localState as View? ?: return false
        if (event.action == DragEvent.ACTION_DROP) {
            isDropped = true
            Timber.d("$v.id")
            Timber.d("${R.id.dock_1} ${R.id.dock_2} ${R.id.dock_3} ${R.id.dock_4}")

            when (v.id) {
                R.id.dock_1, R.id.dock_2, R.id.dock_3, R.id.dock_4, R.id.bot_dock -> {
                    val source = viewSource.parent.parent as RecyclerView
                    val adapterSource = source.adapter as BotManagerAdapter? ?: return false
                    val list = adapterSource.list ?: return false
                    val positionSource = viewSource.tag as Int
                    if (v.id == R.id.bot_dock) {
                        (v as BotDock).addApp(list[positionSource])
                    } else {
                        (v.parent as BotDock).addApp(list[positionSource])
                    }
                }
                R.id.bot_rv -> {
                    val positionSource = viewSource.tag as App
                    (viewSource.parent.parent as BotDock).remove(positionSource)
                }
                else -> {
                }
            }
        }

        return true
    }
}
