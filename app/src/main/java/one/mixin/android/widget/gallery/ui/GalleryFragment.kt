package one.mixin.android.widget.gallery.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_gallery.*
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.entity.Item
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec
import one.mixin.android.widget.gallery.internal.model.AlbumCollection
import one.mixin.android.widget.gallery.internal.model.SelectedItemCollection
import one.mixin.android.widget.gallery.internal.ui.MediaSelectionFragment
import one.mixin.android.widget.gallery.internal.ui.adapter.AlbumMediaAdapter
import one.mixin.android.widget.gallery.internal.ui.adapter.AlbumsAdapter
import one.mixin.android.widget.gallery.internal.utils.MediaStoreCompat

class GalleryFragment : Fragment(), AlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener,
    MediaSelectionFragment.SelectionProvider, AlbumMediaAdapter.CheckStateListener,
    AlbumMediaAdapter.OnMediaClickListener, AlbumMediaAdapter.OnPhotoCapture {
    private val mAlbumCollection = AlbumCollection()
    private val mSelectedCollection by lazy { SelectedItemCollection(requireContext()) }

    private lateinit var mMediaStoreCompat: MediaStoreCompat
    private lateinit var mSpec: SelectionSpec

    private lateinit var mAlbumsAdapter: AlbumsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSpec = SelectionSpec.getInstance()
        super.onCreate(savedInstanceState)

        if (mSpec.capture) {
            mMediaStoreCompat = MediaStoreCompat(requireActivity(), this)
            if (mSpec.captureStrategy == null)
                throw RuntimeException("Don't forget to set CaptureStrategy.")
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy)
        }
        mSelectedCollection.onCreate(savedInstanceState)
        mAlbumsAdapter = AlbumsAdapter(requireContext(), null, false)
        mAlbumCollection.onCreate(requireActivity(), this)
        mAlbumCollection.onRestoreInstanceState(savedInstanceState)
        mAlbumCollection.loadAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mSelectedCollection.onSaveInstanceState(outState)
        mAlbumCollection.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAlbumCollection.onDestroy()
        mSpec.onSelectedListener = null
        previewVideoDialogFragment?.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            mMediaStoreCompat.currentPhotoUri?.let { imageUri ->
                showPreview(imageUri) {
                    onGalleryFragmentCallback?.onCameraClick(imageUri)
                }
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mAlbumCollection.setStateCurrentSelection(position)
        mAlbumsAdapter.cursor.moveToPosition(position)
        val album = Album.valueOf(mAlbumsAdapter.cursor)
        if (album.isAll && SelectionSpec.getInstance().capture) {
            album.addCaptureCount()
        }
        onAlbumSelected(album)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
    }

    override fun onAlbumLoad(cursor: Cursor) {
        mAlbumsAdapter.swapCursor(cursor)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            cursor.moveToPosition(mAlbumCollection.currentSelection)
            val album = Album.valueOf(cursor)
            if (album.isAll && SelectionSpec.getInstance().capture) {
                album.addCaptureCount()
            }
            onAlbumSelected(album)
        }
    }

    override fun onAlbumReset() {
        mAlbumsAdapter.swapCursor(null)
    }

    private fun onAlbumSelected(album: Album) {
        if (album.isAll && album.isEmpty) {
            gallery_container.visibility = View.GONE
            empty_view.visibility = View.VISIBLE
        } else {
            gallery_container.visibility = View.VISIBLE
            empty_view.visibility = View.GONE
            val tag = MediaSelectionFragment::class.java.simpleName
            val fragment = MediaSelectionFragment.newInstance(album).apply {
                setSelectionProvider(this@GalleryFragment)
                setCheckStateListener(this@GalleryFragment)
                setOnMediaClickListener(this@GalleryFragment)
                setOnPhotoCapture(this@GalleryFragment)
            }
            requireFragmentManager().inTransaction {
                replace(R.id.gallery_container, fragment!!, tag)
            }
        }
    }

    override fun onUpdate() {
        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(
                mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
        }
    }

    private var previewDialogFragment: PreviewDialogFragment? = null

    private fun showPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewDialogFragment == null) {
            previewDialogFragment = PreviewDialogFragment.newInstance()
        }
        previewDialogFragment?.show(requireFragmentManager(), uri, action)
    }

    private var previewVideoDialogFragment: PreviewDialogFragment? = null

    private fun showVideoPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewVideoDialogFragment == null) {
            previewVideoDialogFragment = PreviewDialogFragment.newInstance(true)
        }
        previewVideoDialogFragment?.show(requireFragmentManager(), uri, action)
    }

    override fun onMediaClick(album: Album, item: Item, adapterPosition: Int) {
        if (mSpec.preview) {
            if (item.isVideo) {
                showVideoPreview(item.uri) {
                    onGalleryFragmentCallback?.onGalleryClick(item.uri, true)
                }
            } else {
                showPreview(item.uri) {
                    onGalleryFragmentCallback?.onGalleryClick(item.uri, false)
                }
            }
        } else {
            if (item.isVideo) {
                onGalleryFragmentCallback?.onGalleryClick(item.uri, true)
            } else {
                onGalleryFragmentCallback?.onGalleryClick(item.uri, false)
            }
        }
    }

    override fun provideSelectedItemCollection(): SelectedItemCollection {
        return mSelectedCollection
    }

    @SuppressLint("CheckResult")
    override fun capture() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .subscribe({ granted ->
                if (granted) {
                    mMediaStoreCompat.dispatchCaptureIntent(requireContext(), REQUEST_CAMERA)
                } else {
                    context?.openPermissionSetting()
                }
            }, {
            })
    }

    var onGalleryFragmentCallback: OnGalleryFragmentCallback? = null

    interface OnGalleryFragmentCallback {
        fun onGalleryClick(uri: Uri, isVideo: Boolean)
        fun onCameraClick(imageUri: Uri)
    }

    companion object {
        const val TAG = "GalleryFragment"
        fun newInstance() = GalleryFragment()
    }
}
