package one.mixin.android.ui.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentMusicBottomSheetBinding
import one.mixin.android.event.ProgressEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.player.MusicService.Companion.MUSIC_PLAYLIST
import one.mixin.android.ui.player.internal.MusicMetaLoader
import one.mixin.android.ui.player.internal.UrlLoader
import one.mixin.android.ui.player.internal.album
import one.mixin.android.ui.player.internal.albumArtUri
import one.mixin.android.ui.player.internal.displaySubtitle
import one.mixin.android.ui.player.internal.displayTitle
import one.mixin.android.ui.player.internal.id
import one.mixin.android.ui.player.internal.urlLoader
import one.mixin.android.util.MusicPlayer
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.FixedMessageDataSource
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import one.mixin.android.widget.MixinBottomSheetDialog
import timber.log.Timber

@AndroidEntryPoint
class MusicBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "MusicBottomSheetDialogFragment"

        fun newInstance(conversationId: String) = MusicBottomSheetDialogFragment().withArgs {
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
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setContentView(contentView)

        val params = (contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = params?.behavior as? BottomSheetBehavior<*>
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        behavior?.skipCollapsed = true
        behavior?.peekHeight = contentView.measuredHeight
        behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)

        binding.apply {
            listAdapter.listener = object : MediaItemListener {
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
                viewModel.conversationLiveData(conversationId).observe(this@MusicBottomSheetDialogFragment) { list ->
                    listAdapter.submitList(list)
                    pb.isVisible = false
                }

                RxBus.listen(ProgressEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(stopScope)
                    .subscribe { event ->
                        if (event.status == STATUS_PLAY) {
                            val list = listAdapter.currentList ?: return@subscribe

                            var mediaItem = list.find { it?.id == MusicPlayer.get().currentPlayMediaId() }
                            if (mediaItem == null) {
                                mediaItem = list.firstOrNull()
                            }
                            musicLayout.title.text = mediaItem?.displayTitle
                            musicLayout.subtitle.text = mediaItem?.displaySubtitle
                            musicLayout.albumArt.loadImage(mediaItem?.albumArtUri?.path, R.drawable.ic_music_place_holder)

                            if (list.isNotEmpty() && firstOpen) {
                                firstOpen = false
                                val pos = list.indexOf(mediaItem)
                                if (pos != -1) {
                                    playlistRv.post {
                                        (playlistRv.layoutManager as LinearLayoutManager).scrollToPosition(pos)
                                    }
                                }
                            }
                        }
                    }
            }

            playerControlView.player = MusicPlayer.get().exoPlayer
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night)
            )
        }
        (requireActivity() as MusicActivity).checkFloatingPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        urlLoader.removeObserver(urlObserver)
        binding.playerControlView.player = null
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
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
    }

    private fun download(mediaId: String) {
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
                {}
            )
    }

    private val urlObserver = UrlLoader.UrlObserver { list ->
        Timber.d("@@@ urlObserver list size: ${list.size}")
        val pagedConfig = PagedList.Config.Builder()
            .setPageSize(MusicMetaLoader.PLAYLIST_PAGE_SIZE)
            .build()
        val pagedList = PagedList.Builder(
            FixedMessageDataSource(list, list.size),
            pagedConfig
        ).setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())
            .setFetchExecutor(ArchTaskExecutor.getIOThreadExecutor())
            .build()
        lifecycleScope.launch {
            listAdapter.submitList(pagedList)
            binding.pb.isVisible = false
        }
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismissAllowingStateLoss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }
}
