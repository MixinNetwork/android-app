package one.mixin.android.ui.sticker

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerAlbumBottomSheetBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getSafeAreaInsetsBottom
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.realSize
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.StickerFragment.Companion.COLUMN
import one.mixin.android.ui.conversation.StickerFragment.Companion.PADDING
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.widget.MixinBottomSheetDialog
import kotlin.math.roundToInt

@AndroidEntryPoint
class StickerAlbumBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "StickerAlbumBottomSheetFragment"
        const val EXTRA_ALBUM_ID = "extra_album_id"

        @SuppressLint("StaticFieldLeak")
        private var instant: StickerAlbumBottomSheetFragment? = null

        fun newInstance(
            albumId: String,
        ): StickerAlbumBottomSheetFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return StickerAlbumBottomSheetFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(EXTRA_ALBUM_ID, albumId)
                    }
                instant = this
            }
        }
    }

    private lateinit var contentView: View

    private val viewModel by viewModels<ConversationViewModel>()

    private var _binding: FragmentStickerAlbumBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun getTheme() = R.style.MixinBottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme, true).apply {
            dismissWithAnimation = true
        }
    }

    private var translationOffset: Float = 0f
    private var safeAreaTopInset: Int = 0
    private var safeAreaBottomInset: Int = 0
    private var rootPaddingTop: Int = 0


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        _binding = FragmentStickerAlbumBottomSheetBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as BottomSheetBehavior<*>
        val peekHeight = requireContext().realSize().x + 80.dp + 48.dp
        behavior.peekHeight = peekHeight
        binding.root.doOnPreDraw { root ->
            val defaultPadding: Int = 20.dp
            translationOffset = (peekHeight - root.measuredHeight).toFloat()
            binding.bottomFl.translationY = translationOffset
            safeAreaTopInset = root.getSafeAreaInsetsTop()
            safeAreaBottomInset = root.getSafeAreaInsetsBottom()
            rootPaddingTop = root.paddingTop

            binding.root.setPadding(0, rootPaddingTop, 0, 0)
            binding.bottomFl.setPadding(
                0,
                defaultPadding,
                0,
                defaultPadding + safeAreaBottomInset,
            )
        }
        behavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int,
                ) {
                }

                override fun onSlide(
                    bottomSheet: View,
                    slideOffset: Float,
                ) {
                    val normalizedSlideOffset = slideOffset.coerceIn(0f, 1f)
                    val topPadding = (safeAreaTopInset * normalizedSlideOffset).roundToInt()
                    val expectedRootTop = rootPaddingTop + topPadding
                    if (binding.root.paddingTop != expectedRootTop) {
                        binding.root.setPadding(0, expectedRootTop, 0, 0)
                    }
                    binding.bottomFl.translationY = translationOffset * (1 - slideOffset)
                }
            },
        )
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        dialog.window?.setGravity(Gravity.BOTTOM)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val albumId = requireNotNull(requireArguments().getString(EXTRA_ALBUM_ID))
                viewModel.observeAlbumById(albumId)
                    .observe(this@StickerAlbumBottomSheetFragment) { album ->
                        binding.title.titleTv.text = album?.name
                        if (album != null) {
                            updateAction(album)
                        }
                    }
                val stickers = viewModel.findStickersByAlbumId(albumId)
                val padding = PADDING.dp

                val stickerAdapter = StickerAdapter()
                binding.apply {
                    title.rightIv.setOnClickListener { dismiss() }
                    stickerRv.layoutManager = GridLayoutManager(context, COLUMN)
                    stickerRv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
                    stickerAdapter.size =
                        (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN
                    stickerRv.adapter = stickerAdapter
                    stickerAdapter.submitList(stickers)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    private fun updateAction(album: StickerAlbum) {
        binding.actionTv.apply {
            if (album.added) {
                text = getString(R.string.Remove_Stickers)
                setBackgroundResource(R.drawable.bg_round_red_btn)
                setOnClickListener {
                    lifecycleScope.launch {
                        viewModel.updateAlbumAdded(StickerAlbumAdded(album.albumId, false, 0))
                    }
                }
            } else {
                text = getString(R.string.Add_stickers)
                setBackgroundResource(R.drawable.bg_round_blue_btn)
                setOnClickListener {
                    lifecycleScope.launch {
                        val maxOrder = viewModel.findMaxOrder()
                        viewModel.updateAlbumAdded(StickerAlbumAdded(album.albumId, true, maxOrder + 1))
                    }
                }
            }
        }
    }
}
