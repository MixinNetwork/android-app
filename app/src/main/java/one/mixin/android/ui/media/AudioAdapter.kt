package one.mixin.android.ui.media

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import one.mixin.android.R
import one.mixin.android.databinding.ItemAudioBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.round
import one.mixin.android.session.Session
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem

class AudioAdapter(private val onClickListener: (messageItem: MessageItem) -> Unit) :
    SharedMediaHeaderAdapter<AudioHolder>(
        object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(
                oldItem: MessageItem,
                newItem: MessageItem
            ): Boolean {
                return oldItem.mediaStatus == newItem.mediaStatus &&
                    oldItem.status == newItem.status
            }
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AudioHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_audio,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, onClickListener)
        }
    }

    override fun getHeaderTextMargin() = 20f
}

class AudioHolder(itemView: View) : NormalHolder(itemView) {
    private val binding = ItemAudioBinding.bind(itemView)
    @SuppressLint("SetTextI18n")
    fun bind(item: MessageItem, onClickListener: (messageItem: MessageItem) -> Unit) {
        val isMe = item.userId == Session.getAccountId()
        binding.avatar.setInfo(item.userFullName, item.userAvatarUrl, item.userIdentityNumber)
        binding.cover.round(itemView.context.dpToPx(25f))
        item.mediaWaveform?.let {
            binding.audioWaveform.setWaveform(it)
        }
        item.mediaDuration?.let {
            binding.audioDuration.text = it.toLongOrNull()?.formatMillis() ?: ""
        }
        if (!isMe && item.mediaStatus != MediaStatus.READ.name) {
            binding.audioDuration.setTextColor(itemView.context.getColor(R.color.colorBlue))
            binding.audioWaveform.isFresh = true
        } else {
            binding.audioDuration.setTextColor(itemView.context.getColor(R.color.gray_50))
            binding.audioWaveform.isFresh = false
        }
        if (AudioPlayer.isLoaded(item.messageId)) {
            binding.audioWaveform.setProgress(AudioPlayer.getProgress())
        } else {
            binding.audioWaveform.setProgress(0f)
        }
        item.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.audioExpired.visibility = View.VISIBLE
                    binding.audioProgress.visibility = View.INVISIBLE
                }
                MediaStatus.PENDING.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.enableLoading()
                    binding.audioProgress.setBindOnly(item.messageId)
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.setBindOnly(item.messageId)
                    binding.audioWaveform.setBind(item.messageId)
                    if (AudioPlayer.isPlay(item.messageId)) {
                        binding.audioProgress.setPause()
                    } else {
                        binding.audioProgress.setPlay()
                    }
                }
                MediaStatus.CANCELED.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    if (isMe) {
                        binding.audioProgress.enableUpload()
                    } else {
                        binding.audioProgress.enableDownload()
                    }
                    binding.audioProgress.setBindOnly(item.messageId)
                    binding.audioProgress.setProgress(-1)
                }
            }
        }
        itemView.setOnClickListener {
            onClickListener(item)
        }
    }
}
