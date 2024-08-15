package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

open class ShareContactRenderer(val context: Context) {
    private val binding = ItemChatContactCardBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    init {
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        binding.chatName.isVisible = false
    }

    fun render(
        user: User,
        isNightMode: Boolean,
    ) {
        binding.avatarIv.setInfo(
            user.fullName,
            user.avatarUrl,
            user.userId,
        )
        binding.nameTv.setName(user)
        binding.idTv.text = user.identityNumber

        binding.chatTime.load(
            true,
            nowInUtc(),
            MessageStatus.DELIVERED.name,
            isPin = false,
            isRepresentative = false,
            isSecret = true,
            isWhite = true,
        )

        binding.chatContentLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.bill_bubble_me_last
            } else {
                R.drawable.bill_bubble_me_last_night
            },
        )
    }
}
