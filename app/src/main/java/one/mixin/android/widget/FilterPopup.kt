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
import one.mixin.android.extension.colorAttr
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
                binding.icon1.loadUrl(tokens[0].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                setTitle(tokens[0].symbol)
            }
            else -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = tokens.size > 1
                binding.icon3.isVisible = tokens.size > 2

                if (tokens.isNotEmpty()) {
                    binding.icon1.loadUrl(tokens[0].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                if (tokens.size > 1) {
                    binding.icon2.loadUrl(tokens[1].iconUrl, holder = R.drawable.ic_avatar_place_holder)

                }
                if (tokens.size > 2) {
                    binding.icon3.loadUrl(tokens[2].iconUrl, holder = R.drawable.ic_avatar_place_holder)
                }
                setTitle(context.getString(R.string.x_assets, tokens.size))
            }
        }
    }

    fun updateUsers(@StringRes strRes: Int, users: List<User>?) {
        if (users.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true
        when (users.size) {
            1 -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = false
                binding.icon3.isVisible = false
                binding.icon1.setInfo(users[0].fullName, users[0].avatarUrl, users[0].identityNumber)
                setTitle(users[0].fullName)
            }

            else -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = users.size > 1
                binding.icon3.isVisible = users.size > 2

                if (users.isNotEmpty()) {
                    binding.icon1.setInfo(users[0].fullName, users[1].avatarUrl, users[0].identityNumber)
                }
                if (users.size > 1) {
                    binding.icon2.setInfo(users[1].fullName, users[1].avatarUrl, users[1].identityNumber)
                }
                if (users.size > 2) {
                    binding.icon3.setInfo(users[2].fullName, users[2].avatarUrl, users[2].identityNumber)
                }
                setTitle(context.getString(R.string.x_recipients, users.size))
            }
        }
    }

    fun open() {
        setBackgroundResource(R.drawable.bg_inscription_drop)
        binding.title.setTextColor(0xFF4B7CDD.toInt())
        binding.arrow.animate().rotation(-180f).setDuration(200).start()
    }

    fun close() {
        setBackgroundResource(R.drawable.bg_inscription_radio)
        binding.title.setTextColor(context.colorAttr(R.attr.text_primary))
        binding.arrow.animate().rotation(0f).setDuration(200).start()
    }
}