package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import one.mixin.android.ui.setting.ui.compose.BadgeCircleImage
import one.mixin.android.vo.AssetItem

class BadgeCircleImageView(context: Context, attrs: AttributeSet?) :
    AbstractComposeView(context, attrs) {

    fun setContent(asset: AssetItem) {
        setContent(asset.iconUrl, asset.chainIconUrl)
    }

    fun setContent(
        icon: String?,
        subIcon: String?,
        iconBorderWidth: Int? = null,
        flip: Boolean = false,
    ) {
        this.icon = icon
        this.subIcon = subIcon
        this.iconBorderWidth = iconBorderWidth
        this.flip = flip
    }

    private var icon: String? = null
    private var subIcon: String? = null
    private var iconBorderWidth: Int? = null
    private var flip: Boolean = false

    @Composable
    override fun Content() {
        BadgeCircleImage(icon ?: "", subIcon ?: "", iconBorderWidth, flip)
    }
}
