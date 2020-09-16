package one.mixin.android.ui.conversation

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_gallery_album.*
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.GalleryAlbumAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.model.AlbumCollection

class GalleryAlbumFragment : Fragment(), AlbumCollection.AlbumCallbacks {

    companion object {
        const val TAG = "GalleryAlbumFragment"

        const val POS_CONTENT = 0
        const val POS_LOADING = 1

        fun newInstance() = GalleryAlbumFragment()
    }

    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    private val albumCollection = AlbumCollection()

    private val albumAdapter: GalleryAlbumAdapter by lazy {
        GalleryAlbumAdapter(requireActivity())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_gallery_album, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view_pager.adapter = albumAdapter
        TabLayoutMediator(
            album_tl,
            view_pager
        ) { tab, position ->
            tab.text = albumAdapter.albums?.get(position)?.getDisplayName(requireContext())
            view_pager.setCurrentItem(tab.position, true)
        }.attach()
        album_tl.tabMode = TabLayout.MODE_SCROLLABLE
        view_pager.currentItem = 0
        va.displayedChild = POS_LOADING
        albumAdapter.callback = object : GalleryCallback {
            override fun onItemClick(pos: Int, uri: Uri, isVideo: Boolean) {
                callback?.onItemClick(pos, uri, isVideo)
            }

            override fun onCameraClick() {
                callback?.onCameraClick()
            }
        }
        albumAdapter.rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                rvCallback?.onScroll(dis)
            }

            override fun onRelease(fling: Int) {
                rvCallback?.onRelease(fling)
            }
        }
        view_pager.registerOnPageChangeCallback(onPageChangeCallback)

        albumCollection.onCreate(this, this)
        albumCollection.onRestoreInstanceState(savedInstanceState)
        albumCollection.loadAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        albumCollection.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        view_pager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        albumCollection.onDestroy()
    }

    private var first = true

    override fun onAlbumLoad(cursor: Cursor) {
        if (!first) return

        first = false
        va?.post {
            val albums = arrayListOf<Album>()
            va.displayedChild = POS_CONTENT
            while (cursor.moveToNext()) {
                val album = Album.valueOf(cursor)
                albums.add(album)
                album_tl.addTab(album_tl.newTab().setText(album.getDisplayName(requireContext())))
            }
            albumAdapter.albums = albums
        }
    }

    override fun onAlbumReset() {}

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            if (state != ViewPager.SCROLL_STATE_IDLE) {
                albumAdapter.getFragment(view_pager.currentItem)?.hideBlur()
            }
        }
    }
}
