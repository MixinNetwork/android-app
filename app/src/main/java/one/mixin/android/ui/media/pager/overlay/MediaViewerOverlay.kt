@file:Suppress("DEPRECATION")

package one.mixin.android.ui.media.pager.overlay

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.ItemPagerVideoLayoutBinding
import one.mixin.android.databinding.OverlayMediaViewerBinding
import one.mixin.android.databinding.ViewDragImageBottomBinding
import one.mixin.android.databinding.ViewDragVideoBottomBinding
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.copy
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createPngTemp
import one.mixin.android.extension.createWebpTemp
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.isAutoRotate
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.openAsUrlOrQrScan
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.shareMedia
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.windowWidth
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.session.Session
import one.mixin.android.ui.PipVideoView
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity.Companion.PAGE_SIZE
import one.mixin.android.ui.media.pager.MediaPagerActivity.Companion.PREFIX
import one.mixin.android.ui.media.pager.MediaPagerActivity.MediaSource
import one.mixin.android.ui.media.pager.MediaPagerAdapterListener
import one.mixin.android.ui.media.pager.PlayerControlView
import one.mixin.android.ui.media.pager.transcript.MediaPagerAdapterListener as TranscriptMediaPagerAdapterListener
import one.mixin.android.ui.qr.QRCodeProcessor
import one.mixin.android.util.SensorOrientationChangeNotifier
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.image.withDiskCacheFile
import one.mixin.android.util.reportEvent
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.appCardCoverUrl
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isAppCardWithMediaCover
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isMedia
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.loadVideoOrLive
import one.mixin.android.vo.saveToLocal
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import kotlin.math.min

/**
 * Telegram X-style non-Activity fullscreen media viewer overlay supporting both
 * conversation media (PagingData<MessageItem>) and transcript media (List<ChatHistoryMessageItem>).
 */
@OptIn(UnstableApi::class)
class MediaViewerOverlay private constructor(
    private val activity: FragmentActivity,
    private val mode: Mode,
    private val initialThumb: MediaThumbLocation?,
    private val thumbProvider: MediaThumbProvider?,
    private val onShowInChat: ((String) -> Unit)?,
) : MediaDismissLayout.OnDismissListener, SensorOrientationChangeNotifier.Listener {

    private sealed class Mode {
        data class Conversation(
            val conversationId: String,
            val messageId: String,
            val initialItem: MessageItem?,
            val mediaSource: MediaSource,
        ) : Mode()

        data class Transcript(
            val transcriptId: String,
            val messageId: String,
        ) : Mode()
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(activity.applicationContext, MediaOverlayEntryPoint::class.java)
    }
    private val conversationRepository get() = entryPoint.conversationRepository()
    private val jobManager get() = entryPoint.jobManager()

    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val binding = OverlayMediaViewerBinding.inflate(LayoutInflater.from(activity))
    private val dialog =
        MediaOverlayDialog(
            hostActivity = activity,
            keepScreenOn = true,
            hideFloatingViews = true,
            onDispatchTouchEvent = { ev ->
                binding.viewPager.isUserInputEnabled = ev.pointerCount < 2
            },
            onBackRequest = { dismissWithAnimation(0f) },
        )

    private val pipVideoView by lazy { PipVideoView.getInstance() }
    private val processor = QRCodeProcessor()

    private var conversationAdapter: OverlayMediaPagerAdapter? = null
    private var transcriptAdapter: OverlayTranscriptPagerAdapter? = null

    private var initialIndex = 0
    private var firstLoad = true
    private var hasSubmittedPagerData = false
    private var pendingConversationPosition: Int? = null
    private var isOpeningTransition = false
    private var isClosing = false
    private var hasDismissed = false
    private var manualFinish = false
    private var pipAnimationInProgress = false
    private var isLocked = false
    private var orientationModified = false
    private val initialOrientation = activity.requestedOrientation

    private var chromeVisible = true
    private var chromeVisibleBeforeDrag = true
    private var currentIsVideo = false
    private var inDismissState = false
    private var controllerVisibleBeforeDismiss = false
    private var topBarInsetTop = 0

    private var hiddenMessageId: String? = null
    private var hiddenSourceRestore: (() -> Unit)? = null
    private var dataJob: Job? = null
    private var permissionAlert: AlertDialog? = null

    private val hostLifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                SensorOrientationChangeNotifier.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                SensorOrientationChangeNotifier.pause()
            }

            override fun onStop(owner: LifecycleOwner) {
                handleHostStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                cleanupAndDismissImmediate()
            }
        }

    fun show() {
        setupInsets()
        setupTopBar()
        setupViewPager()

        VideoPlayer.player().setCycle(false)
        binding.lockTv.setOnClickListener(onLockClickListener)
        SensorOrientationChangeNotifier.init(this, activity.requestedOrientation)
        SensorOrientationChangeNotifier.resume()
        activity.lifecycle.addObserver(hostLifecycleObserver)

        prepareEnterTransitionState()
        dialog.setContentView(binding.root)
        dialog.show()

        loadData()
        runEnterTransition()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars =
                insets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
                )
            val navBars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            topBarInsetTop = bars.top
            binding.topBar.setPadding(
                maxOf(bars.left, navBars.left),
                topBarInsetTop,
                maxOf(bars.right, navBars.right),
                0,
            )
            insets
        }
    }

    private fun setupTopBar() {
        binding.backIv.setOnClickListener {
            dismissWithAnimation(0f)
        }
        binding.moreIv.setOnClickListener {
            openCurrentBottomMenu()
        }
        when (mode) {
            is Mode.Conversation -> {
                mode.initialItem?.let { updateTopBarForConversation(it) }
            }
            is Mode.Transcript -> {
                binding.titleTv.isVisible = false
                binding.subtitleTv.isVisible = false
            }
        }
    }

    private fun setupViewPager() {
        binding.viewPager.isSaveEnabled = false
        // Disable DefaultItemAnimator so PagingData updates never fade-in the visible page
        (binding.viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        val initialMessageId =
            when (mode) {
                is Mode.Conversation -> mode.messageId
                is Mode.Transcript -> mode.messageId
            }
        val initialPlaceholder: (() -> Drawable?)? =
            initialThumb?.drawable?.let { thumbDrawable ->
                { copyDrawable(thumbDrawable) }
            }
        when (mode) {
            is Mode.Conversation -> {
                conversationAdapter =
                    OverlayMediaPagerAdapter(
                        context = activity,
                        onDismissListener = this,
                        listener = conversationPagerListener,
                        initialMessageId = initialMessageId,
                        initialPlaceholderProvider = initialPlaceholder,
                    )
                binding.viewPager.adapter = conversationAdapter
            }
            is Mode.Transcript -> {
                transcriptAdapter =
                    OverlayTranscriptPagerAdapter(
                        context = activity,
                        onDismissListener = this,
                        listener = transcriptPagerListener,
                        initialMessageId = initialMessageId,
                        initialPlaceholderProvider = initialPlaceholder,
                    )
                binding.viewPager.adapter = transcriptAdapter
            }
        }
        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun prepareEnterTransitionState() {
        val thumb = initialThumb
        isOpeningTransition = true
        binding.backgroundView.alpha = 0f
        binding.topBar.alpha = 0f
        if (thumb != null && !thumb.isEmpty() && thumb.drawable != null) {
            binding.viewPager.visibility = View.INVISIBLE
        }
    }

    private fun runEnterTransition() {
        val thumb = initialThumb
        val initialMessageId =
            when (mode) {
                is Mode.Conversation -> mode.messageId
                is Mode.Transcript -> mode.messageId
            }

        if (thumb != null && !thumb.isEmpty() && thumb.drawable != null) {
            binding.viewPager.visibility = View.INVISIBLE
            binding.backgroundView.alpha = 0f
            binding.topBar.alpha = 0f

            binding.transitionView.doOnLayout {
                if (isClosing) return@doOnLayout
                if (!binding.transitionView.prepareOpen(thumb)) {
                    finishEnterTransitionImmediate()
                    return@doOnLayout
                }
                // Wait until the overlay window has committed its first frame covering the thumbnail
                // before hiding the underlying source view so there is no blank frame on open.
                binding.transitionView.doOnPreDraw {
                    binding.transitionView.post {
                        if (isClosing) return@post
                        hideSourceForMessage(initialMessageId, thumb)
                        binding.transitionView.startPreparedOpen(
                            onProgress = { t ->
                                binding.backgroundView.alpha = t
                                dialog.syncSystemBarAppearance(t)
                                if (!currentIsVideo && chromeVisible) {
                                    binding.topBar.alpha = t
                                }
                            },
                            onEnd = {
                                isOpeningTransition = false
                                binding.backgroundView.alpha = 1f
                                dialog.syncSystemBarAppearance(1f)
                                binding.viewPager.visibility = View.VISIBLE
                                binding.topBar.alpha = if (!currentIsVideo && chromeVisible) 1f else 0f
                                binding.topBar.isVisible = !currentIsVideo && chromeVisible
                                binding.viewPager.doOnPreDraw {
                                    binding.viewPager.post {
                                        if (!isClosing) {
                                            binding.transitionView.clearAndHide()
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } else {
            binding.backgroundView.alpha = 0f
            binding.topBar.alpha = 0f
            binding.backgroundView.animate()
                .alpha(1f)
                .setDuration(200L)
                .setUpdateListener { anim ->
                    dialog.syncSystemBarAppearance(anim.animatedFraction)
                }
                .withEndAction {
                    isOpeningTransition = false
                    dialog.syncSystemBarAppearance(1f)
                }
                .start()
            if (!currentIsVideo) {
                binding.topBar.animate().alpha(1f).setDuration(200L).start()
            }
        }
    }

    private fun finishEnterTransitionImmediate() {
        isOpeningTransition = false
        binding.backgroundView.alpha = 1f
        dialog.syncSystemBarAppearance(1f)
        binding.viewPager.visibility = View.VISIBLE
        binding.topBar.alpha = if (!currentIsVideo && chromeVisible) 1f else 0f
        binding.topBar.isVisible = !currentIsVideo && chromeVisible
        binding.transitionView.clearAndHide()
    }

    @SuppressLint("RestrictedApi")
    private fun loadData() {
        dataJob?.cancel()
        dataJob =
            overlayScope.launch {
                when (mode) {
                    is Mode.Conversation -> loadConversationData(mode)
                    is Mode.Transcript -> loadTranscriptData(mode)
                }
            }
    }

    private suspend fun loadConversationData(mode: Mode.Conversation) {
        val adapter = conversationAdapter ?: return
        val messageItem =
            mode.initialItem
                ?: withContext(Dispatchers.IO) {
                    conversationRepository.getMediaMessage(mode.conversationId, mode.messageId)
                }
                ?: return

        updateTopBarForConversation(messageItem)
        adapter.initialPos = 0
        adapter.submitData(dialog.lifecycle, PagingData.from(listOf(messageItem)))

        if (messageItem.isVideo() || messageItem.isLive()) {
            checkPip(mode.messageId)
            messageItem.loadVideoOrLive {
                VideoPlayer.player().start()
            }
        }

        if (messageItem.isAppCard() && (mode.mediaSource == MediaSource.SharedMedia || !messageItem.isAppCardWithMediaCover())) {
            checkOrientation()
            firstLoad = false
            observeStandaloneMessage(mode.messageId)
            return
        }

        val excludeLive = mode.mediaSource == MediaSource.SharedMedia
        initialIndex =
            withContext(Dispatchers.IO) {
                conversationRepository.indexMediaMessages(mode.conversationId, mode.messageId, excludeLive)
            }
        hasSubmittedPagerData = false
        adapter.addOnPagesUpdatedListener {
            if (firstLoad) {
                positionInitialConversationItem(mode, excludeLive)
            } else {
                val pendingPos = pendingConversationPosition
                if (pendingPos != null && binding.viewPager.currentItem == pendingPos) {
                    val loaded = getConversationItem(pendingPos)
                    if (loaded != null) {
                        pendingConversationPosition = null
                        applyConversationSelection(pendingPos, loaded)
                    }
                }
            }
        }
        Pager(
            config =
                PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PAGE_SIZE * 2,
                    enablePlaceholders = true,
                ),
            initialKey = initialIndex,
            pagingSourceFactory = {
                conversationRepository.getMediaMessagesPagingSource(mode.conversationId, excludeLive)
            },
        ).flow.collectLatest { pagingData ->
            hasSubmittedPagerData = true
            adapter.submitData(pagingData)
        }
    }

    private fun positionInitialConversationItem(
        mode: Mode.Conversation,
        excludeLive: Boolean,
    ) {
        val adapter = conversationAdapter ?: return
        if (!firstLoad || !hasSubmittedPagerData || adapter.itemCount <= 0) return
        val snapshot = adapter.snapshot()
        val itemAtIndex = snapshot.getOrNull(initialIndex)
        val targetIndex =
            if (itemAtIndex?.messageId == mode.messageId) {
                initialIndex
            } else {
                snapshot.indexOfFirst { it?.messageId == mode.messageId }
            }
        if (targetIndex < 0) {
            if (itemAtIndex == null) return
            overlayScope.launch {
                val total =
                    withContext(Dispatchers.IO) {
                        conversationRepository.countIndexMediaMessages(mode.conversationId, excludeLive)
                    }
                reportEvent("Initial index not found, conversationId: ${mode.conversationId}, messageId: ${mode.messageId}, initialIndex: $initialIndex, total: $total")
            }
            firstLoad = false
            mode.initialItem?.let { initialItem ->
                dataJob?.cancel()
                adapter.initialPos = 0
                adapter.submitData(dialog.lifecycle, PagingData.from(listOf(initialItem)))
                binding.viewPager.setCurrentItem(0, false)
                updateTopBarForConversation(initialItem)
                if (initialItem.isAppCard()) {
                    observeStandaloneMessage(mode.messageId)
                }
            }
            checkOrientation()
            return
        }
        runCatching {
            val targetItem = snapshot[targetIndex] ?: return
            adapter.initialPos = targetIndex
            if (binding.viewPager.currentItem != targetIndex) {
                binding.viewPager.setCurrentItem(targetIndex, false)
            }
            updateTopBarForConversation(targetItem)
        }.onFailure {
            overlayScope.launch {
                val total =
                    withContext(Dispatchers.IO) {
                        conversationRepository.countIndexMediaMessages(mode.conversationId, excludeLive)
                    }
                reportEvent("${it.message}, conversationId: ${mode.conversationId}, messageId: ${mode.messageId}, initialIndex: $initialIndex, total: $total")
            }
        }
        checkOrientation()
        firstLoad = false
    }

    private suspend fun loadTranscriptData(mode: Mode.Transcript) {
        val adapter = transcriptAdapter ?: return
        val items =
            withContext(Dispatchers.IO) {
                conversationRepository.getTranscriptMediaMessage(mode.transcriptId)
            }
        if (items.isEmpty()) return
        val matchedIndex = items.indexOfFirst { it.messageId == mode.messageId }
        val safeIndex =
            if (matchedIndex >= 0) {
                matchedIndex
            } else {
                withContext(Dispatchers.IO) {
                    conversationRepository.indexTranscriptMediaMessages(mode.transcriptId, mode.messageId)
                }.coerceIn(0, items.lastIndex)
            }
        adapter.initialPos = safeIndex
        adapter.list = items
        binding.viewPager.setCurrentItem(safeIndex, false)

        val current = items[safeIndex]
        updateTopBarForTranscript(current)
        if (current.isVideo() || current.isLive()) {
            checkPip(mode.messageId)
            current.loadVideoOrLive {
                VideoPlayer.player().start()
            }
        }
        checkOrientation()
    }

    private fun updateTopBarForConversation(item: MessageItem) {
        val isVideoOrLive = item.isVideo() || item.isLive()
        currentIsVideo = isVideoOrLive
        if (isVideoOrLive) {
            binding.topBar.isVisible = false
            attachVideoControlVisibilityListener(item.messageId)
        } else {
            val sender = item.userFullName.orEmpty()
            val time = runCatching { item.createdAt.fullDate() }.getOrDefault("")
            binding.titleTv.text = sender
            binding.titleTv.isVisible = sender.isNotBlank()
            binding.subtitleTv.text = time
            binding.subtitleTv.isVisible = time.isNotBlank()
            binding.moreIv.isVisible = item.isAppCard() || item.isImage()
            if (!isOpeningTransition && !binding.transitionView.isAnimating && !inDismissState) {
                binding.topBar.isVisible = chromeVisible
                binding.topBar.alpha = if (chromeVisible) 1f else 0f
                binding.topBar.translationY = 0f
            }
            dialog.setSystemBarsVisible(chromeVisible)
        }
    }

    private fun updateTopBarForTranscript(item: ChatHistoryMessageItem) {
        val isVideoOrLive = item.isVideo() || item.isLive()
        currentIsVideo = isVideoOrLive
        if (isVideoOrLive) {
            binding.topBar.isVisible = false
            attachVideoControlVisibilityListener(item.messageId)
        } else {
            val sender = item.userFullName.orEmpty()
            val time = runCatching { item.createdAt.fullDate() }.getOrDefault("")
            binding.titleTv.text = sender
            binding.titleTv.isVisible = sender.isNotBlank()
            binding.subtitleTv.text = time
            binding.subtitleTv.isVisible = time.isNotBlank()
            binding.moreIv.isVisible = item.isImage()
            if (!isOpeningTransition && !binding.transitionView.isAnimating && !inDismissState) {
                binding.topBar.isVisible = chromeVisible
                binding.topBar.alpha = if (chromeVisible) 1f else 0f
                binding.topBar.translationY = 0f
            }
            dialog.setSystemBarsVisible(chromeVisible)
        }
    }

    private fun attachVideoControlVisibilityListener(messageId: String) {
        binding.viewPager.post {
            if (isClosing) return@post
            val container = binding.viewPager.findViewWithTag<MediaDismissLayout>("$PREFIX$messageId") ?: return@post
            val playerView = runCatching { ItemPagerVideoLayoutBinding.bind(container).playerView }.getOrNull() ?: return@post
            playerView.playerControlView.visibilityListener =
                object : PlayerControlView.VisibilityListener {
                    override fun onVisibilityChange(visibility: Int) {
                        if (!isClosing && currentIsVideo) {
                            dialog.setSystemBarsVisible(visibility == View.VISIBLE)
                        }
                    }
                }
        }
    }

    private fun toggleImageChrome() {
        if (isClosing || currentIsVideo || binding.transitionView.isAnimating) return
        chromeVisible = !chromeVisible
        binding.topBar.animate().cancel()
        if (chromeVisible) {
            binding.topBar.isVisible = true
            binding.topBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(CHROME_ANIM_DURATION_MS)
                .setInterpolator(CHROME_INTERPOLATOR)
                .start()
        } else {
            val shift = -(binding.topBar.height.takeIf { it > 0 } ?: 120).toFloat() * 0.25f
            binding.topBar.animate()
                .alpha(0f)
                .translationY(shift)
                .setDuration(CHROME_ANIM_DURATION_MS)
                .setInterpolator(CHROME_INTERPOLATOR)
                .withEndAction {
                    if (!chromeVisible) {
                        binding.topBar.isVisible = false
                    }
                }
                .start()
        }
        dialog.setSystemBarsVisible(chromeVisible)
    }

    private fun openCurrentBottomMenu() {
        val pos = binding.viewPager.currentItem
        when (mode) {
            is Mode.Conversation -> {
                val item = getConversationItem(pos) ?: return
                val view = findPageContainer(item.messageId) ?: return
                if (item.isAppCard() || item.isImage()) {
                    showConversationImageBottom(item, view)
                } else if (item.isVideo()) {
                    showConversationVideoBottom(item)
                }
            }
            is Mode.Transcript -> {
                val item = transcriptAdapter?.getItem(pos) ?: return
                val view = findPageContainer(item.messageId) ?: return
                if (item.isImage()) {
                    showTranscriptImageBottom(item, view)
                } else if (item.isVideo()) {
                    showTranscriptVideoBottom(item)
                }
            }
        }
    }

    private fun hideSourceForMessage(
        messageId: String,
        location: MediaThumbLocation?,
    ) {
        if (hiddenMessageId == messageId && hiddenSourceRestore != null) return
        restoreHiddenSource()
        if (location != null) {
            location.setSourceVisible(false)
            hiddenMessageId = messageId
            hiddenSourceRestore = { location.setSourceVisible(true) }
        }
    }

    private fun restoreHiddenSource() {
        hiddenSourceRestore?.invoke()
        hiddenSourceRestore = null
        hiddenMessageId = null
    }

    private fun observeStandaloneMessage(messageId: String) {
        var sawMessage = false
        conversationRepository.findMessageItemByMessageId(messageId).observe(dialog) { item ->
            if (hasDismissed) return@observe
            if (item != null) {
                sawMessage = true
                if (item.type == MessageCategory.MESSAGE_RECALL.name || !item.isAppCard()) {
                    cleanupAndDismissImmediate()
                }
            } else if (sawMessage) {
                cleanupAndDismissImmediate()
            }
        }
    }

    private fun applyConversationSelection(
        position: Int,
        messageItem: MessageItem,
    ) {
        updateTopBarForConversation(messageItem)
        if (VideoPlayer.player().mId != messageItem.messageId && !pipVideoView.shown) {
            VideoPlayer.player().stop()
            VideoPlayer.player().pause()
        }
        if (downloadMedia(position)) return
        if (messageItem.isVideo() || messageItem.isLive()) {
            checkPip(messageItem.messageId)
            loadConversationVideoMessage(messageItem)
        }
    }

    private val onPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.lockTv.removeCallbacks(hideLockRunnable)
                binding.lockTv.post(hideLockRunnable)

                val currentId = getCurrentMessageId(position)
                if (!isOpeningTransition && currentId != null && currentId != hiddenMessageId) {
                    // Restore previous thumbnail so the underlying list stays intact when peeking via drag
                    val newLocation = thumbProvider?.findThumbLocation(currentId)
                    hideSourceForMessage(currentId, newLocation)
                }

                when (mode) {
                    is Mode.Conversation -> {
                        val messageItem = getConversationItem(position)
                        if (messageItem == null) {
                            pendingConversationPosition = position
                            if (!pipVideoView.shown) {
                                VideoPlayer.player().stop()
                                VideoPlayer.player().pause()
                            }
                            return
                        }
                        pendingConversationPosition = null
                        applyConversationSelection(position, messageItem)
                    }
                    is Mode.Transcript -> {
                        val messageItem = transcriptAdapter?.getItem(position) ?: return
                        updateTopBarForTranscript(messageItem)
                        if (VideoPlayer.player().mId != messageItem.messageId && !pipVideoView.shown) {
                            VideoPlayer.player().stop()
                            VideoPlayer.player().pause()
                        }
                        if (messageItem.isVideo() || messageItem.isLive()) {
                            checkPip(messageItem.messageId)
                            loadTranscriptVideoMessage(messageItem)
                        }
                    }
                }
            }
        }

    override fun onDragStart() {
        inDismissState = true
        chromeVisibleBeforeDrag = chromeVisible
        if (!currentIsVideo && chromeVisible) {
            binding.topBar.animate().cancel()
            binding.topBar.animate().alpha(0f).setDuration(120L).start()
        }
        if (currentIsVideo) {
            findCurrentPageContainer()?.let { container ->
                val playerView = runCatching { ItemPagerVideoLayoutBinding.bind(container).playerView }.getOrNull()
                if (playerView != null) {
                    controllerVisibleBeforeDismiss = playerView.useController && playerView.videoAspectRatio.isVisible
                    playerView.hideController()
                }
            }
        }
    }

    override fun onDragProgress(progress: Float) {
        val bgAlpha = (1f - progress).coerceIn(0f, 1f)
        binding.backgroundView.alpha = bgAlpha
        dialog.syncSystemBarAppearance(bgAlpha)
    }

    override fun onDismiss(velocityY: Float) {
        inDismissState = false
        dismissWithAnimation(velocityY)
    }

    override fun onDragCancel() {
        inDismissState = false
        binding.backgroundView.alpha = 1f
        dialog.syncSystemBarAppearance(1f)
        if (!currentIsVideo && chromeVisibleBeforeDrag) {
            binding.topBar.isVisible = true
            binding.topBar.animate().alpha(1f).setDuration(150L).start()
        }
        if (currentIsVideo) {
            findCurrentPageContainer()?.let { container ->
                val playerView = runCatching { ItemPagerVideoLayoutBinding.bind(container).playerView }.getOrNull()
                if (playerView != null) {
                    if (controllerVisibleBeforeDismiss) {
                        playerView.showController(false)
                    } else {
                        playerView.hideController()
                    }
                }
            }
        }
    }

    private fun dismissWithAnimation(velocityY: Float) {
        if (isClosing) return
        isClosing = true
        isOpeningTransition = false
        manualFinish = true
        binding.topBar.animate().cancel()
        val startTopBarAlpha = if (binding.topBar.isVisible) binding.topBar.alpha else 0f
        binding.lockTv.isVisible = false
        dialog.restoreSystemBarsForDismiss()

        val pos = binding.viewPager.currentItem
        val currentId = getCurrentMessageId(pos)
        val container = currentId?.let { findPageContainer(it) }
        container?.dismissEnabled = false

        if (currentIsVideo && container != null) {
            runCatching { ItemPagerVideoLayoutBinding.bind(container).playerView.hideController() }
        }

        val currentContent = resolveCurrentVisualSnapshot(container)
        val targetThumb = currentId?.let { thumbProvider?.findThumbLocation(it) }
        val startBgAlpha = binding.backgroundView.alpha

        if (targetThumb != null) {
            hideSourceForMessage(currentId, targetThumb)
        } else {
            restoreHiddenSource()
        }

        val drawableForClose = currentContent?.second ?: targetThumb?.drawable
        val rectForClose = currentContent?.first ?: RectF(0f, 0f, binding.root.width.toFloat(), binding.root.height.toFloat())

        if (drawableForClose != null) {
            if (targetThumb != null && !targetThumb.isEmpty()) {
                binding.transitionView.animateCloseToTarget(
                    currentScreenRect = rectForClose,
                    currentDrawable = drawableForClose,
                    targetLocation = targetThumb,
                    velocityY = velocityY,
                    onProgress = { t ->
                        val bgAlpha = startBgAlpha * (1f - t)
                        binding.backgroundView.alpha = bgAlpha
                        dialog.syncSystemBarAppearance(bgAlpha)
                        if (startTopBarAlpha > 0f) {
                            binding.topBar.alpha = startTopBarAlpha * (1f - (t * 2.5f).coerceIn(0f, 1f))
                        }
                    },
                    onEnd = {
                        completeCloseAfterHostFrame()
                    },
                )
            } else {
                binding.transitionView.animateFadeClose(
                    currentScreenRect = rectForClose,
                    currentDrawable = drawableForClose,
                    velocityY = velocityY,
                    onProgress = { t ->
                        val bgAlpha = startBgAlpha * (1f - t)
                        binding.backgroundView.alpha = bgAlpha
                        dialog.syncSystemBarAppearance(bgAlpha)
                        if (startTopBarAlpha > 0f) {
                            binding.topBar.alpha = startTopBarAlpha * (1f - (t * 2.5f).coerceIn(0f, 1f))
                        }
                    },
                    onEnd = {
                        completeCloseAfterHostFrame()
                    },
                )
            }
            binding.viewPager.visibility = View.INVISIBLE
        } else {
            cleanupAndDismissImmediate()
        }
    }

    private fun completeCloseAfterHostFrame() {
        binding.backgroundView.alpha = 0f
        binding.topBar.isVisible = false
        dialog.syncSystemBarAppearance(0f)
        // Reveal the underlying thumbnail first and keep the overlay window alive until
        // the host window has committed a frame, preventing a 1-frame flash on dismiss.
        restoreHiddenSource()
        val hostDecor = activity.window?.decorView
        if (hostDecor == null || !hostDecor.isAttachedToWindow) {
            cleanupAndDismissImmediate()
            return
        }
        val finishRunnable = Runnable { cleanupAndDismissImmediate() }
        hostDecor.postDelayed(finishRunnable, 48L)
        hostDecor.doOnPreDraw {
            hostDecor.post {
                hostDecor.removeCallbacks(finishRunnable)
                cleanupAndDismissImmediate()
            }
        }
        hostDecor.invalidate()
    }

    private fun resolveCurrentVisualSnapshot(container: MediaDismissLayout?): Pair<RectF, Drawable?>? {
        if (container == null) return null
        val primary = container.getPrimaryContentChild()
        if (primary is PhotoView) {
            val loc = IntArray(2)
            primary.getLocationOnScreen(loc)
            val displayRect = primary.displayRect
            val scale = primary.scaleX
            val rect =
                if (displayRect != null && displayRect.width() > 0f && displayRect.height() > 0f) {
                    RectF(
                        loc[0] + displayRect.left * scale,
                        loc[1] + displayRect.top * scale,
                        loc[0] + displayRect.right * scale,
                        loc[1] + displayRect.bottom * scale,
                    )
                } else {
                    RectF(
                        loc[0].toFloat(),
                        loc[1].toFloat(),
                        loc[0] + primary.width * scale,
                        loc[1] + primary.height * scale,
                    )
                }
            val drawable = copyDrawable(primary.drawable)
            return rect to drawable
        }

        if (currentIsVideo) {
            val videoBinding = runCatching { ItemPagerVideoLayoutBinding.bind(container) }.getOrNull()
            if (videoBinding != null) {
                val targetView: View =
                    if (videoBinding.previewIv.isVisible) {
                        videoBinding.previewIv
                    } else {
                        videoBinding.playerView.videoAspectRatio
                    }
                val loc = IntArray(2)
                targetView.getLocationOnScreen(loc)
                val scale = container.currentScale
                val rect =
                    RectF(
                        loc[0].toFloat(),
                        loc[1].toFloat(),
                        loc[0] + targetView.width * scale,
                        loc[1] + targetView.height * scale,
                    )
                val textureView = container.findViewById<TextureView>(R.id.video_texture)
                val frameBitmap =
                    if (!videoBinding.previewIv.isVisible && textureView != null && textureView.isAvailable) {
                        runCatching { textureView.bitmap }.getOrNull()
                    } else {
                        null
                    }
                val drawable =
                    frameBitmap?.let { BitmapDrawable(activity.resources, it) }
                        ?: copyDrawable(videoBinding.previewIv.drawable)
                return rect to drawable
            }
        }

        if (primary != null && primary.width > 0 && primary.height > 0) {
            val loc = IntArray(2)
            primary.getLocationOnScreen(loc)
            val scale = primary.scaleX
            val rect =
                RectF(
                    loc[0].toFloat(),
                    loc[1].toFloat(),
                    loc[0] + primary.width * scale,
                    loc[1] + primary.height * scale,
                )
            val snapshot = runCatching { primary.drawToBitmap(Bitmap.Config.RGB_565) }.getOrNull()
            val drawable = snapshot?.let { BitmapDrawable(activity.resources, it) }
            return rect to drawable
        }
        return null
    }

    private fun copyDrawable(drawable: Drawable?): Drawable? {
        drawable ?: return null
        return drawable.constantState?.newDrawable(activity.resources)?.mutate() ?: drawable
    }

    private fun cleanupAndDismissImmediate() {
        if (hasDismissed) return
        hasDismissed = true
        isClosing = true
        isOpeningTransition = false
        binding.transitionView.clearAndHide()
        binding.lockTv.removeCallbacks(hideLockRunnable)
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        activity.lifecycle.removeObserver(hostLifecycleObserver)

        restoreHiddenSource()
        if (orientationModified && !activity.isFinishing && !activity.isDestroyed) {
            activity.requestedOrientation = initialOrientation
            orientationModified = false
        }
        if (!pipVideoView.shown) {
            VideoPlayer.destroy()
        }
        processor.close()
        SensorOrientationChangeNotifier.reset()
        dataJob?.cancel()
        overlayScope.cancel()
        if (currentOverlay === this) {
            currentOverlay = null
        }
        dialog.dismiss()
    }

    private fun handleHostStop() {
        binding.lockTv.removeCallbacks(hideLockRunnable)
        if (isClosing || manualFinish || pipVideoView.shown) return

        when (mode) {
            is Mode.Conversation -> {
                val currMessage = getConversationItem(binding.viewPager.currentItem)
                if (currMessage == null) {
                    pauseVideoIfNotPip()
                    return
                }
                if (!VideoPlayer.player().isPlaying() || !(currMessage.isVideo() || currMessage.isLive())) return
                if (activity.checkInlinePermissions { pauseVideoIfNotPip() }) {
                    val v = findPageContainer(currMessage.messageId)
                    if (v != null) {
                        switchToPipConversation(currMessage, v)
                    } else {
                        pauseVideoIfNotPip()
                    }
                } else {
                    pauseVideoIfNotPip()
                }
            }
            is Mode.Transcript -> {
                pauseVideoIfNotPip()
            }
        }
    }

    private fun pauseVideoIfNotPip() {
        if (!pipVideoView.shown) {
            VideoPlayer.player().pause()
        }
    }

    override fun onOrientationChange(
        oldOrientation: Int,
        newOrientation: Int,
    ) {
        if (isClosing || !activity.isAutoRotate()) return
        showLock()
        if (isLocked) return
        changeOrientation(newOrientation)
    }

    @Synchronized
    private fun changeOrientation(orientation: Int) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) return
        orientationModified = true
        activity.requestedOrientation =
            when (orientation) {
                270 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                90 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        val portrait =
            activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        dialog.setSystemBarsVisible(portrait && (!currentIsVideo || chromeVisible))

        findCurrentPageContainer()?.let { container ->
            runCatching {
                ItemPagerVideoLayoutBinding.bind(container).playerView.switchFullscreen(orientation == 90 || orientation == 270)
            }
            container.resetTransformImmediate()
        }
    }

    private fun checkOrientation() {
        if (activity.isAutoRotate() && !isLocked) {
            val sensorIsLandscape = SensorOrientationChangeNotifier.isLandscape()
            val activityLandscape = activity.isLandscape()
            if (sensorIsLandscape != activityLandscape) {
                changeOrientation(SensorOrientationChangeNotifier.orientation)
            }
        }
    }

    private fun showLock() {
        binding.lockTv.isVisible = true
        binding.lockTv.removeCallbacks(hideLockRunnable)
        binding.lockTv.postDelayed(hideLockRunnable, 3000)
    }

    private val hideLockRunnable =
        Runnable {
            binding.lockTv.isVisible = false
        }

    private val onLockClickListener =
        View.OnClickListener {
            isLocked = !isLocked
            if (isLocked) {
                binding.lockTv.text = activity.getString(R.string.Click_to_unlock)
                binding.lockTv.textColor = activity.getColor(R.color.colorAccent)
            } else {
                binding.lockTv.text = activity.getString(R.string.Click_to_lock)
                binding.lockTv.textColor = activity.getColor(R.color.white)
            }
            binding.lockTv.removeCallbacks(hideLockRunnable)
            binding.lockTv.postDelayed(hideLockRunnable, 3000)
            checkOrientation()
        }

    private fun checkPip(messageId: String) {
        if (pipVideoView.shown) {
            pipVideoView.close(messageId != VideoPlayer.player().mId)
        }
    }

    private fun downloadMedia(position: Int): Boolean {
        val item = getConversationItem(position) ?: return false
        if (item.mediaStatus == MediaStatus.CANCELED.name) return false
        if (item.isMedia() && item.mediaUrl == null) {
            overlayScope.launch(Dispatchers.IO) {
                conversationRepository.suspendFindMessageById(item.messageId)?.let {
                    jobManager.addJobInBackground(AttachmentDownloadJob(it))
                }
            }
            return true
        }
        return false
    }

    private fun retryDownload(messageId: String) {
        overlayScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(messageId)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
    }

    private fun retryUpload(messageId: String) {
        overlayScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(messageId)?.let {
                if (it.isVideo() && it.mediaSize != null && it.mediaSize == 0L) {
                    try {
                        jobManager.addJobInBackground(
                            ConvertVideoJob(
                                it.conversationId,
                                it.userId,
                                Uri.parse(it.mediaUrl),
                                0f,
                                1f,
                                when {
                                    it.isSignal() -> EncryptCategory.SIGNAL
                                    it.isEncrypted() -> EncryptCategory.ENCRYPTED
                                    else -> EncryptCategory.PLAIN
                                },
                                it.messageId,
                                it.createdAt,
                            ),
                        )
                    } catch (e: NullPointerException) {
                        withContext(Dispatchers.Main) { toast(R.string.Retry_upload_failed) }
                    }
                } else if (it.isImage() && it.mediaSize != null && it.mediaSize == 0L) {
                    val category =
                        when {
                            it.isSignal() -> MessageCategory.SIGNAL_IMAGE
                            it.isEncrypted() -> MessageCategory.ENCRYPTED_IMAGE
                            else -> MessageCategory.PLAIN_IMAGE
                        }.name
                    try {
                        jobManager.addJobInBackground(
                            SendGiphyJob(
                                it.conversationId,
                                it.userId,
                                it.mediaUrl!!,
                                it.mediaWidth!!,
                                it.mediaHeight!!,
                                it.mediaSize,
                                category,
                                it.messageId,
                                it.thumbImage ?: "",
                                it.createdAt,
                            ),
                        )
                    } catch (e: NullPointerException) {
                        withContext(Dispatchers.Main) { toast(R.string.Retry_upload_failed) }
                    }
                } else {
                    jobManager.addJobInBackground(SendAttachmentMessageJob(it))
                }
            }
        }
    }

    private fun cancelTransfer(
        messageId: String,
        conversationId: String,
    ) {
        overlayScope.launch(Dispatchers.IO) {
            jobManager.cancelJobByMixinJobId(messageId) {
                overlayScope.launch(Dispatchers.IO) {
                    conversationRepository.updateMediaStatusSuspend(MediaStatus.CANCELED.name, messageId, conversationId)
                }
            }
        }
    }

    private fun loadConversationVideoMessage(messageItem: MessageItem) {
        if (messageItem.isVideo() || messageItem.isLive()) {
            messageItem.loadVideoOrLive {
                val view = findPageContainer(messageItem.messageId) ?: return@loadVideoOrLive
                ItemPagerVideoLayoutBinding.bind(view).playerView.player = VideoPlayer.player().player
            }
        }
    }

    private fun loadTranscriptVideoMessage(messageItem: ChatHistoryMessageItem) {
        if (messageItem.isVideo() || messageItem.isLive()) {
            messageItem.loadVideoOrLive {
                val view = findPageContainer(messageItem.messageId) ?: return@loadVideoOrLive
                ItemPagerVideoLayoutBinding.bind(view).playerView.player = VideoPlayer.player().player
            }
        }
    }

    private fun getConversationItem(position: Int): MessageItem? =
        try {
            conversationAdapter?.peek(position)
        } catch (_: IndexOutOfBoundsException) {
            null
        }

    private fun getCurrentMessageId(position: Int): String? =
        when (mode) {
            is Mode.Conversation -> getConversationItem(position)?.messageId
            is Mode.Transcript -> transcriptAdapter?.getItem(position)?.messageId
        }

    private fun findPageContainer(messageId: String): MediaDismissLayout? =
        binding.viewPager.findViewWithTag("$PREFIX$messageId")

    private fun findCurrentPageContainer(): MediaDismissLayout? {
        val id = getCurrentMessageId(binding.viewPager.currentItem) ?: return null
        return findPageContainer(id)
    }

    // region Bottom menus & actions

    private fun showConversationVideoBottom(messageItem: MessageItem) {
        val builder = BottomSheet.Builder(activity)
        val view = View.inflate(ContextThemeWrapper(activity, R.style.Custom), R.layout.view_drag_video_bottom, null)
        val sheetBinding = ViewDragVideoBottomBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        sheetBinding.showInChat.setOnClickListener {
            showInChat(bottomSheet, messageItem.messageId)
        }
        sheetBinding.saveVideo.setOnClickListener {
            requestStoragePermissionIfNeeded {
                overlayScope.launch {
                    messageItem.saveToLocal(activity)
                }
            }
            bottomSheet.dismiss()
        }
        sheetBinding.share.setOnClickListener {
            messageItem.absolutePath()?.let { activity.shareMedia(true, it) }
            bottomSheet.dismiss()
        }
        sheetBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun showConversationImageBottom(
        item: MessageItem,
        pagerItemView: View,
    ) {
        val builder = BottomSheet.Builder(activity)
        val view = View.inflate(ContextThemeWrapper(activity, R.style.Custom), R.layout.view_drag_image_bottom, null)
        val sheetBinding = ViewDragImageBottomBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        sheetBinding.showInChat.setOnClickListener {
            showInChat(bottomSheet, item.messageId)
        }
        sheetBinding.save.setOnClickListener {
            requestStoragePermissionIfNeeded {
                saveConversationImage(item)
            }
            bottomSheet.dismiss()
        }
        sheetBinding.shareImage.setOnClickListener {
            overlayScope.launch {
                val file = withContext(Dispatchers.IO) { resolveLocalFile(item, reencodeUnsupported = true) }
                if (file != null && file.exists()) {
                    activity.shareMedia(false, Uri.fromFile(file).toString())
                } else {
                    toast(R.string.Save_failure)
                }
            }
            bottomSheet.dismiss()
        }
        sheetBinding.decode.setOnClickListener {
            decodeQRCode(pagerItemView as ViewGroup)
            bottomSheet.dismiss()
        }
        sheetBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun showTranscriptVideoBottom(messageItem: ChatHistoryMessageItem) {
        val builder = BottomSheet.Builder(activity)
        val view = View.inflate(ContextThemeWrapper(activity, R.style.Custom), R.layout.view_drag_video_bottom, null)
        val sheetBinding = ViewDragVideoBottomBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        sheetBinding.showInChat.isVisible = false
        sheetBinding.saveVideo.setOnClickListener {
            requestStoragePermissionIfNeeded {
                overlayScope.launch {
                    messageItem.saveToLocal(activity)
                }
            }
            bottomSheet.dismiss()
        }
        sheetBinding.share.setOnClickListener {
            messageItem.absolutePath()?.let { shareTranscriptMedia(true, it) }
            bottomSheet.dismiss()
        }
        sheetBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun showTranscriptImageBottom(
        item: ChatHistoryMessageItem,
        pagerItemView: View,
    ) {
        val builder = BottomSheet.Builder(activity)
        val view = View.inflate(ContextThemeWrapper(activity, R.style.Custom), R.layout.view_drag_image_bottom, null)
        val sheetBinding = ViewDragImageBottomBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        sheetBinding.showInChat.isVisible = false
        sheetBinding.save.setOnClickListener {
            requestStoragePermissionIfNeeded {
                saveTranscriptImage(item)
            }
            bottomSheet.dismiss()
        }
        sheetBinding.shareImage.setOnClickListener {
            item.mediaUrl?.let { shareTranscriptMedia(false, it) }
            bottomSheet.dismiss()
        }
        sheetBinding.decode.setOnClickListener {
            decodeQRCode(pagerItemView as ViewGroup)
            bottomSheet.dismiss()
        }
        sheetBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun requestStoragePermissionIfNeeded(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(
                    { granted ->
                        if (granted) {
                            onGranted()
                        } else {
                            activity.openPermissionSetting()
                        }
                    },
                    {
                        toast(R.string.Save_failure)
                    },
                )
        } else {
            onGranted()
        }
    }

    private suspend fun resolveLocalFile(
        item: MessageItem,
        reencodeUnsupported: Boolean,
    ): File? {
        val coverUrl = item.appCardCoverUrl()
        if (coverUrl != null) {
            return try {
                val loader = activity.imageLoader
                val result = loader.execute(ImageRequest.Builder(activity).data(coverUrl).build())
                if (result !is SuccessResult) {
                    null
                } else {
                    loader.withDiskCacheFile(result) { cachedFile ->
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(cachedFile.absolutePath, options)
                        val suffix =
                            when {
                                options.outMimeType == MimeType.JPEG.toString() -> ".jpg"
                                options.outMimeType == MimeType.GIF.toString() -> ".gif"
                                options.outMimeType == MimeType.WEBP.toString() && !reencodeUnsupported -> ".webp"
                                else -> ".png"
                            }
                        val destination = createAppCardCoverCacheFile(suffix)
                        try {
                            if (suffix != ".png" || options.outMimeType == MimeType.PNG.toString()) {
                                cachedFile.copy(destination)
                            } else {
                                val bitmap = requireNotNull(BitmapFactory.decodeFile(cachedFile.absolutePath))
                                destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                bitmap.recycle()
                            }
                            destination
                        } catch (e: Exception) {
                            destination.delete()
                            throw e
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
        val path = item.absolutePath() ?: return null
        return Uri.parse(path).toFile()
    }

    private fun createAppCardCoverCacheFile(suffix: String): File {
        val directory = File(activity.getImageCachePath(), APP_CARD_COVER_CACHE_DIR)
        directory.mkdirs()
        val expiration = System.currentTimeMillis() - APP_CARD_COVER_CACHE_MAX_AGE
        directory.listFiles()?.filter { it.lastModified() < expiration }?.forEach { it.delete() }
        return File.createTempFile("cover-", suffix, directory)
    }

    private fun saveConversationImage(item: MessageItem) {
        overlayScope.launch(Dispatchers.IO) {
            val file = resolveLocalFile(item, reencodeUnsupported = false)
            if (file == null || !file.exists()) {
                withContext(Dispatchers.Main) { toast(R.string.Save_failure) }
                return@launch
            }
            val mimeType = item.mediaMimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            val outFile = createOutputImageFile(mimeType)
            try {
                outFile.copyFromInputStream(FileInputStream(file))
            } finally {
                if (item.appCardCoverUrl() != null) {
                    file.delete()
                }
            }
            MediaScannerConnection.scanFile(activity, arrayOf(outFile.toString()), null, null)
            withContext(Dispatchers.Main) {
                toast(activity.getString(R.string.Save_to, outFile.absolutePath))
            }
        }
    }

    private fun saveTranscriptImage(item: ChatHistoryMessageItem) {
        overlayScope.launch(Dispatchers.IO) {
            val path = item.absolutePath()
            if (path == null) {
                withContext(Dispatchers.Main) { toast(R.string.Save_failure) }
                return@launch
            }
            val file = Uri.parse(path).toFile()
            if (!file.exists()) {
                withContext(Dispatchers.Main) { toast(R.string.File_does_not_exist) }
                return@launch
            }
            val outFile = createOutputImageFile(item.mediaMimeType)
            outFile.copyFromInputStream(FileInputStream(file))
            MediaScannerConnection.scanFile(activity, arrayOf(outFile.toString()), null, null)
            withContext(Dispatchers.Main) {
                toast(activity.getString(R.string.Save_to, outFile.absolutePath))
            }
        }
    }

    private fun createOutputImageFile(mimeType: String?): File =
        when {
            mimeType.equals(MimeType.GIF.toString(), true) ->
                activity.getPublicPicturePath().createGifTemp(false)
            mimeType.equals(MimeType.PNG.toString(), true) ->
                activity.getPublicPicturePath().createPngTemp(false)
            mimeType.equals(MimeType.WEBP.toString(), true) ->
                activity.getPublicPicturePath().createWebpTemp(false)
            else ->
                activity.getPublicPicturePath().createImageTemp(noMedia = false)
        }

    private fun shareTranscriptMedia(
        isVideo: Boolean,
        url: String,
    ) {
        var uri: Uri
        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                uri = Uri.parse(url)
                if (ContentResolver.SCHEME_FILE == uri.scheme) {
                    val path = uri.getFilePath(activity)
                    if (path == null) {
                        toast(R.string.File_does_not_exist)
                        return
                    }
                    uri = activity.getUriForFile(File(path))
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                type = if (isVideo) "video/*" else "image/*"
            }
        val name = activity.getString(if (isVideo) R.string.Video else R.string.Photo)
        val chooser = Intent.createChooser(sendIntent, activity.getString(R.string.share_to, name))
        val resInfoList = activity.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
        resInfoList.forEach {
            activity.grantUriPermission(
                it.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        activity.startActivity(chooser)
    }

    private fun decodeQRCode(viewGroup: ViewGroup) {
        val imageView = viewGroup.getChildAt(0)
        val bitmap =
            if (imageView is ImageView) {
                val bitmapDrawable = imageView.drawable as? BitmapDrawable
                if (bitmapDrawable == null) {
                    toast(R.string.can_not_recognize_qr_code)
                    return
                } else {
                    bitmapDrawable.bitmap
                }
            } else {
                imageView.isDrawingCacheEnabled = true
                imageView.buildDrawingCache()
                imageView.drawingCache
            }
        if (bitmap != null) {
            processor.detect(
                activity.lifecycleScope,
                bitmap,
                onSuccess = { result ->
                    result.openAsUrlOrQrScan(activity, activity.supportFragmentManager, activity.lifecycleScope)
                },
                onFailure = { toast(R.string.can_not_recognize_qr_code) },
                onComplete = {
                    if (imageView !is ImageView) {
                        imageView.isDrawingCacheEnabled = false
                    }
                },
            )
        } else {
            toast(R.string.can_not_recognize_qr_code)
            if (imageView !is ImageView) {
                imageView.isDrawingCacheEnabled = false
            }
        }
    }

    private fun showInChat(
        bottomSheet: BottomSheet,
        messageId: String,
    ) {
        bottomSheet.dismiss()
        val conversationMode = mode as? Mode.Conversation ?: return
        if (onShowInChat != null) {
            cleanupAndDismissImmediate()
            onShowInChat.invoke(messageId)
        } else {
            ConversationActivity.showAndClear(activity, conversationMode.conversationId, messageId = messageId)
            cleanupAndDismissImmediate()
        }
    }

    // endregion

    // region PiP

    private fun switchToPipConversation(
        messageItem: MessageItem,
        windowView: View,
    ) {
        val conversationMode = mode as? Mode.Conversation ?: return
        switchToPipCommon(
            conversationId = conversationMode.conversationId,
            messageId = messageItem.messageId,
            isVideo = messageItem.isVideo(),
            mediaSource = conversationMode.mediaSource,
            absolutePath = messageItem.absolutePath(),
            windowView = windowView,
        )
    }

    private fun switchToPipTranscript(
        messageItem: ChatHistoryMessageItem,
        windowView: View,
    ) {
        switchToPipCommon(
            conversationId = messageItem.conversationId ?: "",
            messageId = messageItem.messageId,
            isVideo = messageItem.isVideo(),
            mediaSource = MediaSource.ChatHistory,
            absolutePath = messageItem.absolutePath(),
            windowView = windowView,
        )
    }

    private fun switchToPipCommon(
        conversationId: String,
        messageId: String,
        isVideo: Boolean,
        mediaSource: MediaSource,
        absolutePath: String?,
        windowView: View,
    ) {
        if (!checkPipPermission() || pipAnimationInProgress) return
        pipAnimationInProgress = true
        val videoAspectRatioLayout =
            ItemPagerVideoLayoutBinding.bind(windowView).playerView.videoAspectRatio
        val rect = PipVideoView.getPipRect(videoAspectRatioLayout.aspectRatio)
        val isLandscape = activity.isLandscape()
        if (isLandscape) {
            val screenHeight = activity.realSize().y
            if (rect.width > screenHeight) {
                val ratio = rect.width / rect.height
                rect.height = screenHeight / ratio
                rect.width = screenHeight.toFloat()
            }
        }
        val width = if (isLandscape) windowView.height else windowView.width
        val scale = (if (isLandscape) rect.height else rect.width) / width.coerceAtLeast(1)
        val changedTextureView =
            pipVideoView.show(
                videoAspectRatioLayout.aspectRatio,
                videoAspectRatioLayout.videoRotation,
                conversationId,
                messageId,
                isVideo,
                mediaSource,
                absolutePath,
            )

        val videoTexture = windowView.findViewById<TextureView>(R.id.video_texture)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(binding.backgroundView, View.ALPHA, 0f),
            ObjectAnimator.ofFloat(videoTexture, View.SCALE_X, scale),
            ObjectAnimator.ofFloat(videoTexture, View.SCALE_Y, scale),
            ObjectAnimator.ofFloat(
                videoAspectRatioLayout,
                View.TRANSLATION_X,
                rect.x - videoAspectRatioLayout.x -
                    activity.windowWidth() * (1f - scale) / 2,
            ),
            ObjectAnimator.ofFloat(
                videoAspectRatioLayout,
                View.TRANSLATION_Y,
                rect.y - videoAspectRatioLayout.y +
                    activity.statusBarHeight() - (videoAspectRatioLayout.height - rect.height) / 2,
            ),
        )
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.duration = 250
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    windowView.findViewById<View>(R.id.pip_iv)?.fadeOut()
                    windowView.findViewById<View>(R.id.close_iv)?.fadeOut()
                    windowView.findViewById<View>(R.id.live_tv)?.let {
                        if (it.isEnabled) it.fadeOut()
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    pipAnimationInProgress = false
                    if (isVideo && VideoPlayer.player().player.playbackState == Player.STATE_IDLE && absolutePath != null) {
                        VideoPlayer.player().loadVideo(absolutePath, messageId, true)
                        VideoPlayer.player().setVideoTextureView(changedTextureView)
                        VideoPlayer.player().pause()
                    } else {
                        VideoPlayer.player().setVideoTextureView(changedTextureView)
                    }
                    cleanupAndDismissImmediate()
                }
            },
        )
        animatorSet.start()
    }

    private fun checkPipPermission() =
        activity.checkInlinePermissions {
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions
            permissionAlert =
                AlertDialog.Builder(activity)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.call_pip_permission)
                    .setPositiveButton(R.string.Settings) { d, _ ->
                        try {
                            activity.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${activity.packageName}"),
                                ),
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                        d.dismiss()
                    }.show()
        }

    // endregion

    // region Adapter listeners

    private val conversationPagerListener =
        object : MediaPagerAdapterListener {
            override fun onClick(messageItem: MessageItem) {
                toggleImageChrome()
            }

            override fun onLongClick(
                messageItem: MessageItem,
                view: View,
            ) {
                if (messageItem.isAppCard() || messageItem.isImage()) {
                    showConversationImageBottom(messageItem, view)
                } else if (messageItem.isVideo()) {
                    showConversationVideoBottom(messageItem)
                }
            }

            override fun onCircleProgressClick(messageItem: MessageItem) {
                when (messageItem.mediaStatus) {
                    MediaStatus.CANCELED.name -> {
                        if (Session.getAccountId() == messageItem.userId) {
                            retryUpload(messageItem.messageId)
                        } else {
                            retryDownload(messageItem.messageId)
                        }
                    }
                    MediaStatus.PENDING.name -> {
                        cancelTransfer(messageItem.messageId, messageItem.conversationId)
                    }
                }
            }

            override fun onReadyPostTransition(view: View) {
                // No-op: enter transition is driven directly by MediaTransitionView
            }

            override fun switchToPin(
                messageItem: MessageItem,
                view: View,
            ) {
                switchToPipConversation(messageItem, view)
            }

            override fun finishAfterTransition() {
                dismissWithAnimation(0f)
            }

            override fun switchFullscreen() {
                val orientation = if (activity.isLandscape()) 0 else 270
                changeOrientation(orientation)
            }
        }

    private val transcriptPagerListener =
        object : TranscriptMediaPagerAdapterListener {
            override fun onClick(messageItem: ChatHistoryMessageItem) {
                toggleImageChrome()
            }

            override fun onLongClick(
                messageItem: ChatHistoryMessageItem,
                view: View,
            ) {
                if (messageItem.isImage()) {
                    showTranscriptImageBottom(messageItem, view)
                } else if (messageItem.isVideo()) {
                    showTranscriptVideoBottom(messageItem)
                }
            }

            override fun onCircleProgressClick(messageItem: ChatHistoryMessageItem) {
                when (messageItem.mediaStatus) {
                    MediaStatus.CANCELED.name -> {
                        if (Session.getAccountId() == messageItem.userId) {
                            retryUpload(messageItem.messageId)
                        } else {
                            retryDownload(messageItem.messageId)
                        }
                    }
                    MediaStatus.PENDING.name -> {
                        messageItem.conversationId?.let { cancelTransfer(messageItem.messageId, it) }
                    }
                }
            }

            override fun onReadyPostTransition(view: View) {
                // No-op: enter transition is driven directly by MediaTransitionView
            }

            override fun switchToPin(
                messageItem: ChatHistoryMessageItem,
                view: View,
            ) {
                switchToPipTranscript(messageItem, view)
            }

            override fun finishAfterTransition() {
                dismissWithAnimation(0f)
            }

            override fun switchFullscreen() {
                val orientation = if (activity.isLandscape()) 0 else 270
                changeOrientation(orientation)
            }
        }

    // endregion

    companion object {
        private const val APP_CARD_COVER_CACHE_DIR = "AppCardCover"
        private const val APP_CARD_COVER_CACHE_MAX_AGE = 24 * 60 * 60 * 1000L
        private const val CHROME_ANIM_DURATION_MS = 180L
        private val CHROME_INTERPOLATOR = PathInterpolator(0.2f, 0.88f, 0.3f, 1f)

        @Volatile
        private var currentOverlay: MediaViewerOverlay? = null

        fun isShowing(activity: Activity? = null): Boolean =
            currentOverlay?.let {
                !it.hasDismissed &&
                    it.dialog.isShowing &&
                    (activity == null || it.activity === activity)
            } == true

        fun showConversation(
            activity: FragmentActivity,
            conversationId: String,
            messageId: String,
            initialItem: MessageItem?,
            mediaSource: MediaSource,
            initialThumb: MediaThumbLocation?,
            thumbProvider: MediaThumbProvider? = null,
            onShowInChat: ((String) -> Unit)? = null,
        ) {
            if (activity.isFinishing || activity.isDestroyed) return
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            currentOverlay?.cleanupAndDismissImmediate()
            val overlay =
                MediaViewerOverlay(
                    activity = activity,
                    mode = Mode.Conversation(conversationId, messageId, initialItem, mediaSource),
                    initialThumb = initialThumb,
                    thumbProvider = thumbProvider,
                    onShowInChat = onShowInChat,
                )
            currentOverlay = overlay
            overlay.show()
        }

        fun showTranscript(
            activity: FragmentActivity,
            transcriptId: String,
            messageId: String,
            initialThumb: MediaThumbLocation?,
            thumbProvider: MediaThumbProvider? = null,
        ) {
            if (activity.isFinishing || activity.isDestroyed) return
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            currentOverlay?.cleanupAndDismissImmediate()
            val overlay =
                MediaViewerOverlay(
                    activity = activity,
                    mode = Mode.Transcript(transcriptId, messageId),
                    initialThumb = initialThumb,
                    thumbProvider = thumbProvider,
                    onShowInChat = null,
                )
            currentOverlay = overlay
            overlay.show()
        }
    }
}
