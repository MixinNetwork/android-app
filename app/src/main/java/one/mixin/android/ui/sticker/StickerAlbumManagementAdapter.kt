package one.mixin.android.ui.sticker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemAlbumManagementBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.ItemTouchHelperViewHolder
import java.util.Collections

class StickerAlbumManagementAdapter : RecyclerView.Adapter<AlbumManagementHolder>(), ItemTouchHelperAdapter {
    var data: MutableList<StickerAlbum>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AlbumManagementHolder(ItemAlbumManagementBinding.inflate(LayoutInflater.from(parent.context), parent, false), albumListener)

    override fun onBindViewHolder(holder: AlbumManagementHolder, position: Int) {
        data?.get(position)?.let { album -> holder.bind(album) }
    }

    var albumListener: AlbumListener? = null

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        data?.let { list ->
            Collections.swap(list, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
        }
        return true
    }

    override fun onItemDismiss(position: Int) {
        notifyItemRemoved(position)
    }

    override fun getItemCount() = data?.size ?: 0
}

class AlbumManagementHolder(
    val itemBinding: ItemAlbumManagementBinding,
    private val albumListener: AlbumListener? = null
) : RecyclerView.ViewHolder(itemBinding.root),
    ItemTouchHelperViewHolder {
    @SuppressLint("ClickableViewAccessibility")
    fun bind(album: StickerAlbum,) {
        itemBinding.apply {
            nameTv.text = album.name
            descTv.text = album.description
            stickerIv.loadImage(album.iconUrl)
            deleteIv.setOnClickListener { albumListener?.onDelete(album) }
            sortIv.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    albumListener?.startDrag(this@AlbumManagementHolder)
                }
                false
            }
        }
    }

    override fun onItemSelected() {}

    override fun onItemClear() {
        albumListener?.endDrag()
    }
}

interface AlbumListener {
    fun onDelete(album: StickerAlbum)
    fun startDrag(viewHolder: RecyclerView.ViewHolder)
    fun endDrag()
}
