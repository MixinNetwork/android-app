package one.mixin.android.ui.conversation.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_chat_menu.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.ui.conversation.holder.MenuHolder
import org.jetbrains.anko.dip

class MenuAdapter(private val onMenuClickListener: OnMenuClickListener) : RecyclerView.Adapter<MenuHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MenuHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_menu, parent, false))

    private val dp16 by lazy {
        MixinApplication.appContext.dip(16)
    }

    private val dp24 by lazy {
        MixinApplication.appContext.dip(24)
    }

    var showTransfer = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: MenuHolder, position: Int) {
        when (position) {
            0 -> holder.itemView.setPadding(dp16, 0, 0, 0)
            itemCount - 1 -> holder.itemView.setPadding(dp24, 0, dp16, 0)
            else -> holder.itemView.setPadding(dp24, 0, 0, 0)
        }
        val index = when {
            showTransfer -> position
            else -> position + 1
        }
        holder.itemView.menu_icon.setBackgroundResource(backgrounds[index])
        holder.itemView.menu_icon.setImageResource(icons[index])
        holder.itemView.menu_title.setText(titles[index])
        holder.itemView.setOnClickListener {
            onMenuClickListener.onMenuClick(ids[index])
        }
    }

    companion object {
        val icons = intArrayOf(R.drawable.ic_selector_transfer, R.drawable.ic_selector_camera, R.drawable
            .ic_selector_gallery, R.drawable.ic_selector_video, R.drawable.ic_selector_document)
        val backgrounds = intArrayOf(R.drawable.bg_selector_contact, R.drawable.bg_selector_camera, R.drawable
            .bg_selector_gallery, R.drawable.bg_selector_video, R.drawable.bg_selector_document)
        val titles = intArrayOf(R.string.transfer, R.string.camera, R.string.gallery, R.string.video, R.string.document)
        val ids = intArrayOf(R.id.menu_transfer, R.id.menu_camera, R.id.menu_gallery, R.id.menu_video, R.id
            .menu_document)
    }

    override fun getItemCount(): Int {
        return if (showTransfer) {
            icons.size
        } else {
            icons.size - 1
        }
    }

    interface OnMenuClickListener {
        fun onMenuClick(id: Int)
    }
}
