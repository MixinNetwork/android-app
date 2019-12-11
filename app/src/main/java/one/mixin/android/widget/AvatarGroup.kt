package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_avatar_group.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.App

class AvatarGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    RelativeLayout(context, attrs, defStyle) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_avatar_group, this, true)
    }

    fun setApps(apps: List<App>) {
        if (apps.size >= 3) {
            avatar1.isVisible = true
            avatar2.isVisible = true
            avatar3.isVisible = true
            avatar1.loadImage(apps[0].icon_url)
            avatar2.loadImage(apps[1].icon_url)
            avatar3.loadImage(apps[2].icon_url)
        } else if (apps.size == 2) {
            avatar1.isVisible = true
            avatar2.isVisible = true
            avatar3.isVisible = false
            avatar1.loadImage(apps[0].icon_url)
            avatar2.loadImage(apps[1].icon_url)
        } else if (apps.size == 1) {
            avatar1.isVisible = true
            avatar2.isVisible = false
            avatar3.isVisible = false
            avatar1.loadImage(apps[0].icon_url)
        } else {
            avatar1.isVisible = false
            avatar2.isVisible = false
            avatar3.isVisible = false
        }
    }
}
