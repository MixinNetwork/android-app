package one.mixin.android.ui.home.bot

import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.vo.BotInterface
import one.mixin.android.widget.bot.BotDock

class BotManagerDragListener : OnDragListener {
    override fun onDrag(
        v: View,
        event: DragEvent,
    ): Boolean {
        val viewSource = event.localState as View? ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_ENDED -> {
                if (viewSource.tag is BotInterface && v.id == R.id.dock_1 && v.id == R.id.dock_2 && v.id == R.id.dock_3 && v.id == R.id.dock_4) {
                    (v.parent as BotDock).render()
                } else if (v.id == R.id.bot_dock) {
                    (v as BotDock).render()
                }
                viewSource.alpha = 1f
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                if (viewSource.tag is BotInterface) {
                    when (v.id) {
                        R.id.dock_1 -> {
                            (v.parent as BotDock).shove(1, viewSource.tag as BotInterface)
                        }
                        R.id.dock_2 -> {
                            (v.parent as BotDock).shove(2, viewSource.tag as BotInterface)
                        }
                        R.id.dock_3 -> {
                            (v.parent as BotDock).shove(3, viewSource.tag as BotInterface)
                        }
                        R.id.dock_4 -> {
                            (v.parent as BotDock).shove(4, viewSource.tag as BotInterface)
                        }
                        R.id.bot_dock -> {
                        }
                    }
                } else if (viewSource.tag is Int) {
                    val source = viewSource.parent.parent
                    if (source is RecyclerView) {
                        if (v.id != R.id.bot_dock) {
                            when (v.id) {
                                R.id.dock_1 -> {
                                    0
                                }
                                R.id.dock_2 -> {
                                    1
                                }
                                R.id.dock_3 -> {
                                    2
                                }
                                R.id.dock_4 -> {
                                    3
                                }
                                else -> {
                                    null
                                }
                            }?.let { position ->
                                (v.parent as BotDock).shove(position)
                            }
                        }
                    }
                }
            }
            DragEvent.ACTION_DROP -> {
                when (v.id) {
                    R.id.dock_1, R.id.dock_2, R.id.dock_3, R.id.dock_4, R.id.bot_dock -> {
                        val source = viewSource.parent.parent
                        if (source is RecyclerView) {
                            val adapterSource = source.adapter as BotManagerAdapter? ?: return false
                            val list = adapterSource.list
                            val positionSource = viewSource.tag as Int
                            val position =
                                when (v.id) {
                                    R.id.dock_1 -> {
                                        0
                                    }
                                    R.id.dock_2 -> {
                                        1
                                    }
                                    R.id.dock_3 -> {
                                        2
                                    }
                                    else -> {
                                        3
                                    }
                                }
                            if (v.id == R.id.bot_dock) {
                                (v as BotDock).addApp(position, list[positionSource])
                            } else {
                                (v.parent as BotDock).addApp(position, list[positionSource])
                            }
                        } else if (source is BotDock) {
                            val position =
                                when (v.id) {
                                    R.id.dock_1 -> {
                                        0
                                    }
                                    R.id.dock_2 -> {
                                        1
                                    }
                                    R.id.dock_3 -> {
                                        2
                                    }
                                    else -> {
                                        3
                                    }
                                }
                            source.switch(position, viewSource.tag as BotInterface)
                        }
                    }
                    R.id.bot_rv -> {
                        if (viewSource.tag is Int) return false
                        val positionSource = viewSource.tag as BotInterface
                        (viewSource.parent.parent as BotDock).remove(positionSource)
                    }
                    else -> {
                    }
                }
            }
        }

        return true
    }
}
