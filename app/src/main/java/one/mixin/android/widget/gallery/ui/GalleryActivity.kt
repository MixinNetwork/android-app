package one.mixin.android.widget.gallery.ui

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_gallery.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.entity.Item
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec
import one.mixin.android.widget.gallery.internal.model.AlbumCollection
import one.mixin.android.widget.gallery.internal.model.SelectedItemCollection
import one.mixin.android.widget.gallery.internal.ui.MediaSelectionFragment
import one.mixin.android.widget.gallery.internal.ui.adapter.AlbumMediaAdapter
import one.mixin.android.widget.gallery.internal.ui.adapter.AlbumsAdapter
import one.mixin.android.widget.gallery.internal.ui.widget.AlbumsSpinner
import one.mixin.android.widget.gallery.internal.utils.MediaStoreCompat

class GalleryActivity : AppCompatActivity(), AlbumCollection.AlbumCallbacks,
    AdapterView.OnItemSelectedListener, MediaSelectionFragment.SelectionProvider,
    AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
    AlbumMediaAdapter.OnPhotoCapture {
    private val mAlbumCollection = AlbumCollection()
    private val mSelectedCollection = SelectedItemCollection(this)

    private lateinit var mMediaStoreCompat: MediaStoreCompat
    private lateinit var mSpec: SelectionSpec

    private lateinit var mAlbumsSpinner: AlbumsSpinner
    private lateinit var mAlbumsAdapter: AlbumsAdapter

    private fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    private fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isNightMode()) {
            setTheme(getNightThemeId())
            SystemUIManager.lightUI(window, false)
        } else {
            setTheme(getDefaultThemeId())
            SystemUIManager.lightUI(window, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = colorFromAttribute(R.attr.bg_white)
        }
        mSpec = SelectionSpec.getInstance()
        super.onCreate(savedInstanceState)
        if (!mSpec.hasInited) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContentView(R.layout.activity_gallery)

        if (mSpec.needOrientationRestriction()) {
            requestedOrientation = mSpec.orientation
        }

        if (mSpec.capture) {
            mMediaStoreCompat = MediaStoreCompat(this)
            if (mSpec.captureStrategy == null)
                throw RuntimeException("Don't forget to set CaptureStrategy.")
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy)
        }

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)
        val navigationIcon = toolbar.navigationIcon!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            navigationIcon.colorFilter = BlendModeColorFilter(colorFromAttribute(R.attr.icon_black), BlendMode.SRC_IN)
        } else {
            @Suppress("DEPRECATION")
            navigationIcon.setColorFilter(colorFromAttribute(R.attr.icon_black), PorterDuff.Mode.SRC_IN)
        }

        mSelectedCollection.onCreate(savedInstanceState)

        mAlbumsAdapter = AlbumsAdapter(this, null, false)
        mAlbumsSpinner = AlbumsSpinner(this)
        mAlbumsSpinner.setOnItemSelectedListener(this)
        mAlbumsSpinner.setSelectedTextView(selected_album)
        mAlbumsSpinner.setPopupAnchorView(toolbar)
        mAlbumsSpinner.setAdapter(mAlbumsAdapter)
        mAlbumCollection.onCreate(this, this)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
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
            mAlbumsSpinner.setSelection(
                this@GalleryActivity,
                mAlbumCollection.currentSelection
            )
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
            container.visibility = View.GONE
            empty_view.visibility = View.VISIBLE
        } else {
            container.visibility = View.VISIBLE
            empty_view.visibility = View.GONE
            val fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment, MediaSelectionFragment::class.java.simpleName)
                .commitAllowingStateLoss()
        }
    }

    override fun onUpdate() {
        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(
                mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString()
            )
        }
    }

    private var previewDialogFragment: PreviewDialogFragment? = null

    private fun showPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewDialogFragment == null) {
            previewDialogFragment = PreviewDialogFragment.newInstance()
        }
        previewDialogFragment?.show(supportFragmentManager, uri, action)
    }

    private var previewVideoDialogFragment: PreviewDialogFragment? = null

    private fun showVideoPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewVideoDialogFragment == null) {
            previewVideoDialogFragment = PreviewDialogFragment.newInstance(true)
        }
        previewVideoDialogFragment?.show(supportFragmentManager, uri, action)
    }

    override fun onMediaClick(album: Album, item: Item, adapterPosition: Int) {
        if (!mSpec.preview) {
            val result = Intent()
            result.data = item.uri
            if (item.isVideo) {
                result.putExtra(IS_VIDEO, true)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        } else if (item.isVideo) {
            showVideoPreview(item.uri) {
                val result = Intent()
                result.data = item.uri
                result.putExtra(IS_VIDEO, true)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        } else {
            showPreview(item.uri) {
                val result = Intent()
                result.data = item.uri
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }

    override fun provideSelectedItemCollection(): SelectedItemCollection {
        return mSelectedCollection
    }

    override fun capture() {
        mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE)
    }

    companion object {
        const val IS_VIDEO = "is_video"
        private const val REQUEST_CODE_CAPTURE = 24
    }
}
