package one.mixin.android.ui.media.pager

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.TextureView
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.core.net.toUri
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.Player
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.math.min
import kotlinx.android.synthetic.main.activity_media_pager.*
import kotlinx.android.synthetic.main.item_pager_video_layout.view.*
import kotlinx.android.synthetic.main.layout_player_view.view.*
import kotlinx.android.synthetic.main.view_drag_image_bottom.view.*
import kotlinx.android.synthetic.main.view_drag_video_bottom.view.*
import kotlinx.android.synthetic.main.view_drag_video_bottom.view.cancel
import kotlinx.android.synthetic.main.view_player_control.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createPngTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.isAutoRotate
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.supportsPie
import one.mixin.android.extension.toast
import one.mixin.android.ui.PipVideoView
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.media.SharedMediaViewModel
import one.mixin.android.ui.url.openUrl
import one.mixin.android.util.AnimationProperties
import one.mixin.android.util.SensorOrientationChangeNotifier
import one.mixin.android.util.Session
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.XiaomiUtilities
import one.mixin.android.vo.FixedMessageDataSource
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isMedia
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.loadVideoOrLive
import one.mixin.android.vo.saveToLocal
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PhotoView.DismissFrameLayout
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import timber.log.Timber

class MediaPagerActivity : BaseActivity(), DismissFrameLayout.OnDismissListener, SensorOrientationChangeNotifier.Listener {
    private lateinit var colorDrawable: ColorDrawable

    private val conversationId by lazy {
        intent.getStringExtra(CONVERSATION_ID)
    }
    private val messageId by lazy {
        intent.getStringExtra(MESSAGE_ID)
    }
    private val excludeLive by lazy {
        intent.getBooleanExtra(EXCLUDE_LIVE, false)
    }
    private val ratio by lazy {
        intent.getFloatExtra(RATIO, 0f)
    }

    private var initialIndex: Int = 0
    private var firstLoad = true

    private val pipVideoView by lazy {
        PipVideoView.getInstance()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: SharedMediaViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SharedMediaViewModel::class.java)
    }

    private val adapter: MediaPagerAdapter by lazy {
        MediaPagerAdapter(this, this@MediaPagerActivity, mediaPagerAdapterListener)
    }

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Photo
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Photo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ratio == 0f) {
            postponeEnterTransition()
        }
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.sharedElementEnterTransition.duration = SHARED_ELEMENT_TRANSITION_DURATION
        window.sharedElementExitTransition.duration = SHARED_ELEMENT_TRANSITION_DURATION
        setContentView(R.layout.activity_media_pager)
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        supportsPie {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        SystemUIManager.setSystemUiColor(window, Color.BLACK)
        SystemUIManager.lightUI(window, false)

        colorDrawable = ColorDrawable(Color.BLACK)
        view_pager.backgroundDrawable = colorDrawable
        view_pager.adapter = adapter
        view_pager.registerOnPageChangeCallback(onPageChangeCallback)
        VideoPlayer.player().setCycle(false)

        lock_tv.setOnClickListener(onLockClickListener)

        SensorOrientationChangeNotifier.init(this, requestedOrientation)

        loadData()
    }

    override fun onResume() {
        super.onResume()
        SensorOrientationChangeNotifier.resume()
    }

    override fun onPause() {
        super.onPause()
        SensorOrientationChangeNotifier.pause()
    }

    override fun onStop() {
        super.onStop()
        lock_tv.removeCallbacks(hideLockRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        SensorOrientationChangeNotifier.reset()
        view_pager?.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    override fun onOrientationChange(oldOrientation: Int, newOrientation: Int) {
        if (!isAutoRotate()) return

        showLock()

        if (isLocked) return

        changeOrientation(newOrientation)
    }

    @Synchronized
    private fun changeOrientation(orientation: Int) {
        requestedOrientation = when (orientation) {
            270 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            90 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        findViewPagerChildByTag {
            it.player_view?.switchFullscreen(orientation == 90 || orientation == 270)
            (it as DismissFrameLayout).resetChildren()
        }
    }

    private fun loadData() = lifecycleScope.launch {
        val messageItem = viewModel.getMediaMessage(conversationId, messageId)
        val pagedConfig = PagedList.Config.Builder()
            .setInitialLoadSizeHint(1)
            .setPageSize(1)
            .build()
        val pagedList = PagedList.Builder<Int, MessageItem>(
            FixedMessageDataSource(listOf(messageItem)), pagedConfig
        ).setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())
            .setFetchExecutor(ArchTaskExecutor.getIOThreadExecutor())
            .build()
        adapter.initialPos = initialIndex
        adapter.submitList(pagedList) {
            observeAllDataSource()
        }
        if (messageItem.isVideo() || messageItem.isLive()) {
            checkPip()
            messageItem.loadVideoOrLive {
                VideoPlayer.player().start()
            }
        }
    }

    private fun observeAllDataSource() = lifecycleScope.launch {
        initialIndex = viewModel.indexMediaMessages(conversationId, messageId, excludeLive)
        viewModel.getMediaMessages(conversationId, initialIndex, excludeLive)
            .observe(this@MediaPagerActivity, Observer {
                adapter.submitList(it) {
                    if (firstLoad) {
                        adapter.initialPos = initialIndex
                        view_pager.setCurrentItem(initialIndex, false)
                        checkOrientation()
                        firstLoad = false
                    }
                }
            })
    }

    private fun checkPip() {
        if (pipVideoView.shown) {
            pipVideoView.close(messageId != VideoPlayer.player().mId)
        }
    }

    private fun showVideoBottom(messageItem: MessageItem) {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_drag_video_bottom,
            null
        )
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.save_video.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe({ granted ->
                    if (granted) {
                        messageItem.saveToLocal(this@MediaPagerActivity)
                    } else {
                        openPermissionSetting()
                    }
                }, {
                    toast(R.string.save_failure)
                })
            bottomSheet.dismiss()
        }
        view.share.setOnClickListener {
            messageItem.mediaUrl?.let {
                shareMedia(true, it)
            }
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun showImageBottom(item: MessageItem, pagerItemView: View) {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_drag_image_bottom,
            null
        )
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.save.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe({ granted ->
                    if (granted) {
                        doAsync {
                            val path = item.mediaUrl?.toUri()?.getFilePath()
                            if (path == null) {
                                toast(R.string.save_failure)
                                return@doAsync
                            }
                            val file = File(path)
                            val outFile = when {
                                item.mediaMimeType.equals(
                                    MimeType.GIF.toString(),
                                    true
                                ) -> this@MediaPagerActivity.getPublicPicturePath().createGifTemp(
                                    false
                                )
                                item.mediaMimeType.equals(MimeType.PNG.toString()) ->
                                    this@MediaPagerActivity.getPublicPicturePath().createPngTemp(
                                        false
                                    )
                                else -> this@MediaPagerActivity.getPublicPicturePath().createImageTemp(
                                    noMedia = false
                                )
                            }
                            outFile.copyFromInputStream(FileInputStream(file))
                            sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(outFile)
                                )
                            )
                            uiThread { toast(getString(R.string.save_to, outFile.absolutePath)) }
                        }
                    } else {
                        openPermissionSetting()
                    }
                }, {
                    toast(R.string.save_failure)
                })
            bottomSheet.dismiss()
        }
        view.share_image.setOnClickListener {
            item.mediaUrl?.let {
                shareMedia(false, it)
            }
            bottomSheet.dismiss()
        }
        view.decode.setOnClickListener {
            decodeQRCode(pagerItemView as ViewGroup)
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    @Suppress("DEPRECATION")
    private fun decodeQRCode(viewGroup: ViewGroup) {
        val imageView = viewGroup.getChildAt(0)
        val bitmap = if (imageView is ImageView) {
            val bitmapDrawable = imageView.drawable as? BitmapDrawable
            if (bitmapDrawable == null) {
                toast(R.string.can_not_recognize)
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
            if (isGooglePlayServicesAvailable()) {
                var url: String? = null
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                val detector = FirebaseVision.getInstance().visionBarcodeDetector
                detector.detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        url = barcodes.firstOrNull()?.rawValue
                        if (url != null) {
                            openUrl(url!!, supportFragmentManager) {
                                QrScanBottomSheetDialogFragment.newInstance(url!!)
                                    .showNow(
                                        supportFragmentManager,
                                        QrScanBottomSheetDialogFragment.TAG
                                    )
                            }
                        }
                    }
                    .addOnFailureListener {
                        toast(R.string.can_not_recognize)
                    }
                    .addOnCompleteListener {
                        if (url == null) {
                            decodeWithZxing(imageView, bitmap)
                        } else {
                            if (imageView !is ImageView) {
                                imageView.isDrawingCacheEnabled = false
                            }
                        }
                    }
            } else {
                decodeWithZxing(imageView, bitmap)
            }
        } else {
            toast(R.string.can_not_recognize)
            if (imageView !is ImageView) {
                imageView.isDrawingCacheEnabled = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeWithZxing(imageView: View, bitmap: Bitmap) = lifecycleScope.launch {
        val url = withContext(Dispatchers.IO) {
            bitmap.decodeQR()
        }
        if (imageView !is ImageView) {
            imageView.isDrawingCacheEnabled = false
        }
        if (url != null) {
            openUrl(url, supportFragmentManager) {
                QrScanBottomSheetDialogFragment.newInstance(url)
                    .showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
            }
        } else {
            toast(R.string.can_not_recognize)
        }
    }

    private fun shareMedia(isVideo: Boolean, url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            var uri = Uri.parse(url)
            if (ContentResolver.SCHEME_FILE == uri.scheme) {
                val path = uri.getFilePath(this@MediaPagerActivity)
                if (path == null) {
                    toast(R.string.error_file_exists)
                    return
                }
                uri = getUriForFile(File(path))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            type = if (isVideo) "video/*" else "image/*"
        }
        val name =
            getString(if (isVideo) R.string.conversation_status_video else R.string.conversation_status_pic)
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_to, name)))
    }

    private var pipAnimationInProgress = false
    private fun switchToPip(messageItem: MessageItem, view: View) {
        if (!checkInlinePermissions() || pipAnimationInProgress) {
            return
        }
        pipAnimationInProgress = true
        findViewPagerChildByTag { windowView ->
            val videoAspectRatioLayout =
                windowView.player_view.video_aspect_ratio
            val rect = PipVideoView.getPipRect(videoAspectRatioLayout.aspectRatio)
            val isLandscape = isLandscape()
            if (isLandscape) {
                val screenHeight = realSize().y
                if (rect.width > screenHeight) {
                    val ratio = rect.width / rect.height
                    rect.height = screenHeight / ratio
                    rect.width = screenHeight.toFloat()
                }
            }
            val width = if (isLandscape) windowView.height else windowView.width
            val scale = (if (isLandscape) rect.height else rect.width) / width
            val animatorSet = AnimatorSet()
            val position = IntArray(2)
            videoAspectRatioLayout.getLocationOnScreen(position)
            window.decorView.systemUiVisibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            val changedTextureView = pipVideoView.show(
                this, videoAspectRatioLayout.aspectRatio, videoAspectRatioLayout.videoRotation,
                conversationId, messageItem.messageId, messageItem.isVideo(), excludeLive, messageItem.mediaUrl
            )

            val videoTexture = view.findViewById<TextureView>(R.id.video_texture)
            animatorSet.playTogether(
                ObjectAnimator.ofInt(colorDrawable, AnimationProperties.COLOR_DRAWABLE_ALPHA, 0),
                ObjectAnimator.ofFloat(videoTexture, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(videoTexture, View.SCALE_Y, scale),
                ObjectAnimator.ofFloat(
                    videoAspectRatioLayout,
                    View.TRANSLATION_X,
                    rect.x - videoAspectRatioLayout.x -
                        this.realSize().x * (1f - scale) / 2
                ),
                ObjectAnimator.ofFloat(
                    videoAspectRatioLayout,
                    View.TRANSLATION_Y,
                    rect.y - videoAspectRatioLayout.y +
                        this.statusBarHeight() - (videoAspectRatioLayout.height - rect.height) / 2
                )
            )
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.duration = 250
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    windowView.pip_iv.fadeOut()
                    windowView.close_iv.fadeOut()
                    if (windowView.live_tv.isEnabled) {
                        windowView.live_tv.fadeOut()
                    }
                    if (!SystemUIManager.hasCutOut(window)) {
                        SystemUIManager.clearStyle(window)
                    }
                }

                override fun onAnimationEnd(animation: Animator?) {
                    pipAnimationInProgress = false
                    if (messageItem.isVideo() && VideoPlayer.player().player.playbackState == Player.STATE_IDLE) {
                        VideoPlayer.player()
                            .loadVideo(messageItem.mediaUrl!!, messageItem.messageId, true)
                        VideoPlayer.player().setVideoTextureView(changedTextureView)
                        VideoPlayer.player().pause()
                    } else {
                        VideoPlayer.player().setVideoTextureView(changedTextureView)
                    }
                    dismiss()
                }
            })
            animatorSet.start()
        }
    }

    private fun dismiss() {
        view_pager.visibility = View.INVISIBLE
        overridePendingTransition(0, 0)
        super.finish()
    }

    private fun checkInlinePermissions(): Boolean {
        if (XiaomiUtilities.isMIUI() && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
            var intent = XiaomiUtilities.getPermissionManagerIntent()
            if (intent != null) {
                try {
                    startActivity(intent)
                } catch (x: Exception) {
                    try {
                        intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data =
                            Uri.parse("package:" + MixinApplication.appContext.packageName)
                        startActivity(intent)
                    } catch (xx: Exception) {
                        Timber.e(xx)
                    }
                }
            }
            toast(R.string.need_background_permission)
            return false
        }
        if (Settings.canDrawOverlays(this)) {
            return true
        } else {
            this.let { activity ->
                AlertDialog.Builder(activity)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.live_permission)
                    .setPositiveButton(R.string.live_setting) { _, _ ->
                        try {
                            activity.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + activity.packageName)
                                )
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }.show()
            }
        }
        return false
    }

    private fun setStartPostTransition(sharedView: View) {
        sharedView.doOnPreDraw { startPostponedEnterTransition() }
    }

    private fun downloadMedia(position: Int): Boolean {
        val currMessageItem = getMessageItemByPosition(position) ?: return false
        if (currMessageItem.mediaStatus == MediaStatus.CANCELED.name) return false
        if (currMessageItem.isMedia() && currMessageItem.mediaUrl == null) {
            viewModel.downloadByMessageId(currMessageItem.messageId)
            return true
        }
        return false
    }

    private fun getMessageItemByPosition(position: Int): MessageItem? =
        try {
            adapter.currentList?.get(position)
        } catch (e: IndexOutOfBoundsException) {
            null
        }

    private inline fun findViewPagerChildByTag(
        pos: Int = view_pager.currentItem,
        crossinline action: (v: ViewGroup) -> Unit
    ) {
        if (isFinishing) return
        val id = getMessageItemByPosition(pos)?.messageId ?: return
        val v = view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX$id")
        if (v != null) {
            action(v as ViewGroup)
        }
    }

    private fun loadVideoMessage(messageItem: MessageItem) {
        if (messageItem.isVideo() || messageItem.isLive()) {
            messageItem.loadVideoOrLive {
                val view =
                    view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX${messageItem.messageId}")
                if (view != null) {
                    view.player_view.player = VideoPlayer.player().player
                }
            }
        }
    }

    private var isLocked = false

    private fun showLock() {
        lock_tv.isVisible = true
        lock_tv.removeCallbacks(hideLockRunnable)
        lock_tv.postDelayed(hideLockRunnable, 3000)
    }

    private fun checkOrientation() {
        if (isAutoRotate() && !isLocked) {
            val sensorIsLandscape = SensorOrientationChangeNotifier.isLandscape()
            val activityLandscape = isLandscape()
            if (sensorIsLandscape != activityLandscape) {
                changeOrientation(SensorOrientationChangeNotifier.orientation)
            }
        }
    }

    private val hideLockRunnable = Runnable {
        lock_tv.isVisible = false
    }

    private val onLockClickListener = View.OnClickListener {
        isLocked = !isLocked
        if (isLocked) {
            lock_tv.text = getString(R.string.click_unlock)
            lock_tv.textColor = getColor(R.color.colorAccent)
        } else {
            lock_tv.text = getString(R.string.click_lock)
            lock_tv.textColor = getColor(R.color.white)
        }
        lock_tv.removeCallbacks(hideLockRunnable)
        lock_tv.postDelayed(hideLockRunnable, 3000)
        checkOrientation()
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            lock_tv.removeCallbacks(hideLockRunnable)
            lock_tv.post(hideLockRunnable)

            if (downloadMedia(position)) return

            val messageItem = getMessageItemByPosition(position) ?: return
            if (VideoPlayer.player().mId != messageItem.messageId && !pipVideoView.shown) {
                VideoPlayer.player().stop()
                VideoPlayer.player().pause()
            }
            if (messageItem.isVideo() || messageItem.isLive()) {
                checkPip()
            }
            loadVideoMessage(messageItem)
        }
    }

    private var inDismissState = false
    private var controllerVisibleBeforeDismiss = false

    override fun onDismissProgress(progress: Float) {
        if (progress > 0 && !inDismissState) {
            inDismissState = true
            val messageItem = getMessageItemByPosition(view_pager.currentItem) ?: return
            if (messageItem.isLive() || messageItem.isVideo()) {
                findViewPagerChildByTag {
                    val playerView = it.player_view
                    if (playerView != null) {
                        controllerVisibleBeforeDismiss = playerView.useController && playerView.player_control_view.isVisible
                        playerView.hideController()
                    }
                }
            }
        }
        colorDrawable.alpha = min(ALPHA_MAX, ((1 - progress) * ALPHA_MAX).toInt())
    }

    override fun onDismiss() {
        inDismissState = false
        finishAfterTransition()
    }

    override fun onCancel() {
        if (inDismissState) {
            inDismissState = false
            val messageItem = getMessageItemByPosition(view_pager.currentItem) ?: return
            if (messageItem.isLive() || messageItem.isVideo()) {
                findViewPagerChildByTag {
                    val playerView = it.player_view
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
        colorDrawable.alpha = ALPHA_MAX
    }

    override fun finishAfterTransition() {
        findViewPagerChildByTag {
            it.player_view?.hideController()
        }
        super.finishAfterTransition()
    }

    override fun finish() {
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        if (!pipVideoView.shown) {
            VideoPlayer.destroy()
        }
        super.finish()
        overridePendingTransition(0, R.anim.scale_out)
    }

    private val mediaPagerAdapterListener = object : MediaPagerAdapterListener {
        override fun onClick(messageItem: MessageItem) {
            finishAfterTransition()
        }

        override fun onLongClick(messageItem: MessageItem, view: View) {
            if (messageItem.isImage()) {
                showImageBottom(messageItem, view)
            } else if (messageItem.isVideo()) {
                showVideoBottom(messageItem)
            }
        }

        override fun onCircleProgressClick(messageItem: MessageItem) {
            when (messageItem.mediaStatus) {
                MediaStatus.CANCELED.name -> {
                    if (Session.getAccountId() == messageItem.userId) {
                        viewModel.retryUpload(messageItem.messageId) {
                            toast(R.string.error_retry_upload)
                        }
                    } else {
                        viewModel.retryDownload(messageItem.messageId)
                    }
                }
                MediaStatus.PENDING.name -> {
                    viewModel.cancel(messageItem.messageId)
                }
            }
        }

        override fun onReadyPostTransition(view: View) {
            setStartPostTransition(view)
        }

        override fun switchToPin(messageItem: MessageItem, view: View) {
            switchToPip(messageItem, view)
        }

        override fun finishAfterTransition() {
            this@MediaPagerActivity.finishAfterTransition()
        }

        override fun switchFullscreen() {
            val isLandscape = this@MediaPagerActivity.isLandscape()
            val orientation = if (isLandscape) 0 else 270
            this@MediaPagerActivity.changeOrientation(orientation)
        }
    }

    companion object {
        private const val MESSAGE_ID = "id"
        private const val RATIO = "ratio"
        private const val CONVERSATION_ID = "conversation_id"
        private const val EXCLUDE_LIVE = "exclude_live"
        private const val ALPHA_MAX = 0xFF
        const val PREFIX = "media"
        const val PAGE_SIZE = 3

        private const val SHARED_ELEMENT_TRANSITION_DURATION = 200L

        fun show(
            activity: Activity,
            imageView: View,
            conversationId: String,
            messageId: String,
            excludeLive: Boolean = false
        ) {
            val intent = Intent(activity, MediaPagerActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(MESSAGE_ID, messageId)
                putExtra(EXCLUDE_LIVE, excludeLive)
            }
            activity.startActivity(
                intent, ActivityOptions.makeSceneTransitionAnimation(
                    activity, imageView,
                    "transition"
                ).toBundle()
            )
        }

        fun show(
            context: Context,
            conversationId: String,
            messageId: String,
            ratio: Float,
            excludeLive: Boolean = false
        ) {
            val intent = Intent(context, MediaPagerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(MESSAGE_ID, messageId)
                putExtra(RATIO, ratio)
                putExtra(EXCLUDE_LIVE, excludeLive)
            }
            context.startActivity(intent)
        }
    }
}
