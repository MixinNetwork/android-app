package one.mixin.android.ui.home.bot

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Build
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemBotManagerBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.notEmptyWithElse
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
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val binding = ItemBotManagerBinding.bind(holder.itemView)
        list[position].let { app ->
            binding.avatar.renderApp(app)
            if (app is App) {
                binding.name.text = app.name
            } else if (app is Bot) {
                binding.name.text =
                    when (app.id) {
                        INTERNAL_WALLET_ID -> holder.itemView.context.getString(R.string.Wallet)
                        INTERNAL_CAMERA_ID -> holder.itemView.context.getString(R.string.Camera)
                        INTERNAL_SCAN_ID -> holder.itemView.context.getString(R.string.Scan_QR)
                        else -> app.name
                    }
            }
            holder.itemView.setOnClickListener {
                botCallBack.invoke(app)
            }
            binding.avatar.setOnClickListener {
                botCallBack.invoke(app)
            }
            binding.avatar.tag = position
            binding.avatar.setOnLongClickListener(this)
        }
    }

    override fun getItemCount(): Int {
        return list.notEmptyWithElse({ it.size }, 0)
    }

    val dragInstance: BotManagerDragListener
        get() = BotManagerDragListener()

    class ListViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    override fun onLongClick(v: View): Boolean {
        v.background = null
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = DragShadowBuilder(v)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.startDragAndDrop(data, shadowBuilder, v, DRAG_FLAG_OPAQUE)
        } else {
            @Suppress("DEPRECATION")
            v.startDrag(data, shadowBuilder, v, 0)
        }
        v.setOnDragListener { view, event ->
            if (event.action == DragEvent.ACTION_DRAG_ENDED) {
                val ctx = view.context
                view.background = ResourcesCompat.getDrawable(ctx.resources, R.drawable.mixin_ripple_large, ctx.theme)
            }
            return@setOnDragListener true
        }
        v.alpha = 0.2f
        v.context.clickVibrate()
        return false
    }
}
