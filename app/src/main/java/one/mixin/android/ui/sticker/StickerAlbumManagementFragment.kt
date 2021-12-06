package one.mixin.android.ui.sticker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerAlbumManagementBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.StickerAlbum
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
                }

                override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
                    itemTouchHelper.startDrag(viewHolder)
                }
            }
            viewModel.getSystemAlbums().observe(viewLifecycleOwner) { albums ->
                albumAdapter.submitList(albums)
                if (albums.isNullOrEmpty()) {
                    albumVa.displayedChild = 0
                } else {
                    albumVa.displayedChild = 1
                }
            }
        }
    }
}
