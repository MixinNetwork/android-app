package one.mixin.android.ui.conversation.transcript.holder

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.MusicPlayer
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class FileHolder constructor(val binding: ItemChatFileBinding) : BaseViewHolder(binding.root) {
    init {
        binding.billTime.chatFlag.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        super.bind(messageItem)
        binding.billTime.chatSecret.isVisible = messageItem.isSignal()
        val isMe = false
        chatLayout(isMe, isLast)
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
        } else {
            binding.chatName.visibility = View.GONE
        }
        binding.billTime.chatTime.timeAgoClock(messageItem.createdAt)

        binding.fileNameTv.text = messageItem.mediaName

        binding.bottomLayout.fileSizeTv.clearBindIdAndSetText(messageItem.mediaSize?.fileSize())

        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), false) { statusIcon, secretIcon, representativeIcon ->
            binding.billTime.chatFlag.isVisible = statusIcon != null
            binding.billTime.chatFlag.setImageDrawable(statusIcon)
            binding.billTime.chatSecret.isVisible = secretIcon != null
            binding.billTime.chatRepresentative.isVisible = representativeIcon != null
        }
        binding.bottomLayout.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (MimeTypes.isAudio(messageItem.mediaMimeType) &&
                        MusicPlayer.isPlay(messageItem.messageId)
                    ) {
                        MusicPlayer.seekTo(seekBar.progress)
                    }
                }
            }
        )
        binding.fileExpired.visibility = View.GONE
        binding.fileProgress.visibility = View.VISIBLE
        if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
            binding.fileProgress.setBindOnly(messageItem.messageId)
            binding.bottomLayout.bindId = messageItem.messageId
            if (MusicPlayer.isPlay(messageItem.messageId)) {
                binding.fileProgress.setPause()
                binding.bottomLayout.showSeekBar()
            } else {
                binding.fileProgress.setPlay()
                binding.bottomLayout.showText()
            }
        } else {
            binding.fileProgress.setDone()
            binding.fileProgress.setBindId(null)
            binding.bottomLayout.bindId = null
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night
                )
            }
            (binding.chatLayout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (binding.chatLayout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
