package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import one.mixin.android.R
import one.mixin.android.databinding.ViewAvatarBinding
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.clear
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getColorCode
import one.mixin.android.extension.isActivityNotDestroyed
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.sp
import one.mixin.android.ui.home.bot.Bot
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface
import one.mixin.android.vo.ExploreApp

class AvatarView : ViewAnimator {
    private val binding = ViewAvatarBinding.inflate(LayoutInflater.from(context), this)
    val avatarSimple get() = binding.avatarSimple

    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView)
        if (ta.hasValue(R.styleable.CircleImageView_border_text_size)) {
            binding.avatarTv.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ta.getDimension(
                    R.styleable.CircleImageView_border_text_size,
                    20f.sp.toFloat(),
                ),
            )
        }
        if (ta.hasValue(R.styleable.CircleImageView_border_width)) {
            avatarSimple.borderWidth = ta.getDimensionPixelSize(R.styleable.CircleImageView_border_width, 0)
            avatarSimple.borderColor =
                ta.getColor(
                    R.styleable.CircleImageView_border_color,
                    ContextCompat.getColor(context, android.R.color.white),
                )
            binding.avatarTv.setBorderInfo(avatarSimple.borderWidth.toFloat(), avatarSimple.borderColor)
        }

        ta.recycle()
    }

    companion object {
        const val POS_TEXT = 0
        const val POS_AVATAR = 1

        fun checkEmoji(fullName: String?): String {
            if (fullName.isNullOrEmpty()) return ""
            val name: String = fullName
            if (name.length == 1) return name

            val builder = StringBuilder()
            var step = 0
            for (element in name) {
                if (!Character.isLetterOrDigit(element) && !Character.isSpaceChar(element) && !Character.isWhitespace(element)) {
                    builder.append(element)
                    step++
                    if (step > 1) {
                        break
                    }
                } else {
                    break
                }
            }
            return if (builder.isEmpty()) name[0].toString() else builder.toString()
        }
    }

    fun setGroup(url: String?) {
        if (!isActivityNotDestroyed()) return
        displayedChild = POS_AVATAR
        Glide.with(this)
            .load(url)
            .apply(RequestOptions().dontAnimate().placeholder(R.drawable.ic_group_place_holder))
            .into(avatarSimple)
    }

    fun setNet(padding: Int = context.dpToPx(8f)) {
        displayedChild = POS_AVATAR
        avatarSimple.setBackgroundResource(R.drawable.bg_circle_70_solid_gray)
        avatarSimple.setImageResource(R.drawable.ic_transfer_address)
        avatarSimple.setPadding(padding)
    }

    fun setDeposit() {
        displayedChild = POS_AVATAR
        avatarSimple.setImageResource(R.drawable.ic_snapshot_deposit)
    }

    fun setWithdrawal() {
        displayedChild = POS_AVATAR
        avatarSimple.setImageResource(R.drawable.ic_snapshot_withdrawal)
    }

    fun setAnonymous() {
        displayedChild = POS_AVATAR
        avatarSimple.setImageResource(R.drawable.ic_snapshot_anonymous)
    }

    fun setInfo(
        name: String?,
        url: String?,
        id: String,
    ) {
        binding.avatarTv.text = checkEmoji(name)
        try {
            binding.avatarTv.setBackgroundColor(getAvatarPlaceHolderById(id.getColorCode(CodeType.Avatar(avatarArray.size))))
        } catch (e: NumberFormatException) {
        }
        displayedChild =
            if (url != null && url.isNotEmpty()) {
                avatarSimple.setBackgroundResource(0)
                avatarSimple.setImageResource(0)
                avatarSimple.setPadding(0)
                avatarSimple.loadImage(url, R.drawable.ic_avatar_place_holder)
                POS_AVATAR
            } else {
                POS_TEXT
            }
    }

    fun loadUrl(url: String) {
        displayedChild = POS_AVATAR
        avatarSimple.setBackgroundResource(0)
        avatarSimple.setImageResource(0)
        avatarSimple.setPadding(0)
        avatarSimple.loadImage(url, R.drawable.ic_avatar_place_holder)
    }

    fun renderApp(app: BotInterface) {
        if (app is ExploreApp) {
            setInfo(app.name, app.iconUrl, app.appId)
        } else if (app is App) {
            setInfo(app.name, app.iconUrl, app.appId)
        } else if (app is Bot) {
            displayedChild = POS_AVATAR
            avatarSimple.setBackgroundResource(0)
            avatarSimple.setPadding(0)
            avatarSimple.clear()
            avatarSimple.setImageResource(app.icon)
        }
    }

    fun setBorderWidth(borderWidth: Int) {
        avatarSimple.borderWidth = borderWidth
    }

    fun setBorderColor(borderColor: Int) {
        avatarSimple.borderColor = borderColor
    }

    fun setTextSize(size: Float) {
        binding.avatarTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    private fun getAvatarPlaceHolderById(code: Int): Int {
        try {
            return avatarArray[code]
        } catch (e: Exception) {
        }
        return R.drawable.default_avatar
    }

    private val avatarArray by lazy {
        resources.getIntArray(R.array.avatar_colors)
    }
}
