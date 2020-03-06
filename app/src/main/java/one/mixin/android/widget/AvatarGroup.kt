package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_avatar_group.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.App
import org.jetbrains.anko.dip

class AvatarGroup @JvmOverloads constructor(
    context: Context,
    val attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    RelativeLayout(context, attrs, defStyle) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_avatar_group, this, true)
        attrs?.let {
            attrs
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarsGroup)
            val size = ta.getDimensionPixelSize(
                R.styleable.AvatarsGroup_avatar_group_size,
                context.dip(24)
            )
            avatar1.layoutParams = avatar1.layoutParams.apply {
                width = size
                height = size
            }
            avatar2.layoutParams = avatar2.layoutParams.apply {
                width = size
                height = size
            }
            avatar3.layoutParams = avatar3.layoutParams.apply {
                width = size
                height = size
            }
            val color = ta.getColor(R.styleable.AvatarsGroup_avatar_group_border_color, context.colorFromAttribute(R.attr.bg_gray_light))
            avatar1.borderColor = color
            avatar2.borderColor = color
            avatar3.borderColor = color
            ta.recycle()
        }
    }

    fun setApps(apps: List<App>) {
        when {
            apps.size >= 3 -> {
                avatar1.isVisible = true
                avatar2.isVisible = true
                avatar3.isVisible = true
                avatar1.loadImage(apps[0].iconUrl)
                avatar2.loadImage(apps[1].iconUrl)
                avatar3.loadImage(apps[2].iconUrl)
            }
            apps.size == 2 -> {
                avatar1.isVisible = true
                avatar2.isVisible = true
                avatar3.isVisible = false
                avatar1.loadImage(apps[0].iconUrl)
                avatar2.loadImage(apps[1].iconUrl)
            }
            apps.size == 1 -> {
                avatar1.isVisible = true
                avatar2.isVisible = false
                avatar3.isVisible = false
                avatar1.loadImage(apps[0].iconUrl)
            }
            else -> {
                avatar1.isVisible = false
                avatar2.isVisible = false
                avatar3.isVisible = false
            }
        }
    }
}
