package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.view_avatar.view.*
import one.mixin.android.R
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.getColorCode
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.loadImage
import org.jetbrains.anko.sp

class AvatarView(context: Context, attrs: AttributeSet?) : ViewAnimator(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_avatar, this, true)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView)
        if (ta != null) {
            if (ta.hasValue(R.styleable.CircleImageView_border_text_size)) {
                avatar_tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.CircleImageView_border_text_size,
                    sp(20f).toFloat()))
            }
            if (ta.hasValue(R.styleable.CircleImageView_border_width)) {
                avatar_simple.borderWidth = ta.getDimensionPixelSize(R.styleable.CircleImageView_border_width, 0)
                avatar_simple.borderColor = ta.getColor(R.styleable.CircleImageView_border_color,
                    ContextCompat.getColor(context, android.R.color.white))
                avatar_tv.setBorderInfo(avatar_simple.borderWidth.toFloat(), avatar_simple.borderColor)
            }

            ta.recycle()
        }
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
            for (i in 0 until name.length) {
                val c = name[i]
                if (!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c) && !Character.isWhitespace(c)) {
                    builder.append(c)
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
        displayedChild = POS_AVATAR
        Glide.with(this)
            .load(url)
            .apply(RequestOptions().dontAnimate().placeholder(R.drawable.ic_group_place_holder))
            .into(avatar_simple)
    }

    fun setUrl(url: String?, placeHolder: Int) {
        displayedChild = POS_AVATAR
        avatar_simple.loadCircleImage(url, placeHolder)
    }

    fun setInfo(name: String?, url: String?, id: String) {
        avatar_tv.text = checkEmoji(name)
        try {
            avatar_tv.setBackgroundResource(getAvatarPlaceHolderById(id.getColorCode(CodeType.Avatar)))
        } catch (e: NumberFormatException) {
        }
        displayedChild = if (url != null && url.isNotEmpty()) {
            avatar_simple.loadImage(url, R.drawable.ic_avatar_place_holder)
            POS_AVATAR
        } else {
            POS_TEXT
        }
    }

    fun setTextSize(size: Float) {
        avatar_tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    private fun getAvatarPlaceHolderById(code: Int): Int {
        try {
           return avatarArray.getResourceId(code, -1)
        } catch (e: Exception) {
        }
        return R.drawable.default_avatar
    }


    private val avatarArray by lazy {
        resources.obtainTypedArray(R.array.avatar)
    }
}