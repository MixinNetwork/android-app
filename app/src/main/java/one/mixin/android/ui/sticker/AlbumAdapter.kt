package one.mixin.android.ui.sticker

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Embedded
import androidx.room.Relation
import one.mixin.android.R
import one.mixin.android.databinding.ItemAlbumBinding
import one.mixin.android.databinding.ItemStickerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.textColor
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.widget.RLottieImageView
import one.mixin.android.widget.SpacesItemDecoration

class AlbumAdapter(
    private val fragmentManager: FragmentManager,
    private val addAction: (String) -> Unit,
) : ListAdapter<StoreAlbum, AlbumHolder>(StoreAlbum.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AlbumHolder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false), fragmentManager, addAction)

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        getItem(position)?.let { album -> holder.bind(album) }
    }
}

class AlbumHolder(
    val binding: ItemAlbumBinding,
    private val fragmentManager: FragmentManager,
    private val addAction: (String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val padding: Int = 4.dp

    fun bind(album: StoreAlbum) {
        val ctx = binding.root.context
        binding.apply {
            tileTv.text = album.album.name
            actionTv.updateAlbumAdd(ctx, album.album.added) {
                addAction.invoke(album.album.albumId)
            }
            val adapter = StickerAdapter()
            stickerRv.apply {
                setHasFixedSize(true)
                if (itemDecorationCount == 0) {
                    addItemDecoration(SpacesItemDecoration(padding))
                }
                layoutManager = LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false)
                this.adapter = adapter
                adapter.stickerListener = object : StickerListener {
                    override fun onItemClick(sticker: Sticker) {
                        val stickerAlbumId = sticker.albumId ?: return
                        StickerAlbumBottomSheetFragment.newInstance(stickerAlbumId)
                            .showNow(fragmentManager, StickerAlbumBottomSheetFragment.TAG)
                    }
                }
                adapter.submitList(album.stickers)
            }
        }
    }
}

class StickerAdapter : ListAdapter<Sticker, StickerViewHolder>(Sticker.DIFF_CALLBACK) {
    var size: Int = 72.dp
    var stickerListener: StickerListener? = null

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val params = holder.itemView.layoutParams
        params.width = size
        params.height = size
        holder.itemView.layoutParams = params
        val item = (holder.itemView as ViewGroup).getChildAt(0) as RLottieImageView
        item.updateLayoutParams<ViewGroup.LayoutParams> {
            width = size
            height = size
        }
        getItem(position)?.let { s ->
            item.loadSticker(s.assetUrl, s.assetType, "${s.assetUrl}${s.stickerId}")
            item.setOnClickListener {
                stickerListener?.onItemClick(s)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StickerViewHolder(ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
}

data class StoreAlbum(
    @Embedded val album: StickerAlbum,
    @Relation(parentColumn = "album_id", entityColumn = "album_id")
    val stickers: List<Sticker>,
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StoreAlbum>() {
            override fun areItemsTheSame(oldItem: StoreAlbum, newItem: StoreAlbum) =
                oldItem.album.albumId == newItem.album.albumId

            override fun areContentsTheSame(oldItem: StoreAlbum, newItem: StoreAlbum) =
                oldItem.album == newItem.album && oldItem.stickers.size == newItem.stickers.size
        }
    }
}

class StickerViewHolder(val binding: ItemStickerBinding) : RecyclerView.ViewHolder(binding.root)

interface StickerListener {
    fun onItemClick(sticker: Sticker)
}

fun TextView.updateAlbumAdd(ctx: Context, added: Boolean, action: (() -> Unit)? = null) {
    if (added) {
        text = ctx.getString(R.string.added)
        textColor = ctx.getColor(R.color.colorAccent)
        setBackgroundResource(R.drawable.bg_round_gray_btn)
        isEnabled = false
        setOnClickListener(null)
    } else {
        text = ctx.getString(R.string.action_add)
        textColor = ctx.getColor(R.color.white)
        setBackgroundResource(R.drawable.bg_round_blue_btn)
        isEnabled = true
        setOnClickListener { action?.invoke() }
    }
}
