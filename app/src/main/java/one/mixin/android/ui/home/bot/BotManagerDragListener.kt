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
    override fun onDrag(v: View, event: DragEvent): Boolean {
        Timber.d("${event.action}")
        val viewSource = event.localState as View? ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                when (v.id) {
                    R.id.dock_1 -> {
                        Timber.d("111")
                    }
                    R.id.dock_2 -> {
                        Timber.d("222")
                    }
                    R.id.dock_3 -> {
                        Timber.d("333")
                    }
                    R.id.dock_4 -> {
                        Timber.d("444")
                    }
                    R.id.bot_dock -> {
                    }
                }
            }
            DragEvent.ACTION_DROP -> {
                when (v.id) {
                    R.id.dock_1, R.id.dock_2, R.id.dock_3, R.id.dock_4, R.id.bot_dock -> {
                        val source = viewSource.parent.parent
                        if (source is RecyclerView) {
                            val adapterSource = source.adapter as BotManagerAdapter? ?: return false
                            val list = adapterSource.list ?: return false
                            val positionSource = viewSource.tag as Int
                            if (v.id == R.id.bot_dock) {
                                (v as BotDock).addApp(list[positionSource])
                            } else {
                                (v.parent as BotDock).addApp(list[positionSource])
                            }
                        }
                    }
                    R.id.bot_rv -> {
                        if (viewSource.tag is Int) return false
                        val positionSource = viewSource.tag as AppInterface
                        (viewSource.parent.parent as BotDock).remove(positionSource)
                    }
                    else -> {
                    }
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                viewSource.alpha = 1f
            }
        }

        return true
    }
}
