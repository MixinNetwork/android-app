package one.mixin.android.ui.home.bot

import android.content.ClipData
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bot_manager.view.*
import one.mixin.android.R
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.tapVibrate
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface

class BotManagerAdapter(private val botCallBack: (BotInterface) -> Unit) : RecyclerView.Adapter<BotManagerAdapter.ListViewHolder>(), View.OnLongClickListener {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(
            parent.context
        ).inflate(R.layout.item_bot_manager, parent, false)
        return ListViewHolder(view)
    }

    var list: List<BotInterface> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        list[position].let { app ->
            holder.itemView.avatar.renderApp(app)
            if (app is App) {
                holder.itemView.name.text = app.name
            } else if (app is Bot) {
                holder.itemView.name.text = app.name
            }
            holder.itemView.setOnClickListener {
                botCallBack.invoke(app)
            }
            holder.itemView.avatar.setOnClickListener {
                botCallBack.invoke(app)
            }
            holder.itemView.avatar.tag = position
            holder.itemView.avatar.setOnLongClickListener(this)
        }
    }

    override fun getItemCount(): Int {
        return list.notEmptyWithElse({ it.size }, 0)
    }

    val dragInstance: BotManagerDragListener?
        get() = BotManagerDragListener()

    class ListViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    override fun onLongClick(v: View): Boolean {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = DragShadowBuilder(v)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.startDragAndDrop(data, shadowBuilder, v, DRAG_FLAG_OPAQUE)
        } else {
            @Suppress("DEPRECATION")
            v.startDrag(data, shadowBuilder, v, 0)
        }
        v.alpha = 0.2f
        v.context.tapVibrate()
        return false
    }
}
