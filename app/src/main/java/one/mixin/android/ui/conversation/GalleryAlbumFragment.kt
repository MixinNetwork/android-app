package one.mixin.android.ui.conversation

import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import one.mixin.android.R
import one.mixin.android.databinding.FragmentGalleryAlbumBinding
import one.mixin.android.ui.conversation.adapter.GalleryAlbumAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.model.AlbumCollection
import timber.log.Timber

class GalleryAlbumFragment : Fragment(R.layout.fragment_gallery_album), AlbumCollection.AlbumCallbacks {

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

    private val binding by viewBinding(FragmentGalleryAlbumBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            viewPager.adapter = albumAdapter
            TabLayoutMediator(
                albumTl,
                viewPager
            ) { tab, position ->
                context?.let {
                    tab.text = albumAdapter.albums?.get(position)?.getDisplayName(it)
                    viewPager.setCurrentItem(tab.position, true)
                }
            }.attach()
            albumTl.tabMode = TabLayout.MODE_SCROLLABLE
            viewPager.currentItem = 0
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
        }

        albumCollection.onCreate(this, this)
        albumCollection.onRestoreInstanceState(savedInstanceState)
        albumCollection.loadAlbums()
        Timber.e("$TAG albumCollection init and start load albums")

        requireContext().contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalObserver
        )
        requireContext().contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalObserver
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        albumCollection.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    override fun onStop() {
        super.onStop()
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding.va.removeCallbacks(restartLoadRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().contentResolver.unregisterContentObserver(internalObserver)
        requireContext().contentResolver.unregisterContentObserver(externalObserver)
        albumCollection.onDestroy()
    }

    override fun onAlbumLoad(cursor: Cursor?) {
        Timber.e("$TAG onAlbumLoad cursor: $cursor")
        if (cursor == null || cursor.isClosed) return

        binding.apply {
            va.post {
                Timber.e("$TAG onAlbumLoad before loop cursor")
                val albums = arrayListOf<Album>()
                va.displayedChild = POS_CONTENT
                while (!cursor.isClosed && cursor.moveToNext()) {
                    val album = Album.valueOf(cursor)
                    Timber.e("$TAG onAlbumLoad loop cursor album: $album")
                    albums.add(album)
                }
                Timber.e("$TAG onAlbumLoad albums size: ${albums.size}")
                if (albums.isNullOrEmpty()) return@post

                if (albumTl.tabCount == 0) {
                    albums.forEach { album ->
                        albumTl.addTab(albumTl.newTab().setText(album.getDisplayName(requireContext())))
                    }
                }
                albumAdapter.albums = albums
            }
        }
    }

    override fun onAlbumReset() {
        Timber.e("$TAG onAlbumReset")
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            if (state != ViewPager.SCROLL_STATE_IDLE) {
                albumAdapter.getFragment(binding.viewPager.currentItem)?.hideBlur()
            }
        }
    }

    private val internalObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            Timber.e("$TAG internalObserver onChange selfChange: $selfChange")
            binding.va.postDelayed(restartLoadRunnable, 2000)
        }
    }

    private val externalObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            Timber.e("$TAG externalObserver onChange selfChange: $selfChange")
            binding.va.postDelayed(restartLoadRunnable, 2000)
        }
    }

    private val restartLoadRunnable = Runnable {
        albumCollection.restartLoader()
    }
}
