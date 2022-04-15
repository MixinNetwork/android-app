package one.mixin.android.ui.conversation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatMenuBinding
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
            Menu(MenuType.Transfer, R.string.Transfer, R.drawable.ic_menu_transfer, null)
        val voiceMenu = Menu(
            MenuType.Voice,
            if (isGroup) R.string.Group_Call else R.string.Voice,
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
        add(Menu(MenuType.Camera, R.string.Camera, R.drawable.ic_menu_camera, null))
        add(Menu(MenuType.File, R.string.Document, R.drawable.ic_menu_file, null))
        add(Menu(MenuType.Contact, R.string.Contact, R.drawable.ic_menu_contact, null))
        add(Menu(MenuType.Location, R.string.Location, R.drawable.ic_menu_location, null))
    }

    var onMenuListener: OnMenuListener? = null

    var appList = listOf<AppItem>()
        @SuppressLint("NotifyDataSetChanged")
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
        MenuHolder(ItemChatMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply {
            binding.appIcon.pos = BadgeCircleImageView.END_BOTTOM
        }

    override fun getItemCount() = menus.size

    override fun onBindViewHolder(holder: MenuHolder, position: Int) {
        val binding = holder.binding
        val ctx = holder.itemView.context
        val menu = menus[position]
        if (menu.icon != null) {
            binding.menuIcon.visibility = VISIBLE
            binding.menuIcon.setImageResource(menu.icon)
            binding.appIcon.visibility = GONE
            menu.nameRes?.let {
                binding.menuTitle.text = ctx.getString(it)
            }
        } else {
            binding.appIcon.visibility = VISIBLE
            binding.appIcon.bg.loadImage(menu.app?.iconUrl, R.drawable.ic_avatar_place_holder)
            if (!isGroup) {
                menu.app?.avatarUrl?.let {
                    binding.appIcon.badge.loadImage(it, R.drawable.ic_avatar_place_holder)
                }
            }
            binding.menuIcon.visibility = GONE
            binding.menuTitle.text = menu.app?.name
        }
        binding.root.setOnClickListener {
            onMenuListener?.onMenuClick(menu)
        }
    }

    class MenuHolder(val binding: ItemChatMenuBinding) : RecyclerView.ViewHolder(binding.root)

    interface OnMenuListener {
        fun onMenuClick(menu: Menu)
    }
}
