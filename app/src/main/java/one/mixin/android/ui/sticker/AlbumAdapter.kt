package one.mixin.android.ui.sticker

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemAlbumBinding
import one.mixin.android.databinding.ItemStickerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.textColor
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.widget.RLottieImageView
import one.mixin.android.widget.SpacesItemDecoration

class AlbumAdapter(
    private val fragmentManager: FragmentManager,
    private val viewModel: ConversationViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val addAction: (String) -> Unit,
) : ListAdapter<StickerAlbum, AlbumHolder>(StickerAlbum.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AlbumHolder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false), fragmentManager, viewModel, lifecycleOwner, addAction)

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        getItem(position)?.let { album -> holder.bind(album) }
    }
}

class AlbumHolder(
    val binding: ItemAlbumBinding,
    private val fragmentManager: FragmentManager,
    private val viewModel: ConversationViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val addAction: (String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val padding: Int = 4.dp

    fun bind(album: StickerAlbum) {
        val ctx = binding.root.context
        binding.apply {
            tileTv.text = album.name
            actionTv.updateAlbumAdd(ctx, album.added) {
                addAction.invoke(album.albumId)
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
                viewModel.observeSystemStickersByAlbumId(album.albumId).observe(lifecycleOwner) { stickers ->
                    adapter.submitList(stickers)
                }
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

class StickerViewHolder(val binding: ItemStickerBinding) : RecyclerView.ViewHolder(binding.root)

interface StickerListener {
    fun onItemClick(sticker: Sticker)
}

fun TextView.updateAlbumAdd(ctx: Context, added: Boolean, action: (() -> Unit)? = null) {
    if (added) {
        text = ctx.getString(R.string.sticker_store_added)
        textColor = ctx.getColor(R.color.colorAccent)
        setBackgroundResource(R.drawable.bg_round_gray_btn)
        isEnabled = false
        setOnClickListener(null)
    } else {
        text = ctx.getString(R.string.sticker_store_add)
        textColor = ctx.getColor(R.color.white)
        setBackgroundResource(R.drawable.bg_round_blue_btn)
        isEnabled = true
        setOnClickListener { action?.invoke() }
    }
}
