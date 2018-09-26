package one.mixin.android.ui.conversation.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import android.view.View
import kotlinx.android.synthetic.main.layout_sticker_tab.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.vo.StickerAlbum

class AlbumAdapter(fm: FragmentManager, private val albums: List<StickerAlbum>) : FragmentPagerAdapter(fm) {
    companion object {
        const val TYPE_RECENT = 0
        const val TYPE_LIKE = 1
        const val TYPE_GIPHY = 2
        const val TYPE_NORMAL = 3

        const val UN_NORMAL_COUNT = 3
    }

    var callback: StickerAlbumFragment.Callback? = null

    override fun getItem(position: Int): Fragment {
        val stickerFragment = when (position) {
            TYPE_RECENT -> StickerFragment.newInstance(type = TYPE_RECENT)
            TYPE_LIKE -> StickerFragment.newInstance(type = TYPE_LIKE)
            TYPE_GIPHY -> StickerFragment.newInstance(type = TYPE_GIPHY)
            else -> StickerFragment.newInstance(albums[position - UN_NORMAL_COUNT].albumId, TYPE_NORMAL)
        }
        stickerFragment.setCallback(object : Callback {
            override fun onStickerClick(stickerId: String) {
                callback?.onStickerClick(stickerId)
            }

            override fun onGiphyClick(url: String) {
                callback?.onGiphyClick(url)
            }
        })
        return stickerFragment
    }

    override fun getCount(): Int = albums.size + UN_NORMAL_COUNT

    fun getTabView(pos: Int, context: Context): View {
        val view = View.inflate(context, R.layout.layout_sticker_tab, null)
        when (pos) {
            TYPE_RECENT -> view.icon.setImageResource(R.drawable.ic_access_time_gray_24dp)
            TYPE_LIKE -> view.icon.setImageResource(R.drawable.ic_favorite_border_gray_24dp)
            TYPE_GIPHY -> view.icon.setImageResource(R.drawable.ic_sticker_gif)
            else -> view.icon.loadImage(albums[pos - UN_NORMAL_COUNT].iconUrl)
        }
        return view
    }

    interface Callback {
        fun onStickerClick(stickerId: String)
        fun onGiphyClick(url: String)
    }
}
