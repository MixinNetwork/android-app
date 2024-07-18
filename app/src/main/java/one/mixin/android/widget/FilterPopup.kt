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
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.User
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.displayAddress
import one.mixin.android.vo.formatAddress
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

    fun updateUsers(@StringRes strRes: Int, recipients: List<Recipient>?) {
        if (recipients.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true
        when (recipients.size) {
            1 -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = false
                binding.icon3.isVisible = false
                val item = recipients[0]
                loadIcon(binding.icon1, item)
                if (item is UserItem) {
                    setTitle(item.fullName)
                } else if (item is AddressItem) {
                    setTitle(item.formatAddress())
                }
            }

            else -> {
                binding.icon1.isVisible = true
                binding.icon2.isVisible = recipients.size > 1
                binding.icon3.isVisible = recipients.size > 2

                if (recipients.isNotEmpty()) {
                    loadIcon(binding.icon1, recipients[0])
                }
                if (recipients.size > 1) {
                    loadIcon(binding.icon2, recipients[1])
                }
                if (recipients.size > 2) {
                    loadIcon(binding.icon3, recipients[2])
                }
                setTitle(context.getString(R.string.x_recipients, recipients.size))
            }
        }
    }

    private fun loadIcon(avatarView: AvatarView, recipient: Recipient){
        if (recipient is UserItem){
            avatarView.setInfo(recipient.fullName, recipient.avatarUrl, recipient.id)
        } else if (recipient is AddressItem){
            avatarView.loadUrl(recipient.iconUrl, R.drawable.ic_avatar_place_holder)
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