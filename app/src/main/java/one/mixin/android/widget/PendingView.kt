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
import one.mixin.android.databinding.ViewPendingBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.PendingDisplay
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.formatAddress
import one.mixin.android.vo.safe.TokenItem

class PendingView @JvmOverloads constructor(
    context: Context,
    val attrs: AttributeSet? = null,
    defStyle: Int = 0,
) :
    LinearLayout
        (context, attrs, defStyle) {

    private val binding = ViewPendingBinding.inflate(LayoutInflater.from(context), this)

    init {
        setBackgroundResource(R.drawable.bg_round_window_btn)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
        }
    }

    fun updateTokens(pendingDisplays: List<PendingDisplay>) {
        if (pendingDisplays.isEmpty()) {
            return
        }
        binding.iconGroup.isVisible = true

        val icons = listOf(
            binding.icon1, binding.icon2
        )
        icons.forEach { it.isVisible = false }
        val count = pendingDisplays.size.coerceAtMost(2)
        for (i in 0 until count) {
            icons[i].isVisible = true
            icons[i].loadImage(
                pendingDisplays[i].iconUrl,
                holder = R.drawable.ic_avatar_place_holder
            )
        }
        if (pendingDisplays.size == 1) {
            binding.icon2.isVisible = false
            binding.icon3.isVisible = false
        } else if (pendingDisplays.size == 2) {
            binding.icon2.isVisible = true
            binding.icon3.isVisible = false
        } else {
            binding.icon2.isVisible = true
            binding.icon3.isVisible = true
            binding.icon3.text = "+${pendingDisplays.size - 2}"
        }

        binding.content.text = if (pendingDisplays.size == 1) binding.root.context.getString(
            R.string.recharge_confirmation,
            "${pendingDisplays[0].amount} ${pendingDisplays[0].symbol}"
        )
        else binding.root.context.getString(
            R.string.recharge_confirmation_count,
            pendingDisplays.size
        )
    }

    private fun loadIcon(avatarView: AvatarView, recipient: Recipient) {
        if (recipient is UserItem) {
            avatarView.setInfo(recipient.fullName, recipient.avatarUrl, recipient.id)
        } else if (recipient is AddressItem) {
            avatarView.loadUrl(recipient.iconUrl, R.drawable.ic_avatar_place_holder)
        }
    }
}
