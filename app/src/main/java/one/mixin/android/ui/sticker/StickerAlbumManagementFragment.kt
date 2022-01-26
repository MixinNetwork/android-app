package one.mixin.android.ui.sticker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerAlbumManagementBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.vo.StickerAlbumOrder
import one.mixin.android.widget.recyclerview.SimpleItemTouchHelperCallback

@AndroidEntryPoint
class StickerAlbumManagementFragment : BaseFragment(R.layout.fragment_sticker_album_management) {
    companion object {
        const val TAG = "StickerAlbumManagementFragment"
        fun newInstance() = StickerAlbumManagementFragment()
    }

    private val viewModel by viewModels<ConversationViewModel>()
    private val binding by viewBinding(FragmentStickerAlbumManagementBinding::bind)

    private val albumAdapter = StickerAlbumManagementAdapter()

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.leftIb.setOnClickListener { activity?.onBackPressed() }
            val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(albumAdapter)
            itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper.attachToRecyclerView(albumsRv)
            albumsRv.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = albumAdapter
            }
            albumAdapter.albumListener = object : AlbumListener {
                override fun onDelete(album: StickerAlbum) {
                    lifecycleScope.launch {
                        viewModel.updateAlbumAdded(StickerAlbumAdded(album.albumId, false, 0))
                    }
                }

                override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
                    itemTouchHelper.startDrag(viewHolder)
                }

                override fun endDrag() {
                    lifecycleScope.launch {
                        val orders = albumAdapter.data?.reversed()?.mapIndexed { index, item ->
                            StickerAlbumOrder(item.albumId, index + 1)
                        } ?: return@launch
                        viewModel.updateAlbumOrders(orders)
                    }
                }
            }
            viewModel.observeSystemAddedAlbums().observe(viewLifecycleOwner) { albums ->
                mutableListOf<StickerAlbum>().apply {
                    addAll(albums)
                    albumAdapter.data = this
                }
                if (albums.isNullOrEmpty()) {
                    albumVa.displayedChild = 0
                } else {
                    albumVa.displayedChild = 1
                }
            }
        }
    }
}
