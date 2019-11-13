package one.mixin.android.ui.common.info

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.layout_menu.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.roundTopOrBottom

@DslMarker
annotation class MenuDsl

fun menuList(block: MenuListBuilder.() -> Unit) = MenuListBuilder().apply(block).build()

fun menuGroup(block: MenuGroupBuilder.() -> Unit) = MenuGroupBuilder().apply(block).build()

fun menu(block: MenuBuilder.() -> Unit) = MenuBuilder().apply(block).build()

@MenuDsl
class MenuListBuilder {
    private var groups = arrayListOf<MenuGroup>()

    fun menuGroup(block: MenuGroupBuilder.() -> Unit) {
        groups.add(MenuGroupBuilder().apply(block).build())
    }

    fun build() = MenuList(groups)
}

@MenuDsl
class MenuGroupBuilder {
    private var menus = arrayListOf<Menu>()

    fun menu(block: MenuBuilder.() -> Unit) {
        menus.add(MenuBuilder().apply(block).build())
    }

    fun menu(menu: Menu) {
        menus.add(menu)
    }

    fun build() = MenuGroup(menus)
}

@MenuDsl
class MenuBuilder {
    var title: String = ""
    var subtitle: String? = null
    var style: MenuStyle = MenuStyle.Normal
    var action: (() -> Unit)? = null

    fun build() = Menu(title, subtitle, style, action)
}

data class MenuList(
    val groups: ArrayList<MenuGroup>
)

data class MenuGroup(
    val menus: ArrayList<Menu>
)

data class Menu(
    val title: String,
    val subtitle: String? = null,
    val style: MenuStyle = MenuStyle.Normal,
    val action: (() -> Unit)? = null
)

enum class MenuStyle {
    Normal, Danger
}

@SuppressLint("InflateParams")
fun MenuList.createMenuLayout(context: Context): ViewGroup {
    val listLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }
    val dp5 = context.dpToPx(5f)
    val dp13 = context.dpToPx(13f)
    val dp16 = context.dpToPx(16f)
    val dp64 = context.dpToPx(64f)
    groups.forEach { group ->
        val groupLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        group.menus.forEachIndexed { index, menu ->
            val menuLayout = LayoutInflater.from(context).inflate(R.layout.layout_menu, null, false)
            menuLayout.title_tv.text = menu.title
            menuLayout.subtitle_tv.text = menu.subtitle
            menuLayout.title_tv.setTextColor(if (menu.style == MenuStyle.Normal) {
                context.colorFromAttribute(R.attr.text_primary)
            } else {
                context.resources.getColor(R.color.colorRed, context.theme)
            })
            val top = index == 0
            val bottom = index == group.menus.size - 1
            menuLayout.roundTopOrBottom(dp13.toFloat(), top, bottom)
            menuLayout.setOnClickListener { menu.action?.invoke() }
            groupLayout.addView(menuLayout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp64))
        }
        listLayout.addView(groupLayout, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp16
            marginEnd = dp16
            topMargin = dp5
            bottomMargin = dp5
        })
    }
    return listLayout
}
