package one.mixin.android.ui.sticker

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentStickerPreviewBottomSheetBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SpacesItemDecoration

@AndroidEntryPoint
class StickerPreviewBottomSheetFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "StickerPreviewBottomSheetFragment"
        const val EXTRA_STICKER_ID = "extra_sticker_id"

        @SuppressLint("StaticFieldLeak")
        private var instant: StickerPreviewBottomSheetFragment? = null
        fun newInstance(
            stickerId: String,
        ): StickerPreviewBottomSheetFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return StickerPreviewBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_STICKER_ID, stickerId)
                }
                instant = this
            }
        }
    }

    private val viewModel by viewModels<ConversationViewModel>()
    private val binding by viewBinding(FragmentStickerPreviewBottomSheetBinding::inflate)

    private var firstLoad = true

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        val stickerId = requireNotNull(requireArguments().getString(EXTRA_STICKER_ID))
        val padding: Int = 4.dp
        val adapter = StickerAdapter()
        binding.apply {
            title.rightIv.setOnClickListener { dismiss() }
            stickerRv.apply {
                setHasFixedSize(true)
                addItemDecoration(SpacesItemDecoration(padding))
                layoutManager =
                    LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
                this.adapter = adapter
            }
            adapter.stickerListener = object : StickerListener {
                override fun onItemClick(sticker: Sticker) {
                    previewIv.loadImage(sticker.assetUrl)
                }
            }
        }

        val stickerLiveData = viewModel.observeStickerById(stickerId)
        val observer = object : Observer<Sticker> {
            override fun onChanged(sticker: Sticker) {
                binding.previewIv.loadImage(sticker.assetUrl)

                val albumId = sticker.albumId
                if (albumId.isNullOrBlank()) {
                    return
                }
                stickerLiveData.removeObserver(this)

                viewModel.observeSystemAlbumById(albumId)
                    .observe(this@StickerPreviewBottomSheetFragment) albumObserve@{ album ->
                        if (album == null) return@albumObserve

                        binding.tileTv.text = album.name
                        binding.actionTv.isVisible = album.category == "SYSTEM"
                        binding.actionTv.updateAlbumAdd(requireContext(), album.added) {
                            lifecycleScope.launch {
                                val maxOrder = viewModel.findMaxOrder()
                                viewModel.updateAlbumAdded(
                                    StickerAlbumAdded(
                                        albumId,
                                        true,
                                        maxOrder + 1
                                    )
                                )
                            }
                        }
                    }

                viewModel.observeSystemStickersByAlbumId(albumId)
                    .observe(this@StickerPreviewBottomSheetFragment) { stickers ->
                        adapter.submitList(stickers)
                        if (stickers.isNullOrEmpty()) {
                            if (firstLoad) {
                                binding.bottomVa.displayedChild = 1
                                firstLoad = false
                            } else {
                                binding.bottomVa.displayedChild = 0
                            }
                        } else {
                            if (binding.bottomVa.displayedChild != 2) {
                                binding.bottomVa.displayedChild = 2
                            }
                        }
                    }
            }
        }
        stickerLiveData.observe(this, observer)
        viewModel.refreshStickerAndRelatedAlbum(stickerId)
    }
}
