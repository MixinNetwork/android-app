package one.mixin.android.ui.conversation

import android.Manifest
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.checkout.threedsobfuscation.ad
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import one.mixin.android.R
import one.mixin.android.databinding.FragmentGalleryAlbumBinding
import one.mixin.android.databinding.ViewConversationBottomBinding
import one.mixin.android.databinding.ViewPermissionBottomBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.selectDocument
import one.mixin.android.ui.conversation.adapter.GalleryAlbumAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.entity.Item
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
        GalleryAlbumAdapter(this)
    }

    private val binding by viewBinding(FragmentGalleryAlbumBinding::bind)

    override fun onResume() {
        super.onResume()
        binding.permissionTv.isVisible = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && RxPermissions(requireActivity()).isGranted(Manifest.permission.READ_MEDIA_IMAGES) && RxPermissions(requireActivity()).isGranted(Manifest.permission.READ_MEDIA_VIDEO) -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && RxPermissions(requireActivity()).isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> true
            else -> false
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            viewPager.adapter = albumAdapter
            TabLayoutMediator(
                albumTl,
                viewPager,
            ) { tab, position ->
                context?.let {
                    tab.text = albumAdapter.albums?.get(position)?.getDisplayName(it)
                    viewPager.setCurrentItem(tab.position, true)
                }
            }.attach()
            albumTl.tabMode = TabLayout.MODE_SCROLLABLE
            viewPager.currentItem = 0
            va.displayedChild = POS_LOADING
            albumAdapter.callback =
                object : GalleryCallback {
                    override fun onItemClick(
                        pos: Int,
                        item: Item,
                        send: Boolean,
                    ) {
                        callback?.onItemClick(pos, item, send)
                    }

                    override fun onCameraClick() {
                        callback?.onCameraClick()
                    }
                }
            albumAdapter.rvCallback =
                object : DraggableRecyclerView.Callback {
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

        requireContext().contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            true,
            internalObserver,
        )
        requireContext().contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            externalObserver,
        )
        binding.permissionTv.highLight(getString(R.string.Manage))
        binding.permissionTv.setOnClickListener {
            showPermissionBottom()
        }
    }

    fun showPermissionBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val viewBinding = ViewPermissionBottomBinding.inflate(LayoutInflater.from(ContextThemeWrapper(requireActivity(), R.style.Custom)), null, false)
        builder.setCustomView(viewBinding.root)

        val bottomSheet = builder.create()
        viewBinding.cancel.setOnClickListener {
            bottomSheet.dismiss()
        }
        viewBinding.setting.setOnClickListener {
            requireActivity().openPermissionSetting()
            bottomSheet.dismiss()
        }
        viewBinding.select.setOnClickListener {
            RxPermissions(requireActivity()).request(
                *arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            ).autoDispose(stopScope).subscribe(
                { granted ->
                    if (RxPermissions(requireActivity()).isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
                        albumAdapter.getFragment(binding.viewPager.currentItem)?.reloadAlbum()
                    } else if (granted) {
                        albumAdapter.getFragment(binding.viewPager.currentItem)?.reloadAlbum()
                    } else {
                        requireActivity().openPermissionSetting()
                    }
                },
                {
                    Timber.e(it)
                })
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private val stopScope = scope(Lifecycle.Event.ON_STOP)

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
        callback = null
        rvCallback = null
    }

    override fun onAlbumLoad(cursor: Cursor?) {
        if (cursor == null || cursor.isClosed) return

        binding.apply {
            va.post {
                val albums = arrayListOf<Album>()
                va.displayedChild = POS_CONTENT
                while (!cursor.isClosed && cursor.moveToNext()) {
                    val album = Album.valueOf(cursor)
                    albums.add(album)
                }
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

    override fun onAlbumReset() {}

    private val onPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state != ViewPager.SCROLL_STATE_IDLE) {
                    albumAdapter.getFragment(binding.viewPager.currentItem)?.hideBlur()
                }
            }
        }

    private val internalObserver =
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                if (this@GalleryAlbumFragment.isAdded) {
                    binding.va.postDelayed(restartLoadRunnable, 2000)
                }
            }
        }

    private val externalObserver =
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                if (this@GalleryAlbumFragment.isAdded) {
                    binding.va.postDelayed(restartLoadRunnable, 2000)
                }
            }
        }

    private val restartLoadRunnable =
        Runnable {
            albumCollection.restartLoader()
        }
}
