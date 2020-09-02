package one.mixin.android.ui.conversation.adapter

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.layout_sticker_tab.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.conversation.GiphyFragment
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.DraggableRecyclerView

class StickerAlbumAdapter(activity: FragmentActivity, private val albums: List<StickerAlbum>) : FragmentStateAdapter(activity) {
    companion object {
        const val TYPE_RECENT = 0
        const val TYPE_LIKE = 1
        const val TYPE_GIPHY = 2
        const val TYPE_NORMAL = 3

        const val UN_NORMAL_COUNT = 3
    }

    var callback: StickerAlbumFragment.Callback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    override fun getItemCount() = albums.size + UN_NORMAL_COUNT

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
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
                override fun onStickerClick(stickerId: String) {
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

                    override fun onStickerClick(stickerId: String) {
                        callback?.onStickerClick(stickerId)
                    }
                }
            )
            fragment.rvCallback = rvCallback
        }
        return fragment
    }

    fun getTabView(pos: Int, context: Context): View {
        val view = View.inflate(context, R.layout.layout_sticker_tab, null)
        when (pos) {
            TYPE_RECENT -> view.icon.setImageResource(R.drawable.ic_sticker_common)
            TYPE_LIKE -> view.icon.setImageResource(R.drawable.ic_sticker_favorite)
            TYPE_GIPHY -> view.icon.setImageResource(R.drawable.ic_sticker_gif)
            else -> view.icon.loadImage(albums[pos - UN_NORMAL_COUNT].iconUrl)
        }
        return view
    }

    interface Callback {
        fun onStickerClick(stickerId: String)
        fun onGiphyClick(image: Image, previewUrl: String)
    }
}
