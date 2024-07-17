package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
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

    fun updateTokens( @StringRes strRes: Int,tokens: List<TokenItem>?) {
        if (tokens.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true
        when (tokens.size) {
            1 -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = false
                binding.icon3.isVisible = false
                binding.icon1.loadImage(tokens[0].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                setTitle(tokens[0].symbol)
            }
            else -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = tokens.size > 1
                binding.icon3.isVisible = tokens.size > 2

                if (tokens.isNotEmpty()) {
                    binding.icon1.loadImage(tokens[0].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                if (tokens.size > 1) {
                    binding.icon2.loadImage(tokens[1].iconUrl, holder = R.drawable.ic_avatar_place_holder)

                }
                if (tokens.size > 2) {
                    binding.icon3.loadImage(tokens[2].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                setTitle(context.getString(R.string.x_assets, tokens.size))
            }
        }
    }

    fun updateUsers(@StringRes strRes: Int, tokens: List<User>?) {
        if (tokens.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true
        when (tokens.size) {
            1 -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = false
                binding.icon3.isVisible = false
                binding.icon1.loadImage(tokens[0].avatarUrl, holder = R.drawable.ic_avatar_place_holder)
                setTitle(tokens[0].fullName)
            }
            else -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = tokens.size > 1
                binding.icon3.isVisible = tokens.size > 2

                if (tokens.isNotEmpty()) {
                    binding.icon1.loadImage(tokens[0].avatarUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                if (tokens.size > 1) {
                    binding.icon2.loadImage(tokens[1].avatarUrl, holder = R.drawable.ic_avatar_place_holder)

                }
                if (tokens.size > 2) {
                    binding.icon3.loadImage(tokens[2].avatarUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                setTitle(context.getString(R.string.x_recipients, tokens.size))
            }
        }
    }
}