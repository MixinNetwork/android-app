package one.mixin.android.ui.conversation.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatGalleryBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.util.image.HeicLoader
import one.mixin.android.util.image.ImageListener
import one.mixin.android.widget.gallery.internal.entity.Item

class GalleryItemAdapter(
    private val needCamera: Boolean,
) : RecyclerView.Adapter<GalleryItemAdapter.ItemViewHolder>() {
    var items: List<Item>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var listener: GalleryCallback? = null
    var size: Int = 0

    var selectedPos: Int? = null
    var selectedUri: Uri? = null

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
    ) {
        val params = holder.itemView.layoutParams
        params.width = size
        params.height = size
        val ctx = holder.itemView.context
        holder.itemView.layoutParams = params
        val imageView = holder.binding.thumbnailIv
        val coverView = holder.binding.coverView
        if (position == 0 && needCamera) {
            holder.binding.thumbnailIv.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.binding.gifTv.isVisible = false
            holder.binding.videoIv.isVisible = false
            holder.binding.durationTv.isVisible = false
            coverView.isVisible = false
            holder.binding.sendTv.isVisible = false
            imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                width = ctx.dpToPx(42f)
                height = ctx.dpToPx(42f)
            }
            imageView.setImageResource(R.drawable.ic_gallery_camera)
            holder.binding.bg.setBackgroundResource(R.drawable.bg_gray_black_round_8dp)
            imageView.setOnClickListener { listener?.onCameraClick() }
        } else {
            imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
            imageView.round(ctx.dpToPx(8f))
            coverView.round(ctx.dpToPx(8f))
            val item = items!![if (needCamera) position - 1 else position]
            holder.binding.bg.setBackgroundResource(0)
            if (item.isGif) {
                holder.binding.gifTv.isVisible = true
                holder.binding.videoIv.isVisible = false
                holder.binding.durationTv.isVisible = false
                imageView.loadImage(item.uri, R.drawable.ic_giphy_place_holder)
            } else {
                holder.binding.gifTv.isVisible = false
                if (item.isVideo) {
                    holder.binding.videoIv.isVisible = true
                    holder.binding.durationTv.isVisible = true
                    holder.binding.durationTv.text = DateUtils.formatElapsedTime(item.duration / 1000)
                } else {
                    holder.binding.videoIv.isVisible = false
                    holder.binding.durationTv.isVisible = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && item.isHeif) {
                    holder.binding.thumbnailIv.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setImageDrawable(null)
                    HeicLoader.fromUrl(ctx, item.uri).addListener(
                        object : ImageListener<Drawable> {
                            override fun onResult(result: Drawable) {
                                imageView.setImageDrawable(result)
                            }
                        },
                    )
                } else {
                    imageView.loadImage(item.uri, R.drawable.image_holder)
                }
            }
            if (selectedUri == item.uri) {
                coverView.isVisible = true
                holder.binding.sendTv.isVisible = true
            } else {
                coverView.isVisible = false
                holder.binding.sendTv.isVisible = false
            }
            imageView.setOnClickListener {
                val send = selectedUri == item.uri
                selectedUri = null
                selectedPos = null
                listener?.onItemClick(position, item, send)
                notifyItemChanged(position)
            }
            imageView.setOnLongClickListener {
                if (selectedUri != item.uri) {
                    selectedPos?.let { notifyItemChanged(it) }
                    selectedUri = item.uri
                    selectedPos = position
                    notifyItemChanged(position)
                } else {
                    selectedUri = null
                    selectedPos = null
                    notifyItemChanged(position)
                }
                return@setOnLongClickListener true
            }
        }
    }

    override fun getItemCount(): Int =
        if (needCamera) {
            items?.size ?: +1
        } else {
            items?.size ?: 0
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        return ItemViewHolder(ItemChatGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @Synchronized
    fun hideBLur() {
        if (selectedPos == null) return

        val currentPos = selectedPos
        selectedUri = null
        selectedPos = null
        currentPos?.let { notifyItemChanged(it) }
    }

    class ItemViewHolder(val binding: ItemChatGalleryBinding) : RecyclerView.ViewHolder(binding.root)
}
