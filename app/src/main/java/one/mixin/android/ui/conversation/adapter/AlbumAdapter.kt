package one.mixin.android.ui.conversation.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
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
        const val TYPE_NORMAL = 2
    }

    var callback: StickerAlbumFragment.Callback? = null

    override fun getItem(position: Int): Fragment {
        val stickerFragment = when (position) {
            TYPE_RECENT -> StickerFragment.newInstance(type = TYPE_RECENT)
            TYPE_LIKE -> StickerFragment.newInstance(type = TYPE_LIKE)
            else -> StickerFragment.newInstance(albums[position - 2].albumId, TYPE_NORMAL)
        }
        stickerFragment.setCallback(object : Callback {
            override fun onStickerClick(albumId: String, name: String) {
                callback?.onStickerClick(albumId, name)
            }
        })
        return stickerFragment
    }

    override fun getCount(): Int = albums.size + 2

    fun getTabView(pos: Int, context: Context): View {
        val view = View.inflate(context, R.layout.layout_sticker_tab, null)
        when (pos) {
            TYPE_RECENT -> view.icon.setImageResource(R.drawable.ic_access_time_gray_24dp)
            TYPE_LIKE -> view.icon.setImageResource(R.drawable.ic_favorite_border_gray_24dp)
            else -> view.icon.loadImage(albums[pos - 2].iconUrl)
        }
        return view
    }

    interface Callback {
        fun onStickerClick(albumId: String, name: String)
    }
}
