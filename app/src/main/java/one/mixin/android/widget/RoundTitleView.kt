package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User

class RoundTitleView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_round_title, this, true)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundTitleView)
        if (ta != null) {
            if (ta.hasValue(R.styleable.RoundTitleView_title_text)) {
                title_tv.text = ta.getString(R.styleable.RoundTitleView_title_text)
            }
            if (ta.hasValue(R.styleable.RoundTitleView_right_icon)) {
                right_iv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_right_icon, 0))
                right_iv.visibility = View.VISIBLE
            }
            if (ta.hasValue(R.styleable.RoundTitleView_left_icon)) {
                left_iv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_left_icon, 0))
                left_iv.visibility = View.VISIBLE
            } else {
                title_ll.updateLayoutParams<RelativeLayout.LayoutParams> {
                    marginStart = context.dpToPx(20f)
                }
            }
            ta.recycle()
        }
    }

    fun hideLeftIv() {
        left_iv.visibility = View.GONE
        title_ll.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = context.dpToPx(20f)
        }
    }

    fun showLeftIv() {
        left_iv.visibility = View.VISIBLE
        title_ll.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = 0
        }
    }

    fun showAvatar(user: User) {
        avatar_iv.visibility = VISIBLE
        avatar_iv.setTextSize(16f)
        avatar_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
        title_ll.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = 0
        }
    }

    fun showBadgeCircleView(asset: AssetItem) {
        badge_circle_iv.isVisible = true
        bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        title_ll.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = 0
        }
    }

    fun setSubTitle(first: String, second: String) {
        title_tv.text = first
        if (second.isBlank()) {
            sub_title_tv.visibility = View.GONE
            title_tv.textSize = 22f
        } else {
            sub_title_tv.visibility = View.VISIBLE
            title_tv.textSize = 18f
            sub_title_tv.text = second
        }
    }
}