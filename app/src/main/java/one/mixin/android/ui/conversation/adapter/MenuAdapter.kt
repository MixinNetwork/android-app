package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_chat_menu.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.vo.App
import org.jetbrains.anko.padding

class MenuAdapter(
    private val isGroup: Boolean,
    private val isBot: Boolean,
    private val isSelfCreatedBot: Boolean
) : RecyclerView.Adapter<MenuAdapter.MenuHolder>() {

    private val buildInMenus = arrayListOf<Menu>().apply {
        add(Menu(MenuType.Camera, R.string.camera, R.drawable.ic_menu_camera, null))
        val transferMenu = Menu(MenuType.Transfer, R.string.transfer, R.drawable.ic_menu_transfer, null)
        val voiceMenu = Menu(MenuType.Voice, R.string.voice, R.drawable.ic_menu_call, null)
        if (isBot) {
            if (isSelfCreatedBot) {
                add(transferMenu)
            }
        } else if (!isGroup) {
            add(transferMenu)
            add(voiceMenu)
        }
        add(Menu(MenuType.File, R.string.document, R.drawable.ic_menu_file, null))
        add(Menu(MenuType.Contact, R.string.contact, R.drawable.ic_menu_contact, null))
    }

    var onMenuListener: OnMenuListener? = null

    var appList = listOf<App>()
        set(value) {
            if (field == value) return
            field = value

            menus = mutableListOf<Menu>().apply {
                addAll(buildInMenus)
            }
            for (app in appList) {
                menus.add(Menu(MenuType.App, null, null, app.icon_url, app.homeUri, app.name))
            }

            notifyDataSetChanged()
        }

    private var menus = mutableListOf<Menu>().apply {
        addAll(buildInMenus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MenuHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_menu, parent, false))

    override fun getItemCount() = menus.size

    override fun onBindViewHolder(holder: MenuHolder, position: Int) {
        val view = holder.itemView
        val ctx = view.context
        val menu = menus[position]
        if (menu.icon != null) {
            view.menu_icon.setImageResource(menu.icon)
            view.menu_icon.setBackgroundResource(R.drawable.bg_menu)
            view.menu_icon.padding = ctx.dpToPx(20f)
            menu.nameRes?.let {
                view.menu_title.text = ctx.getString(it)
            }
        } else {
            view.menu_icon.loadCircleImage(menu.iconUrl)
            view.menu_icon.padding = 0
            view.menu_title.text = menu.name
        }
        view.setOnClickListener {
            onMenuListener?.onMenuClick(menu)
        }
    }

    class MenuHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnMenuListener {
        fun onMenuClick(menu: Menu)
    }
}