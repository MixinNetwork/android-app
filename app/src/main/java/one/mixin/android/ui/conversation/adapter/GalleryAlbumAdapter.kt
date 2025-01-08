package one.mixin.android.ui.conversation.adapter

import android.annotation.SuppressLint
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import one.mixin.android.ui.conversation.GalleryItemFragment
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.entity.Item

class GalleryAlbumAdapter(
    private val fragment: Fragment,
) : FragmentStateAdapter(fragment) {
    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    var albums: List<Album>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val pageMap = ArrayMap<Int, GalleryItemFragment>()

    override fun getItemCount() = albums?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val fragment = GalleryItemFragment.newInstance(albums!![position], position == 0)
        fragment.callback =
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
        fragment.rvCallback =
            object : DraggableRecyclerView.Callback {
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

    override fun onBindViewHolder(
        holder: FragmentViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        super.onBindViewHolder(holder, position, payloads)
        val fragment: GalleryItemFragment? = fragment.childFragmentManager.findFragmentByTag("f$position") as? GalleryItemFragment?
        fragment?.reloadAlbum()
    }

    fun getFragment(index: Int) = pageMap[index]

}
