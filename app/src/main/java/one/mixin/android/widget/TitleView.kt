package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.dp
import one.mixin.android.vo.User

class TitleView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val binding: ViewTitleBinding =
        ViewTitleBinding.inflate(LayoutInflater.from(context), this, true)

    val titleTv = binding.titleTv
    val leftIb = binding.leftIb
    val rightIb = binding.rightIb
    val rightTv = binding.rightTv
    val rightExtraIb = binding.rightExtraIb
    val rightAnimator = binding.rightAnimator
    val avatarIv = binding.avatarIv
    val titleContainer = binding.titleContainer

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TitleView)
        if (ta.hasValue(R.styleable.TitleView_titleText)) {
            binding.titleTv.text = ta.getString(R.styleable.TitleView_titleText)
        }
        if (ta.hasValue(R.styleable.TitleView_rightIcon)) {
            binding.rightIb.setImageResource(ta.getResourceId(R.styleable.TitleView_rightIcon, 0))
        }
        if (ta.hasValue(R.styleable.TitleView_rightText)) {
            binding.rightTv.text = ta.getString(R.styleable.TitleView_rightText)
            binding.rightAnimator.displayedChild = POS_TEXT
        }
        if (ta.hasValue(R.styleable.TitleView_rightTextColor)) {
            binding.rightTv.setTextColor(
                ta.getColor(
                    R.styleable.TitleView_rightTextColor,
                    ContextCompat.getColor(context, R.color.text_gray),
                ),
            )
            binding.rightAnimator.displayedChild = POS_TEXT
        }
        if (ta.hasValue(R.styleable.TitleView_leftIcon)) {
            binding.leftIb.setImageResource(ta.getResourceId(R.styleable.TitleView_leftIcon, 0))
        }
        if (ta.hasValue(R.styleable.TitleView_titleColor)) {
            binding.titleTv.setTextColor(
                ta.getColor(
                    R.styleable.TitleView_titleColor,
                    ContextCompat.getColor(context, android.R.color.black),
                ),
            )
        }
        if (ta.hasValue(R.styleable.TitleView_android_background)) {
            setBackgroundResource(
                ta.getResourceId(
                    R.styleable.TitleView_android_background,
                    ContextCompat.getColor(context, android.R.color.white),
                ),
            )
        } else {
            setBackgroundResource(android.R.color.white)
        }
        if (ta.hasValue(R.styleable.TitleView_need_divider)) {
            binding.divider.visibility = if (ta.getBoolean(R.styleable.TitleView_need_divider, false)) VISIBLE else GONE
        }
        if (ta.hasValue(R.styleable.TitleView_rightIcon) || ta.hasValue(R.styleable.TitleView_rightText)) {
            binding.rightAnimator.visibility = View.VISIBLE
        } else {
            binding.rightAnimator.visibility = View.GONE
        }
        ta.recycle()
    }

    fun setSubTitle(
        first: String,
        second: String,
    ) {
        binding.titleTv.setTextOnly(first)
        if (second.isBlank()) {
            binding.subTitleTv.visibility = View.GONE
        } else {
            binding.subTitleTv.visibility = View.VISIBLE
            binding.subTitleTv.text = second
        }
    }


    fun setUser(user: User) {
        binding.titleTv.setName(user)
        binding.subTitleTv.visibility = View.VISIBLE
        binding.subTitleTv.text = user.identityNumber
    }

    fun initProgress(
        max: Int,
        progress: Int,
    ) {
        binding.pb.max = max
        binding.pb.indicatorSize = 24.dp
        binding.pb.progress = progress
        binding.rightAnimator.isVisible = true
        binding.rightAnimator.displayedChild = POS_PROGRESS
    }

    fun setProgress(index: Int) {
        binding.pb.setProgress(index, true)
    }

    companion object {
        const val POS_TEXT = 1
        const val POS_PROGRESS = 2
    }
}
