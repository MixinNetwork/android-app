package one.mixin.android.ui.sticker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemAlbumManagementBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import java.util.Collections

class StickerAlbumManagementAdapter :
    ListAdapter<StickerAlbum, AlbumManagementHolder>(StickerAlbum.DIFF_CALLBACK),
    ItemTouchHelperAdapter {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AlbumManagementHolder(ItemAlbumManagementBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: AlbumManagementHolder, position: Int) {
        getItem(position)?.let { album -> holder.bind(album, albumListener) }
    }

    var albumListener: AlbumListener? = null
    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == 0 || toPosition == 0) return false
        currentList.toMutableList().let { list ->
            Collections.swap(list, fromPosition - 1, toPosition - 1)
            notifyItemMoved(fromPosition, toPosition)
        }
        return true
    }

    override fun onItemDismiss(position: Int) {
        if (position == 0) return
        notifyItemRemoved(position)
    }
}

class AlbumManagementHolder(val itemBinding: ItemAlbumManagementBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("ClickableViewAccessibility")
    fun bind(album: StickerAlbum, albumListener: AlbumListener? = null) {
        itemBinding.apply {
            nameTv.text = album.name
            descTv.text = album.description
            stickerIv.loadImage(album.iconUrl)
            deleteIv.setOnClickListener { albumListener?.onDelete(album) }
            sortIv.setOnTouchListener { _, event ->
                if (event.action and (MotionEvent.ACTION_DOWN) == 0) {
                    albumListener?.startDrag(this@AlbumManagementHolder)
                }
                false
            }
        }
    }
}

interface AlbumListener {
    fun onDelete(album: StickerAlbum)
    fun startDrag(viewHolder: RecyclerView.ViewHolder)
}
