package one.mixin.android.ui.player

import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemFragmentMediaBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.util.AudioPlayer
import one.mixin.android.widget.CircleProgress

class MediaItemAdapter : ListAdapter<MediaItemData, MediaViewHolder>(MediaItemData.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFragmentMediaBinding.inflate(inflater, parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.titleView.text = mediaItem.title
        holder.subtitleView.text = mediaItem.subtitle
        when (mediaItem.downloadStatus) {
            MediaDescriptionCompat.STATUS_NOT_DOWNLOADED -> {
                holder.progress.isVisible = true
                holder.progress.enableDownload()
                holder.progress.setBindId(mediaItem.mediaId)
                holder.progress.setProgress(-1)
                holder.root.setOnClickListener {
                    listener?.onDownload(mediaItem)
                }
            }
            MediaDescriptionCompat.STATUS_DOWNLOADING -> {
                holder.progress.isVisible = true
                holder.progress.enableLoading(MixinJobManager.getAttachmentProcess(mediaItem.mediaId))
                holder.progress.setBindOnly(mediaItem.mediaId)
                holder.root.setOnClickListener {
                    listener?.onCancel(mediaItem)
                }
            }
            MediaDescriptionCompat.STATUS_DOWNLOADED -> {
                holder.progress.isVisible = true
                holder.progress.setBindOnly(mediaItem.mediaId)
                if (AudioPlayer.isPlay(mediaItem.mediaId)) {
                    holder.progress.setPause()
                } else {
                    holder.progress.setPlay()
                }
                holder.root.setOnClickListener {
                    listener?.onItemClick(mediaItem)
                }
            }
        }
        holder.albumArt.loadImage(mediaItem.albumArtUri?.path)
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
    fun onItemClick(mediaItem: MediaItemData)
    fun onDownload(mediaItem: MediaItemData)
    fun onCancel(mediaItem: MediaItemData)
}
