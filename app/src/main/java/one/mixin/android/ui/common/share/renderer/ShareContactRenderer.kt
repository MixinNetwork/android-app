package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

open class ShareContactRenderer(val context: Context) : ShareMessageRenderer {

    private val binding = ItemChatContactCardBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root
    init {
        (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        binding.chatName.isVisible = false
    }

    fun render(user: User, isNightMode: Boolean) {
        binding.avatarIv.setInfo(
            user.fullName,
            user.avatarUrl,
            user.userId
        )
        binding.nameTv.text = user.fullName
        binding.idTv.text = user.identityNumber
        binding.dataWrapper.chatTime.timeAgoClock(nowInUtc())
        user.showVerifiedOrBot(binding.verifiedIv, binding.botIv)

        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
        }

        binding.chatContentLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.bill_bubble_me_last
            } else {
                R.drawable.bill_bubble_me_last_night
            }
        )
    }
}
