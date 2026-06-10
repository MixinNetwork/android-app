package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.databinding.ViewRoundTitleBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

class RoundTitleView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val binding = ViewRoundTitleBinding.inflate(LayoutInflater.from(context), this, true)
    val leftIv = binding.leftIv
    val rightIv = binding.rightIv
    val titleTv = binding.titleTv

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundTitleView)
        if (ta.hasValue(R.styleable.RoundTitleView_title_text)) {
            binding.titleTv.text = ta.getString(R.styleable.RoundTitleView_title_text)
        }
        if (ta.hasValue(R.styleable.RoundTitleView_right_icon)) {
            binding.rightIv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_right_icon, 0))
            binding.rightIv.visibility = View.VISIBLE
        }
        if (ta.hasValue(R.styleable.RoundTitleView_left_icon)) {
            binding.leftIv.setImageResource(ta.getResourceId(R.styleable.RoundTitleView_left_icon, 0))
            binding.leftIv.visibility = View.VISIBLE
        } else {
            binding.titleLl.updateLayoutParams<LayoutParams> {
                marginStart = context.dpToPx(20f)
            }
        }
        ta.recycle()
    }

    fun hideLeftIv() {
        binding.leftIv.visibility = View.GONE
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = context.dpToPx(20f)
        }
    }

    fun showLeftIv() {
        binding.leftIv.visibility = View.VISIBLE
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = 0
        }
    }

    fun showAvatar(user: User) {
        binding.avatarIv.visibility = VISIBLE
        binding.avatarIv.setTextSize(16f)
        binding.avatarIv.setInfo(user.fullName, user.avatarUrl, user.userId)
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = 0
        }
    }

    fun showBadgeCircleView(asset: TokenItem) {
        binding.badgeCircleIv.isVisible = true
        binding.badgeCircleIv.loadToken(asset)
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = 0
        }
    }

    fun showAddressAvatar() {
        binding.addressAvatar.isVisible = true
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = 0
        }
    }

    fun setSubTitle(
        first: String,
        second: String? = null,
    ) {
        binding.titleTv.text = first
        if (second.isNullOrBlank()) {
            binding.subTitleTv.visibility = View.GONE
        } else {
            binding.subTitleTv.visibility = View.VISIBLE
            binding.subTitleTv.text = second
        }
    }

    fun roundClose() {
        binding.rightIv.setImageResource(R.drawable.ic_close)
    }

    fun centerTitle() {
        binding.titleLl.updateLayoutParams<LayoutParams> {
            marginStart = 0
            addRule(CENTER_IN_PARENT, TRUE)
            removeRule(END_OF)
        }
        binding.titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    }
}
