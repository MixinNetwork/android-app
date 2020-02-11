package one.mixin.android.ui.media

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_audio.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.round
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.Session
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem

class AudioAdapter(private val onClickListener: (messageItem: MessageItem) -> Unit) :
    SharedMediaHeaderAdapter<AudioHolder>() {
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
    @SuppressLint("SetTextI18n")
    fun bind(item: MessageItem, onClickListener: (messageItem: MessageItem) -> Unit) {
        val isMe = item.userId == Session.getAccountId()
        itemView.avatar.setInfo(item.userFullName, item.userAvatarUrl, item.userIdentityNumber)
        itemView.cover.round(itemView.context.dpToPx(25f))
        item.mediaWaveform?.let {
            itemView.audio_waveform.setWaveform(it)
        }
        item.mediaDuration?.let {
            itemView.audio_duration.text = try {
                it.toLong().formatMillis()
            } catch (e: Exception) {
                ""
            }
        }
        if (!isMe && item.mediaStatus != MediaStatus.READ.name) {
            itemView.audio_duration.setTextColor(itemView.context.getColor(R.color.colorBlue))
            itemView.audio_waveform.isFresh = true
        } else {
            itemView.audio_duration.setTextColor(itemView.context.getColor(R.color.gray_50))
            itemView.audio_waveform.isFresh = false
        }
        if (AudioPlayer.get().isLoaded(item.messageId)) {
            itemView.audio_waveform.setProgress(AudioPlayer.get().progress)
        } else {
            itemView.audio_waveform.setProgress(0f)
        }
        item.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.audio_expired.visibility = View.VISIBLE
                    itemView.audio_progress.visibility = View.INVISIBLE
                }
                MediaStatus.PENDING.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.enableLoading()
                    itemView.audio_progress.setBindOnly(item.messageId)
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.setBindOnly(item.messageId)
                    itemView.audio_waveform.setBind(item.messageId)
                    if (AudioPlayer.get().isPlay(item.messageId)) {
                        itemView.audio_progress.setPause()
                    } else {
                        itemView.audio_progress.setPlay()
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    if (isMe) {
                        itemView.audio_progress.enableUpload()
                    } else {
                        itemView.audio_progress.enableDownload()
                    }
                    itemView.audio_progress.setBindOnly(item.messageId)
                    itemView.audio_progress.setProgress(-1)
                }
            }
        }
        itemView.setOnClickListener {
            onClickListener(item)
        }
    }
}
