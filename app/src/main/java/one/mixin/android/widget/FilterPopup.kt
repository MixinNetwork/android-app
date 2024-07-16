package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import coil.transform.CircleCropTransformation
import one.mixin.android.R
import one.mixin.android.databinding.ViewFilterPopupBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.dp
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem

class FilterPopup @JvmOverloads constructor(
    context: Context,
    val attrs: AttributeSet? = null,
    defStyle: Int = 0,
) :
    LinearLayout
        (context, attrs, defStyle) {

    private val binding = ViewFilterPopupBinding.inflate(LayoutInflater.from(context), this)

    init {
        setBackgroundResource(R.drawable.bg_inscription_radio)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setPadding(16.dp, 8.dp, 16.dp, 8.dp)
        }
    }

    fun setTitle(@StringRes resid: Int) {
        binding.title.setText(resid)
    }

    fun setTitle(text: CharSequence?) {
        binding.title.text = text
    }

    fun loadToken(token: TokenItem?) {
        if (token != null) {
            binding.startIcon.isVisible = true
            setTitle(token.symbol)
            binding.startIcon.loadImage(token.iconUrl, holder = R.drawable.ic_avatar_place_holder, transformation = CircleCropTransformation())
        } else {
            binding.startIcon.isVisible = false
            setTitle(R.string.All_Assets)
        }
    }

    fun loadUser(user: User?) {
        if (user != null) {
            binding.endIcon.isVisible = true
            setTitle(user.fullName)
            binding.endIcon.loadImage(user?.avatarUrl, holder = R.drawable.ic_avatar_place_holder, transformation = CircleCropTransformation())
        } else {
            binding.endIcon.isVisible = false
            setTitle(R.string.All_Recipients)
        }
    }
}