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
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.ui.wallet.Web3TokenFilterType
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
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

    fun updateWeb3Tokens(@StringRes strRes: Int, tokens: List<Web3TokenItem>?) {
        if (tokens.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true

        val icons = listOf(
            binding.icon1, binding.icon2, binding.icon3, binding.icon4, binding.icon5,
            binding.icon6, binding.icon7, binding.icon8, binding.icon9, binding.icon10
        )
        icons.forEach { it.isVisible = false }

        val count = tokens.size.coerceAtMost(10)

        for (i in 0 until count) {
            icons[i].isVisible = true
            icons[i].loadUrl(tokens[i].iconUrl, holder = R.drawable.ic_avatar_place_holder)
        }

        if (count == 1) {
            val item = tokens[0]
            setTitle(item.symbol)
        } else {
            setTitle(context.getString(R.string.number_of_assets, tokens.size))
        }
    }

    fun updateTokens(@StringRes strRes: Int, tokens: List<TokenItem>?) {
        if (tokens.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true

        // Hide all icons initially
        val icons = listOf(
            binding.icon1, binding.icon2, binding.icon3, binding.icon4, binding.icon5,
            binding.icon6, binding.icon7, binding.icon8, binding.icon9, binding.icon10
        )
        icons.forEach { it.isVisible = false }

        val count = tokens.size.coerceAtMost(10)

        for (i in 0 until count) {
            icons[i].isVisible = true
            icons[i].loadUrl(tokens[i].iconUrl, holder = R.drawable.ic_avatar_place_holder)
        }

        if (count == 1) {
            val item = tokens[0]
            setTitle(item.symbol)
        } else {
            setTitle(context.getString(R.string.number_of_assets, tokens.size))
        }
    }

    fun updateUsers(@StringRes strRes: Int, recipients: List<Recipient>?) {
        if (recipients.isNullOrEmpty()) {
            binding.iconGroup.isVisible = false
            setTitle(strRes)
            return
        }

        binding.iconGroup.isVisible = true

        // Hide all icons initially
        val icons = listOf(
            binding.icon1, binding.icon2, binding.icon3, binding.icon4, binding.icon5,
            binding.icon6, binding.icon7, binding.icon8, binding.icon9, binding.icon10
        )
        icons.forEach { it.isVisible = false }

        val count = recipients.size.coerceAtMost(10)

        for (i in 0 until count) {
            icons[i].isVisible = true
            loadIcon(icons[i], recipients[i])
        }

        if (count == 1) {
            val item = recipients[0]
            if (item is UserItem) {
                setTitle(item.fullName)
            } else if (item is AddressItem) {
                setTitle(item.formatAddress())
            }
        } else {
            setTitle(context.getString(R.string.number_of_opponents, recipients.size))
        }
    }

    private fun loadIcon(avatarView: AvatarView, recipient: Recipient){
        if (recipient is UserItem){
            avatarView.setInfo(recipient.fullName, recipient.avatarUrl, recipient.id)
        } else if (recipient is AddressItem){
            avatarView.loadUrl(recipient.iconUrl, R.drawable.ic_avatar_place_holder)
        }
    }

    fun updateWeb3TokenFilterType(filterType: Web3TokenFilterType) {
        binding.iconGroup.isVisible = false
        setTitle(context.getString(filterType.titleRes))
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