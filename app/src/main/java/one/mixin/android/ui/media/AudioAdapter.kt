package one.mixin.android.ui.media

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_audio.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.round
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem

class AudioAdapter(private val onClickListener: (messageItem: MessageItem) -> Unit) :
    SharedMediaHeaderAdapter<AudioHolder>() {
    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
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
}

class AudioHolder(itemView: View) : NormalHolder(itemView) {
    @SuppressLint("SetTextI18n")
    fun bind(item: MessageItem, onClickListener: (messageItem: MessageItem) -> Unit) {
        itemView.avatar.setInfo(item.userFullName, item.userAvatarUrl, item.userIdentityNumber)
        itemView.cover.round(itemView.context.dpToPx(25f))
        item.mediaWaveform?.let {
            itemView.audio_waveform.setWaveform(it)
        }
        item.mediaDuration?.let {
            itemView.audio_duration.text = "${it.toLong() / 1000}'"
        }
        itemView.audio_waveform.isFresh = item.mediaStatus != MediaStatus.READ.name
        if (AudioPlayer.get().isLoaded(item.messageId)) {
            itemView.audio_waveform.setProgress(AudioPlayer.get().progress)
        } else {
            itemView.audio_waveform.setProgress(0f)
        }
        itemView.audio_progress.visibility = View.VISIBLE
        itemView.audio_progress.setBindOnly(item.messageId)
        itemView.audio_waveform.setBind(item.messageId)
        if (AudioPlayer.get().isPlay(item.messageId)) {
            itemView.audio_progress.setPause()
        } else {
            itemView.audio_progress.setPlay()
        }
        itemView.audio_progress.setOnClickListener {
            onClickListener(item)
        }
    }
}
