package one.mixin.android.ui.home.bot

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemBotManagerBinding
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface

class BotManagerAdapter(private val botCallBack: (BotInterface) -> Unit) : RecyclerView.Adapter<BotManagerAdapter.ListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ListViewHolder {
        val view =
            LayoutInflater.from(
                parent.context,
            ).inflate(R.layout.item_bot_manager, parent, false)
        return ListViewHolder(view)
    }

    var list: List<BotInterface> = listOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(
        holder: ListViewHolder,
        position: Int,
    ) {
        val binding = ItemBotManagerBinding.bind(holder.itemView)
        list[position].let { app ->
            binding.avatar.renderApp(app)
            if (app is App) {
                binding.name.text = app.name
            } else if (app is Bot) {
                binding.name.text =
                    when (app.id) {
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
        }
    }

    override fun getItemCount(): Int {
        return list.notEmptyWithElse({ it.size }, 0)
    }

    class ListViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
}
