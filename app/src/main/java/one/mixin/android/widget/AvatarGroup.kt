package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_avatar_group.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.App

@SuppressLint("CustomViewStyleable")
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
                24.dp
            )
            val margin = ta.getDimensionPixelSize(R.styleable.AvatarsGroup_avatar_group_margin, 16.dp)
            setSize(size, margin)
            val color = ta.getColor(R.styleable.AvatarsGroup_avatar_group_border_color, context.colorFromAttribute(R.attr.bg_gray_light))
            avatar1.borderColor = color
            avatar2.borderColor = color
            avatar3.borderColor = color
            ta.recycle()
        }
    }

    fun setSize(size: Int, margin: Int) {
        avatar1.layoutParams = (avatar1.layoutParams as MarginLayoutParams).apply {
            width = size
            height = size
        }
        avatar2.layoutParams = (avatar2.layoutParams as MarginLayoutParams).apply {
            marginStart = margin
            width = size
            height = size
        }
        avatar3.layoutParams = (avatar3.layoutParams as MarginLayoutParams).apply {
            marginStart = margin * 2
            width = size
            height = size
        }
    }

    fun setUrls(urls: List<String?>) {
        when {
            urls.size >= 3 -> {
                avatar1.isVisible = true
                avatar2.isVisible = true
                avatar3.isVisible = true
                avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
                avatar3.loadImage(urls[2], R.drawable.ic_avatar_place_holder)
            }
            urls.size == 2 -> {
                avatar1.isVisible = true
                avatar2.isVisible = true
                avatar3.isVisible = false
                avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
            }
            urls.size == 1 -> {
                avatar1.isVisible = true
                avatar2.isVisible = false
                avatar3.isVisible = false
                avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
            }
            else -> {
                avatar1.isVisible = false
                avatar2.isVisible = false
                avatar3.isVisible = false
            }
        }
    }

    fun setApps(apps: List<App>) {
        setUrls(
            apps.map {
                it.iconUrl
            }
        )
    }
}
