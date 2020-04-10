package one.mixin.android.ui.conversation.adapter

import android.net.Uri
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import one.mixin.android.ui.conversation.GalleryItemFragment
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album

class GalleryAlbumAdapter(
    context: FragmentActivity
) : FragmentStateAdapter(context) {

    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    var albums: List<Album>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val pageMap = ArrayMap<Int, GalleryItemFragment>()

    override fun getItemCount() = albums?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val fragment = GalleryItemFragment.newInstance(albums!![position], position == 0)
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

    fun getFragment(index: Int) = pageMap[index]
}
