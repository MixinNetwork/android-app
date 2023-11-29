package one.mixin.android.ui.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.view.View
import android.view.ViewGroup
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMusicBottomSheetBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.player.MusicService.Companion.MUSIC_PLAYLIST
import one.mixin.android.ui.player.internal.UrlLoader
import one.mixin.android.ui.player.internal.album
import one.mixin.android.ui.player.internal.albumArtUri
import one.mixin.android.ui.player.internal.displaySubtitle
import one.mixin.android.ui.player.internal.displayTitle
import one.mixin.android.ui.player.internal.id
import one.mixin.android.ui.player.internal.urlLoader
import one.mixin.android.util.MusicPlayer
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.FixedMessageDataSource
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import one.mixin.android.widget.MixinBottomSheetDialog
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
@UnstableApi
@AndroidEntryPoint
class MusicBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "MusicBottomSheetDialogFragment"
        const val CONVERSATION_UI_PAGE_SIZE = 15

        fun newInstance(conversationId: String) =
            MusicBottomSheetDialogFragment().withArgs {
                putString(EXTRA_CONVERSATION_ID, conversationId)
            }
    }

    private val stopScope = scope(Lifecycle.Event.ON_STOP)

    private val binding by viewBinding(FragmentMusicBottomSheetBinding::inflate)

    private val conversationId: String by lazy {
        requireArguments().getString(EXTRA_CONVERSATION_ID)!!
    }

    override fun getTheme() = R.style.MixinBottomSheet

    private val listAdapter = MediaItemAdapter()

    private lateinit var contentView: View

    private var firstOpen = true

    private val viewModel by viewModels<MusicViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme).apply {
            dismissWithAnimation = true
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setContentView(contentView)

        val params = (contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = params?.behavior as? BottomSheetBehavior<*>
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        behavior?.skipCollapsed = true
        behavior?.peekHeight = contentView.measuredHeight
        behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)

        binding.apply {
            listAdapter.listener =
                object : MediaItemListener {
                    override fun onItemClick(mediaItem: MediaMetadataCompat) {
                        val id = requireNotNull(mediaItem.id)
                        if (MusicPlayer.isPlay(id)) {
                            MusicPlayer.pause()
                        } else {
                            val albumId = requireNotNull(mediaItem.album)
                            if (albumId == MUSIC_PLAYLIST) {
                                MusicPlayer.get().playMediaById(id)
                            } else {
                                MusicService.playConversation(requireContext(), requireNotNull(mediaItem.album), id)
                            }
                        }
                    }

                    override fun onDownload(mediaItem: MediaMetadataCompat) {
                        download(requireNotNull(mediaItem.id))
                    }

                    override fun onCancel(mediaItem: MediaMetadataCompat) {
                        viewModel.cancel(requireNotNull(mediaItem.id), conversationId)
                    }
                }
            titleView.leftIv.setPadding(12.dp)
            titleView.rightIv.setPadding(8.dp)
            titleView.leftIv.setOnClickListener { dismiss() }
            titleView.rightIv.setOnClickListener {
                alertDialogBuilder()
                    .setMessage(getString(R.string.Stop_playing_this_list))
                    .setNegativeButton(R.string.Cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.Stop_Playing) { _, _ ->
                        MusicPlayer.pause()
                        MusicService.stopMusic(this@MusicBottomSheetDialogFragment.requireContext())
                        lifecycleScope.launch {
                            (requireActivity() as MusicActivity).serviceStopped = true
                            dismiss()
                        }
                    }
                    .show()
            }
            playlistRv.layoutManager = LinearLayoutManager(requireContext())
            playlistRv.adapter = listAdapter

            musicLayout.progress.isVisible = false

            if (conversationId == MUSIC_PLAYLIST) {
                urlLoader.addObserver(urlObserver)
            } else {
                lifecycleScope.launch {
                    val mediaId = MusicPlayer.get().currentPlayMediaId()
                    val index =
                        if (mediaId != null) {
                            viewModel.indexAudioByConversationId(conversationId, mediaId)
                        } else {
                            0
                        }
                    viewModel.conversationLiveData(conversationId, index)
                        .observe(this@MusicBottomSheetDialogFragment) { list ->
                            if (list.isEmpty()) return@observe
                            listAdapter.submitList(list)
                            pb.isVisible = false

                            if (firstOpen && mediaId != null) {
                                updateMusicLayout(list, mediaId)
                            }
                        }
                }
            }

            playerControlView.player = MusicPlayer.get().exoPlayer
            MusicPlayer.get().exoPlayer.addListener(playerListener)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
        (requireActivity() as MusicActivity).checkFloatingPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        urlLoader.removeObserver(urlObserver)
        binding.playerControlView.player = null
        MusicPlayer.get().exoPlayer.removeListener(playerListener)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().finish()
    }

    override fun dismiss() {
        if ((requireActivity() as MusicActivity).checkFloatingPermission()) {
            dismissAllowingStateLoss()
        }
    }

    override fun dismissAllowingStateLoss() {
        try {
            super.dismissAllowingStateLoss()
        } catch (ignored: IllegalStateException) {
        }
    }

    private fun updateMusicLayout(
        pagedList: PagedList<MediaMetadataCompat>,
        mediaId: String,
    ) {
        binding.apply {
            var mediaItem: MediaMetadataCompat? = null
            for (i in 0 until pagedList.size) {
                val item = pagedList[i]
                if (item != null && item.id == mediaId) {
                    mediaItem = item
                    break
                }
            }
            if (mediaItem == null) {
                lifecycleScope.launch {
                    val index = viewModel.indexAudioByConversationId(conversationId, mediaId)
                    pagedList.loadAround(max(0, min(pagedList.size - 1, index)))
                    firstOpen = true
                    return@launch
                }
            }
            musicLayout.title.text = mediaItem?.displayTitle
            musicLayout.subtitle.text = mediaItem?.displaySubtitle
            musicLayout.albumArt.loadImage(mediaItem?.albumArtUri?.path, R.drawable.ic_music_place_holder)

            if (pagedList.isNotEmpty() && firstOpen) {
                firstOpen = false
                val pos = pagedList.indexOf(mediaItem)
                if (pos != -1) {
                    playlistRv.post {
                        (playlistRv.layoutManager as LinearLayoutManager).scrollToPosition(pos)
                    }
                }
            }
        }
    }

    private fun download(mediaId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            lifecycleScope.launch {
                                viewModel.download(mediaId)
                            }
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {},
                )
        } else {
            lifecycleScope.launch {
                viewModel.download(mediaId)
            }
        }
    }

    private val playerListener =
        object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                if (reason == MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
                    reason == MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                    mediaItem == null
                ) {
                    return
                }
                val pagedList = listAdapter.currentList ?: return

                val mediaId = mediaItem.mediaId
                updateMusicLayout(pagedList, mediaId)
            }
        }

    @SuppressLint("RestrictedApi")
    private val urlObserver =
        UrlLoader.UrlObserver { list ->
            val pagedConfig =
                PagedList.Config.Builder()
                    .setPageSize(CONVERSATION_UI_PAGE_SIZE)
                    .build()
            val pagedList =
                PagedList.Builder(
                    FixedMessageDataSource(list, list.size),
                    pagedConfig,
                ).setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())
                    .setFetchExecutor(ArchTaskExecutor.getIOThreadExecutor())
                    .build()
            lifecycleScope.launch {
                listAdapter.submitList(pagedList)
                binding.pb.isVisible = false
            }
        }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }
}
