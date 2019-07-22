package one.mixin.android.ui.conversation.adapter

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import one.mixin.android.ui.conversation.GalleryItemFragment
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album

class GalleryAlbumAdapter(
    private val context: Context,
    fm: FragmentManager
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    var albums: List<Album>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val pageMap = ArrayMap<Int, GalleryItemFragment>()

    override fun getItem(position: Int): Fragment {
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

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        pageMap.remove(position)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return albums!![position].getDisplayName(context)
    }

    override fun getCount() = albums?.size ?: 0

    fun getFragment(index: Int) = pageMap[index]
}
