package one.mixin.android.ui.conversation.adapter

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.DataSetObserver
import android.database.MergeCursor
import android.net.Uri
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import one.mixin.android.ui.conversation.GalleryItemFragment
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album

@SuppressLint("NotifyDataSetChanged")
class GalleryAlbumAdapter(
    private val context: FragmentActivity,
) : FragmentStateAdapter(context) {

    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    var cursor: Cursor? = null
    private var dataValid: Boolean = cursor != null
    private var rowIdColumn: Int = if (dataValid) cursor!!.getColumnIndex("_id") else -1
    private var dataSetObserver: DataSetObserver? = null

    private val pageMap = ArrayMap<Int, GalleryItemFragment>()

    init {
        dataSetObserver = NotifyingDataSetObserver(this)
        cursor?.registerDataSetObserver(dataSetObserver!!)
    }

    override fun getItemCount() = if (dataValid && cursor != null) cursor!!.count else 0

    override fun createFragment(position: Int): Fragment {
        cursor!!.moveToPosition(position)
        val album = Album.valueOf(cursor)
        val fragment = GalleryItemFragment.newInstance(album, position == 0)
        fragment.callback = object : GalleryCallback {
            override fun onItemClick(pos: Int, uri: Uri, isVideo: Boolean) {
                callback?.onItemClick(pos, uri, isVideo)
            }

            override fun onCameraClick() {
                callback?.onCameraClick()
            }
        }
        fragment.rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                rvCallback?.onScroll(dis)
            }

            override fun onRelease(fling: Int) {
                rvCallback?.onRelease(fling)
            }
        }
        pageMap[position] = fragment
        return fragment
    }

    override fun onBindViewHolder(holder: FragmentViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        val fragment: GalleryItemFragment? = context.supportFragmentManager.findFragmentByTag("f$position") as? GalleryItemFragment?
        fragment?.reloadAlbum()
    }

    fun getFragment(index: Int) = pageMap[index]

    fun changeCursor(cursor: Cursor) {
        val old = swapCursor(cursor)
        old?.close()
    }

    fun swapCursor(newCursor: Cursor): Cursor? {
        if (newCursor === cursor) {
            return null
        }
        val oldCursor: Cursor? = cursor
        if (oldCursor != null && dataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver)
            cursor = MergeCursor(arrayOf(oldCursor, newCursor))
        } else {
            cursor = newCursor
        }
        if (cursor != null) {
            if (dataSetObserver != null) {
                cursor?.registerDataSetObserver(dataSetObserver)
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id")
            dataValid = true
            notifyDataSetChanged()
        } else {
            rowIdColumn = -1
            dataValid = false
            notifyDataSetChanged()
        }
        return oldCursor
    }

    private class NotifyingDataSetObserver(private val adapter: GalleryAlbumAdapter) :
        DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            adapter.dataValid = true
            adapter.notifyDataSetChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            adapter.dataValid = false
        }
    }
}
