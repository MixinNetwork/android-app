package one.mixin.android.ui.conversation

import android.database.Cursor
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentDraggableRecyclerViewBinding
import one.mixin.android.event.DragReleaseEvent
import one.mixin.android.extension.isWideScreen
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.ui.conversation.adapter.GalleryItemAdapter
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.DIRECTION_NONE
import one.mixin.android.widget.DraggableRecyclerView.Companion.DIRECTION_TOP_2_BOTTOM
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.entity.Item
import one.mixin.android.widget.gallery.internal.model.AlbumMediaCollection
import org.jetbrains.anko.dip

class GalleryItemFragment : Fragment(R.layout.fragment_draggable_recycler_view), AlbumMediaCollection.AlbumMediaCallbacks {
    companion object {
        const val TAG = "GalleryItemFragment"
        const val ARGS_ALBUM = "args_album"
        const val ARGS_NEED_CAMERA = "args_need_camera"
        const val PADDING = 10
        var COLUMN = 3

        fun newInstance(album: Album, needCamera: Boolean) = GalleryItemFragment().withArgs {
            putParcelable(ARGS_ALBUM, album)
            putBoolean(ARGS_NEED_CAMERA, needCamera)
        }
    }

    var callback: GalleryCallback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    private val stopScope = scope(Lifecycle.Event.ON_STOP)

    private val album: Album by lazy { requireArguments().getParcelable(ARGS_ALBUM)!! }
    private val needCamera: Boolean by lazy { requireArguments().getBoolean(ARGS_NEED_CAMERA) }

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    private val adapter: GalleryItemAdapter by lazy {
        GalleryItemAdapter(needCamera)
    }

    private val albumMediaCollection = AlbumMediaCollection()

    private val binding by viewBinding(FragmentDraggableRecyclerViewBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        COLUMN = if (requireContext().isWideScreen()) {
            5
        } else {
            3
        }
        binding.apply {
            rv.layoutManager = GridLayoutManager(context, COLUMN)
            rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
            adapter.size = (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN
            rv.adapter = adapter
            adapter.listener = object : GalleryCallback {
                override fun onItemClick(pos: Int, item: Item, send: Boolean) {
                    callback?.onItemClick(pos, item, send)
                }

                override fun onCameraClick() {
                    callback?.onCameraClick()
                }
            }
            rv.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (rv.scrollState != SCROLL_STATE_IDLE) {
                            adapter.hideBLur()
                        }
                    }
                }
            )
            rv.callback = object : DraggableRecyclerView.Callback {
                override fun onScroll(dis: Float) {
                    rvCallback?.onScroll(dis)
                }

                override fun onRelease(fling: Int) {
                    rvCallback?.onRelease(fling)
                }
            }
        }

        albumMediaCollection.onCreate(this, this)
        albumMediaCollection.load(album)

        RxBus.listen(DragReleaseEvent::class.java)
            .autoDispose(stopScope)
            .subscribe {
                binding.rv.direction = if (it.isExpand) DIRECTION_TOP_2_BOTTOM else DIRECTION_NONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        albumMediaCollection.onDestroy()
    }

    override fun onAlbumMediaLoad(cursor: Cursor?) {
        if (cursor == null || cursor.isClosed) return

        binding.rv.post {
            val itemList = arrayListOf<Item>()
            while (!cursor.isClosed && cursor.moveToNext()) {
                val item = Item.valueOf(cursor)
                itemList.add(item)
            }
            if (itemList.isNullOrEmpty()) return@post

            adapter.items = itemList
        }
    }

    override fun onAlbumMediaReset() {}

    fun hideBlur() {
        adapter.hideBLur()
    }

    fun reloadAlbum() {
        albumMediaCollection.restartLoader(album)
    }
}
