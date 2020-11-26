package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_chat_menu.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.AppItem
import one.mixin.android.widget.BadgeCircleImageView

class MenuAdapter(
    private val isGroup: Boolean,
    private val isBot: Boolean,
    private val isSelfCreatedBot: Boolean
) : RecyclerView.Adapter<MenuAdapter.MenuHolder>() {

    private val buildInMenus = arrayListOf<Menu>().apply {
        val transferMenu =
            Menu(MenuType.Transfer, R.string.transfer, R.drawable.ic_menu_transfer, null)
        val voiceMenu = Menu(
            MenuType.Voice,
            if (isGroup) R.string.group_call else R.string.voice,
            R.drawable.ic_menu_call,
            null
        )
        if (isBot) {
            if (isSelfCreatedBot) {
                add(transferMenu)
            }
        } else if (!isGroup) {
            add(transferMenu)
            add(voiceMenu)
        } else {
            add(voiceMenu)
        }
        add(Menu(MenuType.Camera, R.string.camera, R.drawable.ic_menu_camera, null))
        add(Menu(MenuType.File, R.string.document, R.drawable.ic_menu_file, null))
        add(Menu(MenuType.Contact, R.string.contact, R.drawable.ic_menu_contact, null))
        add(Menu(MenuType.Location, R.string.location, R.drawable.ic_menu_location, null))
    }

    var onMenuListener: OnMenuListener? = null

    var appList = listOf<AppItem>()
        set(value) {
            if (field == value) return
            field = value

            menus = mutableListOf<Menu>().apply {
                addAll(buildInMenus)
            }
            for (app in appList) {
                menus.add(Menu(MenuType.App, null, null, app))
            }

            notifyDataSetChanged()
        }

    private var menus = mutableListOf<Menu>().apply {
        addAll(buildInMenus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MenuHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_chat_menu,
                parent,
                false
            )
        ).apply {
            itemView.app_icon.pos = BadgeCircleImageView.END_BOTTOM
        }

    override fun getItemCount() = menus.size

    override fun onBindViewHolder(holder: MenuHolder, position: Int) {
        val view = holder.itemView
        val ctx = view.context
        val menu = menus[position]
        if (menu.icon != null) {
            view.menu_icon.visibility = VISIBLE
            view.menu_icon.setImageResource(menu.icon)
            view.app_icon.visibility = GONE
            menu.nameRes?.let {
                view.menu_title.text = ctx.getString(it)
            }
        } else {
            view.app_icon.visibility = VISIBLE
            view.app_icon.bg.loadImage(menu.app?.iconUrl, R.drawable.ic_avatar_place_holder)
            if (!isGroup) {
                menu.app?.avatarUrl?.let {
                    view.app_icon.badge.loadImage(it, R.drawable.ic_avatar_place_holder)
                }
            }
            view.menu_icon.visibility = GONE
            view.menu_title.text = menu.app?.name
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
