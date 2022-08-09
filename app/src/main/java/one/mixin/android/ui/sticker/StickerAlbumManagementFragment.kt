package one.mixin.android.ui.sticker

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AlbumUploadRequest
import one.mixin.android.databinding.FragmentStickerAlbumManagementBinding
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
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

    private val selectZip = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult

        lifecycleScope.launch {
            val dialog = indeterminateProgressDialog(
                message = R.string.Please_wait_a_bit,
                title = R.string.Creating
            ).apply {
                setCancelable(false)
            }
            dialog.show()

            val albumUploadRequest = withContext(Dispatchers.IO) {
                val bytes = requireContext().contentResolver.openInputStream(uri)?.readBytes()
                if (bytes == null) {
                    null
                } else {
                    AlbumUploadRequest(bytes.base64RawEncode())
                }
            }
            if (albumUploadRequest == null) {
                dialog.dismiss()
                return@launch
            }

            handleMixinResponse(
                invokeNetwork = { viewModel.uploadAlbum(albumUploadRequest) },
                switchContext = Dispatchers.IO,
                successBlock = { rep ->
                    val album = requireNotNull(rep.data)
                    viewModel.saveAlbum(album)
                },
                doAfterNetworkSuccess = { dialog.dismiss() },
                exceptionBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            if (BuildConfig.DEBUG) {
                title.setRightIcon(R.drawable.ic_add_black_24dp)
                title.rightIb.setOnClickListener { uploadAlbum() }
            }
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

    private fun uploadAlbum() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .autoDispose(stopScope)
            .subscribe(
                { granted ->
                    if (granted) {
                        selectZip.launch("application/zip")
                    } else {
                        context?.openPermissionSetting()
                    }
                },
                {
                }
            )
    }
}
