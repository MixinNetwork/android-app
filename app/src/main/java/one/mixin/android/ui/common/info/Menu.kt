package one.mixin.android.ui.common.info

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.LayoutMenuBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.textColor
import one.mixin.android.vo.App
import one.mixin.android.widget.FlowLayout

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
    var icon: Int? = null
    var apps: List<App>? = null
    var circleNames: List<String>? = null

    fun build() = Menu(title, subtitle, style, action, icon, apps, circleNames)
}

data class MenuList(
    val groups: ArrayList<MenuGroup>,
)

data class MenuGroup(
    val menus: ArrayList<Menu>,
)

data class Menu(
    val title: String,
    val subtitle: String? = null,
    val style: MenuStyle = MenuStyle.Normal,
    val action: (() -> Unit)? = null,
    val icon: Int? = null,
    val apps: List<App>? = null,
    val circleNames: List<String>? = null,
)

enum class MenuStyle {
    Normal,
    Danger,
    Info,
}

@SuppressLint("InflateParams")
fun MenuList.createMenuLayout(
    context: Context,
    createdAt: String? = null,
): ViewGroup {
    val listLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
    val dp5 = context.dpToPx(5f)
    val dp13 = context.dpToPx(13f)
    val dp16 = context.dpToPx(16f)
    val dp56 = context.dpToPx(56f)
    groups.forEach { group ->
        val groupLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
        group.menus.forEachIndexed { index, menu ->
            val menuBinding = LayoutMenuBinding.inflate(LayoutInflater.from(context), null, false)
            menuBinding.titleTv.text = menu.title
            menuBinding.subtitleTv.text = menu.subtitle
            menuBinding.titleTv.setTextColor(
                when (menu.style) {
                    MenuStyle.Normal -> {
                        context.colorFromAttribute(R.attr.text_primary)
                    }
                    MenuStyle.Info -> {
                        context.resources.getColor(R.color.colorBlue, context.theme)
                    }
                    else -> {
                        context.resources.getColor(R.color.colorRed, context.theme)
                    }
                },
            )
            val icon = menu.icon
            if (icon != null) {
                menuBinding.icon.isVisible = true
                menuBinding.icon.setImageResource(icon)
            } else {
                menuBinding.icon.isVisible = false
            }
            val apps = menu.apps
            if (apps != null) {
                menuBinding.avatarGroup.isVisible = true
                menuBinding.avatarGroup.setApps(apps)
            } else {
                menuBinding.avatarGroup.isVisible = false
            }
            val circleNames = menu.circleNames
            if (circleNames != null) {
                menuBinding.flowLayout.isVisible = true
                addCirclesLayout(context, circleNames, menuBinding.flowLayout)
            } else {
                menuBinding.flowLayout.isVisible = false
            }
            val top = index == 0
            val bottom = index == group.menus.size - 1
            menuBinding.root.roundTopOrBottom(dp13.toFloat(), top, bottom)
            menuBinding.root.setOnClickListener { menu.action?.invoke() }
            groupLayout.addView(
                menuBinding.root,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp56),
            )
        }

        listLayout.addView(
            groupLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp16
                marginEnd = dp16
                topMargin = dp5
                bottomMargin = dp5
            },
        )
    }
    if (createdAt != null) {
        listLayout.addView(
            TextView(context).apply {
                text = createdAt
                textColor = context.colorFromAttribute(R.attr.text_assist)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp5
            },
        )
    }
    return listLayout
}

private fun addCirclesLayout(
    context: Context,
    circles: List<String>,
    flowLayout: FlowLayout,
) {
    val dp12 = context.dpToPx(12f)
    val dp4 = context.dpToPx(4f)
    circles.forEach { name ->
        val tv =
            TextView(context).apply {
                setBackgroundResource(R.drawable.bg_round_rect_gray_border)
                text = name
                setTextColor(context.colorAttr(R.attr.text_remarks))
                setPadding(dp12, dp4, dp12, dp4)
            }
        flowLayout.addView(tv)
        (tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
    }
}
