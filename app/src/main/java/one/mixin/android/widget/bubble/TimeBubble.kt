package one.mixin.android.widget.bubble

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.BubbleTimeBinding
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus

class TimeBubble(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet) {
    private val binding = BubbleTimeBinding.inflate(LayoutInflater.from(context), this)
    val chatStatus = binding.chatStatus

    init {
        gravity = Gravity.CENTER
    }

    fun load(createdAt: String, isWhite: Boolean = false) {
        load(
            false,
            createdAt,
            null,
            isPin = false,
            isRepresentative = false,
            isSecret = false,
            isWhite = isWhite
        )
    }

    fun load(
        isMe: Boolean,
        createdAt: String,
        status: String?,
        isPin: Boolean,
        isRepresentative: Boolean,
        isSecret: Boolean,
        isWhite: Boolean = false
    ) {
        binding.chatTime.timeAgoClock(createdAt)
        binding.chatTime.setTextColor(
            context.getColor(
                if (isWhite) {
                    R.color.white
                } else {
                    R.color.color_chat_date
                }
            )
        )
        binding.chatPin.isVisible = isPin
        binding.chatPin.setImageDrawable(
            if (isWhite) {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_pin_white)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_pin)
            }
        )
        binding.chatSecret.isVisible = isSecret
        binding.chatSecret.setImageDrawable(
            if (isWhite) {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_secret_white)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_secret)
            }
        )
        binding.chatRepresentative.isVisible = isRepresentative
        binding.chatRepresentative.setImageDrawable(
            if (isWhite) {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_representative_white)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.ic_chat_representative)
            }
        )
        binding.chatStatus.isVisible = isMe && status != null
        if (isMe && status != null) {
            binding.chatStatus.setImageDrawable(
                when (status) {
                    MessageStatus.SENDING.name ->
                        if (isWhite) {
                            R.drawable.ic_status_sending_white
                        } else {
                            R.drawable.ic_status_sending
                        }
                    MessageStatus.SENT.name ->
                        if (isWhite) {
                            R.drawable.ic_status_sent_white
                        } else {
                            R.drawable.ic_status_sent
                        }
                    MessageStatus.DELIVERED.name ->
                        if (isWhite) {
                            R.drawable.ic_status_delivered_white
                        } else {
                            R.drawable.ic_status_delivered
                        }
                    MessageStatus.READ.name ->
                        R.drawable.ic_status_read
                    else ->
                        null
                }?.let {
                    AppCompatResources.getDrawable(context, it)
                }
            )
        }
    }
}
