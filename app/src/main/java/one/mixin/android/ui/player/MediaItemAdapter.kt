package one.mixin.android.ui.player

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemFragmentMediaBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.ui.player.internal.albumArtUri
import one.mixin.android.ui.player.internal.diffCallback
import one.mixin.android.ui.player.internal.displaySubtitle
import one.mixin.android.ui.player.internal.displayTitle
import one.mixin.android.ui.player.internal.downloadStatus
import one.mixin.android.ui.player.internal.id
import one.mixin.android.util.MusicPlayer
import one.mixin.android.widget.CircleProgress

class MediaItemAdapter : SafePagedListAdapter<MediaMetadataCompat, MediaViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFragmentMediaBinding.inflate(inflater, parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = getItem(position) ?: return

        holder.titleView.text = mediaItem.displayTitle
        holder.subtitleView.text = mediaItem.displaySubtitle
        val mediaId = requireNotNull(mediaItem.id)
        when (mediaItem.downloadStatus) {
            MediaDescriptionCompat.STATUS_NOT_DOWNLOADED -> {
                holder.progress.isVisible = true
                holder.progress.enableDownload()
                holder.progress.setBindId(mediaId)
                holder.progress.setProgress(-1)
                holder.root.setOnClickListener {
                    listener?.onDownload(mediaItem)
                }
            }
            MediaDescriptionCompat.STATUS_DOWNLOADING -> {
                holder.progress.isVisible = true
                holder.progress.enableLoading(MixinJobManager.getAttachmentProcess(mediaId))
                holder.progress.setBindOnly(mediaId)
                holder.root.setOnClickListener {
                    listener?.onCancel(mediaItem)
                }
            }
            MediaDescriptionCompat.STATUS_DOWNLOADED -> {
                holder.progress.isVisible = true
                holder.progress.setBindOnly(mediaId)
                if (MusicPlayer.isPlay(mediaId)) {
                    holder.progress.setPause()
                } else {
                    holder.progress.setPlay()
                }
                holder.root.setOnClickListener {
                    listener?.onItemClick(mediaItem)
                }
            }
        }
        holder.albumArt.loadImage(mediaItem.albumArtUri.path, R.drawable.ic_music_place_holder)
    }

    var listener: MediaItemListener? = null
}

class MediaViewHolder(
    binding: ItemFragmentMediaBinding,
) : RecyclerView.ViewHolder(binding.root) {

    val titleView: TextView = binding.title
    val subtitleView: TextView = binding.subtitle
    val albumArt: ImageView = binding.albumArt
    val progress: CircleProgress = binding.progress
    val root: View = binding.root
}

interface MediaItemListener {
    fun onItemClick(mediaItem: MediaMetadataCompat)
    fun onDownload(mediaItem: MediaMetadataCompat)
    fun onCancel(mediaItem: MediaMetadataCompat)
}
