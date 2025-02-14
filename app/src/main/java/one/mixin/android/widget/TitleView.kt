package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.RelativeSizeSpan
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
import one.mixin.android.widget.linktext.RoundBackgroundColorSpan
import timber.log.Timber

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
            binding.titleTv.setTextOnly(ta.getString(R.styleable.TitleView_titleText))
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
            binding.subTitleTv.setTextOnly(second)
        }
    }

    fun setSubTitle(
        first: String,
        second: User?,
    ) {
        binding.titleTv.setTextOnly(first)
        if (second == null) {
            binding.subTitleTv.visibility = GONE
            binding.subTitleAvatar.visibility = GONE
        } else {
            binding.subTitleTv.visibility = VISIBLE
            binding.subTitleAvatar.visibility = VISIBLE
            binding.subTitleTv.setName(second)
            binding.subTitleAvatar.setInfo(second.fullName, second.avatarUrl, second.userId)
        }
    }

    fun setSubTitle(
        title: String,
        users: List<User>,
        callback: () -> Unit,
    ) {
        binding.titleTv.setTextOnly(title)
        if (users.isEmpty()) {
            binding.subTitleTv.visibility = GONE
            binding.subTitleAvatar.visibility = GONE
        } else if (users.size == 1) {
            val user = users.first()
            binding.subTitleTv.visibility = VISIBLE
            binding.subTitleAvatar.visibility = VISIBLE
            binding.subTitleTv.setName(user)
            binding.subTitleAvatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        } else {
            binding.subTitleTv.visibility = GONE
            binding.subTitleAvatar.visibility = GONE
            binding.receiversView.visibility = VISIBLE
            binding.receiversView.addList(users)
            binding.receiversView.setOnClickListener {
                callback()
            }
        }
    }

    fun setLabel(
        title: String,
        label: String?,
        content: String,
        toWallet: Boolean = false,
    ) {
        binding.titleTv.setTextOnly(title)
        binding.subTitleTv.visibility = VISIBLE
        if (label != null) {
            val spannableString = SpannableString("$label ")
            val backgroundColor: Int = if (toWallet) Color.parseColor("#7EABFB") else Color.parseColor("#8DCC99")
            val backgroundColorSpan = RoundBackgroundColorSpan(backgroundColor, Color.WHITE)
            val endIndex = label.length
            if (endIndex > 0) {
                spannableString.setSpan(RelativeSizeSpan(0.8f), 0, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(backgroundColorSpan, 0, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            binding.subTitleTv.setTextOnly(spannableString)
        } else {
            binding.subTitleTv.setTextOnly(content)
        }
    }

    fun setUser(user: User) {
        binding.titleTv.setName(user)
        binding.subTitleTv.visibility = View.VISIBLE
        binding.subTitleTv.setTextOnly(user.identityNumber)
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
