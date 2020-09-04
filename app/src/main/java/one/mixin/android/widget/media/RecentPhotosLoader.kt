package one.mixin.android.widget.media

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.loader.content.CursorLoader

class RecentPhotosLoader(context: Context) : CursorLoader(context) {

    override fun loadInBackground(): Cursor? {
        return context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            WHERE,
            null,
            MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        )
    }

    companion object {

        var BASE_URL = MediaStore.Images.Media.EXTERNAL_CONTENT_URI!!
        private const val WHERE = MediaStore.Images.Media.MIME_TYPE +
            "='image/jpeg'" + " OR " + MediaStore.Images.Media.MIME_TYPE +
            "='image/png'" + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
        private val PROJECTION = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.ORIENTATION,
            MediaStore.Images.ImageColumns.MIME_TYPE
        )
    }
}
