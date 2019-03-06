package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import one.mixin.android.R

class BadgeTextAvatarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_badge_avatar_text, this, true)
    }
}