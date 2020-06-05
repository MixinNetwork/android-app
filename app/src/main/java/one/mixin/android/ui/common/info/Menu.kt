package one.mixin.android.ui.common.info

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.layout_menu.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.vo.App
import one.mixin.android.widget.FlowLayout
import org.jetbrains.anko.colorAttr

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
    var apps: List<App>? = null
    var circleNames: List<String>? = null

    fun build() = Menu(title, subtitle, style, action, apps, circleNames)
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
    val action: (() -> Unit)? = null,
    val apps: List<App>? = null,
    val circleNames: List<String>? = null
)

enum class MenuStyle {
    Normal, Danger
}

@SuppressLint("InflateParams")
fun MenuList.createMenuLayout(
    context: Context
): ViewGroup {
    val listLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    val dp5 = context.dpToPx(5f)
    val dp13 = context.dpToPx(13f)
    val dp16 = context.dpToPx(16f)
    val dp56 = context.dpToPx(56f)
    groups.forEach { group ->
        val groupLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        group.menus.forEachIndexed { index, menu ->
            val menuLayout = LayoutInflater.from(context).inflate(R.layout.layout_menu, null, false)
            menuLayout.title_tv.text = menu.title
            menuLayout.subtitle_tv.text = menu.subtitle
            menuLayout.title_tv.setTextColor(
                if (menu.style == MenuStyle.Normal) {
                    context.colorFromAttribute(R.attr.text_primary)
                } else {
                    context.resources.getColor(R.color.colorRed, context.theme)
                }
            )
            menu.apps.notNullWithElse(
                {
                    menuLayout.avatar_group.isVisible = true
                    menuLayout.avatar_group.setApps(it)
                },
                {
                    menuLayout.avatar_group.isVisible = false
                }
            )
            menu.circleNames.notNullWithElse(
                {
                    menuLayout.flow_layout.isVisible = true
                    addCirclesLayout(context, it, menuLayout.flow_layout)
                },
                {
                    menuLayout.flow_layout.isVisible = false
                }
            )
            val top = index == 0
            val bottom = index == group.menus.size - 1
            menuLayout.roundTopOrBottom(dp13.toFloat(), top, bottom)
            menuLayout.setOnClickListener { menu.action?.invoke() }
            groupLayout.addView(
                menuLayout,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp56)
            )
        }
        listLayout.addView(
            groupLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp16
                marginEnd = dp16
                topMargin = dp5
                bottomMargin = dp5
            }
        )
    }
    return listLayout
}

private fun addCirclesLayout(
    context: Context,
    circles: List<String>,
    flowLayout: FlowLayout
) {
    val dp12 = context.dpToPx(12f)
    val dp4 = context.dpToPx(4f)
    circles.forEach { name ->
        val tv = TextView(context).apply {
            setBackgroundResource(R.drawable.bg_round_rect_gray_border)
            text = name
            setTextColor(context.colorAttr(R.attr.text_remarks))
            setPadding(dp12, dp4, dp12, dp4)
        }
        flowLayout.addView(tv)
        (tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
    }
}
