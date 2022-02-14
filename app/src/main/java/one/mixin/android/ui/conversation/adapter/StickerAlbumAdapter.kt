package one.mixin.android.ui.conversation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import one.mixin.android.R
import one.mixin.android.databinding.LayoutStickerTabBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.conversation.GiphyFragment
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.recyclerview.OffsetListUpdateCallback

class StickerAlbumAdapter(
    activity: FragmentActivity,
    private val albums: MutableList<StickerAlbum>
) : FragmentStateAdapter(activity) {
    companion object {
        const val TYPE_STORE = 0
        const val TYPE_RECENT = 1
        const val TYPE_LIKE = 2
        const val TYPE_GIPHY = 3
        const val TYPE_NORMAL = 4

        const val UN_NORMAL_COUNT = 4
    }

    var callback: StickerAlbumFragment.Callback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    override fun getItemCount() = albums.size + UN_NORMAL_COUNT

    override fun getItemId(position: Int): Long {
        return when (position) {
            TYPE_STORE -> 0L
            TYPE_RECENT -> 1L
            TYPE_LIKE -> 2L
            TYPE_GIPHY -> 3L
            else -> albums[position - UN_NORMAL_COUNT].albumId.hashCode().toLong()
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemId == 0L || itemId == 1L || itemId == 2L || itemId == 3L || albums.any { it.albumId.hashCode().toLong() == itemId }
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            TYPE_STORE -> return Fragment()
            TYPE_RECENT -> StickerFragment.newInstance(type = TYPE_RECENT)
            TYPE_LIKE -> StickerFragment.newInstance(type = TYPE_LIKE)
            TYPE_GIPHY -> GiphyFragment.newInstance()
            else -> StickerFragment.newInstance(albums[position - UN_NORMAL_COUNT].albumId, TYPE_NORMAL)
        }
        val rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                rvCallback?.onScroll(dis)
            }

            override fun onRelease(fling: Int) {
                rvCallback?.onRelease(fling)
            }
        }
        if (fragment is GiphyFragment) {
            fragment.callback = object : Callback {
                override fun onStickerClick(stickerId: String, albumId: String?) {
                }

                override fun onGiphyClick(image: Image, previewUrl: String) {
                    callback?.onGiphyClick(image, previewUrl)
                }
            }
            fragment.rvCallback = rvCallback
        } else {
            fragment as StickerFragment
            fragment.setCallback(
                object : Callback {
                    override fun onGiphyClick(image: Image, previewUrl: String) {
                    }

                    override fun onStickerClick(stickerId: String, albumId: String?) {
                        callback?.onStickerClick(stickerId, albumId)
                    }
                }
            )
            fragment.rvCallback = rvCallback
        }
        return fragment
    }

    fun getTabView(pos: Int, context: Context): View {
        val binding = LayoutStickerTabBinding.inflate(LayoutInflater.from(context))
        when (pos) {
            TYPE_STORE -> return binding.root
            TYPE_RECENT -> binding.icon.setImageResource(R.drawable.ic_sticker_common)
            TYPE_LIKE -> binding.icon.setImageResource(R.drawable.ic_sticker_favorite)
            TYPE_GIPHY -> binding.icon.setImageResource(R.drawable.ic_sticker_gif)
            else -> binding.icon.loadImage(albums[pos - UN_NORMAL_COUNT].iconUrl)
        }
        return binding.root
    }

    fun setItems(newItems: List<StickerAlbum>) {
        val callback = StickerAlbumDiffUtil(albums, newItems)
        val diff = DiffUtil.calculateDiff(callback)

        albums.clear()
        albums.addAll(newItems)

        diff.dispatchUpdatesTo(OffsetListUpdateCallback(this, UN_NORMAL_COUNT))
    }

    interface Callback {
        fun onStickerClick(stickerId: String, albumId: String?)
        fun onGiphyClick(image: Image, previewUrl: String)
    }
}

class StickerAlbumDiffUtil(
    private val oldList: List<StickerAlbum>,
    private val newList: List<StickerAlbum>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].albumId == newList[newItemPosition].albumId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
