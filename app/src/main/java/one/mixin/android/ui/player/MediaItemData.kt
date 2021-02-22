package one.mixin.android.ui.player

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class MediaItemData(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val albumArtUri: Uri?,
    val browsable: Boolean,
    val downloadStatus: Long,
) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemData>() {
            override fun areItemsTheSame(
                oldItem: MediaItemData,
                newItem: MediaItemData
            ): Boolean =
                oldItem.mediaId == newItem.mediaId

            override fun areContentsTheSame(oldItem: MediaItemData, newItem: MediaItemData): Boolean =
                oldItem == newItem
        }
    }

    fun createNew(newStatus: Long) = MediaItemData(mediaId, title, subtitle, albumArtUri, browsable, newStatus)
}

data class MessageIdIdAndMediaStatus(
    val mediaId: String,
    val mediaStatus: String,
)
