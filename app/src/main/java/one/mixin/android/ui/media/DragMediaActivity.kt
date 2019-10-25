package one.mixin.android.ui.media

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.systemuidemo.SystemUIManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.shizhefei.view.largeimage.LargeImageView
import com.shizhefei.view.largeimage.factory.FileBitmapDecoderFactory
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min
import kotlinx.android.synthetic.main.activity_drag_media.*
import kotlinx.android.synthetic.main.item_video_layout.view.*
import kotlinx.android.synthetic.main.view_drag_image_bottom.view.*
import kotlinx.android.synthetic.main.view_drag_image_bottom.view.cancel
import kotlinx.android.synthetic.main.view_drag_video_bottom.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.belowOreo
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createPngTemp
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.displayRatio
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.inflate
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.supportsPie
import one.mixin.android.extension.toast
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.PipVideoView
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.url.openUrl
import one.mixin.android.util.AnimationProperties
import one.mixin.android.util.Session
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.XiaomiUtilities
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isMedia
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.saveToLocal
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CircleProgress
import one.mixin.android.widget.PhotoView.DismissFrameLayout
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.PlayView.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView.Companion.STATUS_LOADING
import one.mixin.android.widget.PlayView.Companion.STATUS_PLAYING
import one.mixin.android.widget.PlayView.Companion.STATUS_REFRESH
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber

@SuppressLint("InvalidWakeLockTag")
class DragMediaActivity : BaseActivity(), DismissFrameLayout.OnDismissListener {
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
    private var lastPos: Int = -1
    private val pagerAdapter by lazy {
        MediaAdapter(this@DragMediaActivity)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: SharedMediaViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SharedMediaViewModel::class.java)
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository
    @Inject
    lateinit var jobManager: MixinJobManager

    private val powerManager: PowerManager by lazy {
        applicationContext.getSystemService<PowerManager>()!!
    }

    private val aodWakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "mixin:aod"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ratio == 0f) {
            postponeEnterTransition()
        }
        if (pipVideoView.shown) {
            pipVideoView.close(messageId != VideoPlayer.player().mId)
        }
        super.onCreate(savedInstanceState)
        VideoPlayer.player().setOnMediaPlayerListener(mediaListener)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_drag_media)
        colorDrawable = ColorDrawable(Color.BLACK)
        view_pager.backgroundDrawable = colorDrawable
        lifecycleScope.launch {
            initialIndex =
                conversationRepository.indexMediaMessages(conversationId, messageId, excludeLive)
            conversationRepository.getMediaMessages(conversationId, initialIndex, excludeLive)
                .observe(this@DragMediaActivity, Observer { list ->
                    var needReload = false
                    pagerAdapter.visiblePositions.forEach { entry ->
                        val oldMessageItem = entry.value
                        val newMessageItem = list[entry.key]
                        if (oldMessageItem?.messageId != newMessageItem?.messageId ||
                            oldMessageItem?.mediaStatus != newMessageItem?.mediaStatus) {
                            needReload = true
                            return@forEach
                        }
                    }
                    pagerAdapter.submitAction = {
                        if (firstLoad) {
                            firstLoad = false
                            view_pager.currentItem = initialIndex
                            lastPos = initialIndex
                            play(initialIndex)
                        }
                    }
                    if (firstLoad || needReload) {
                        if (view_pager.adapter == null) {
                            view_pager.adapter = pagerAdapter
                        }
                        pagerAdapter.submitList(list)
                    }
                })
        }

        view_pager.addOnPageChangeListener(pageListener)
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        supportsPie {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        SystemUIManager.setSystemUiColor(window, Color.BLACK)
        SystemUIManager.lightUI(window, false)
    }

    override fun onPause() {
        super.onPause()
        if (!pipVideoView.shown) {
            pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoPlayer.player().setOnVideoPlayerListener(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!pipVideoView.shown) {
            VideoPlayer.destroy()
        }
    }

    private fun showVideoBottom() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_drag_video_bottom,
            null
        )
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.save_video.setOnClickListener {
            val messageItem = pagerAdapter.getItem(view_pager.currentItem)
            if (messageItem == null) {
                toast(R.string.save_failure)
            } else {
                RxPermissions(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe({ granted ->
                        if (granted) {
                            messageItem.saveToLocal(this@DragMediaActivity)
                        } else {
                            openPermissionSetting()
                        }
                    }, {
                        toast(R.string.save_failure)
                    })
            }
            bottomSheet.dismiss()
        }
        view.share.setOnClickListener {
            shareMedia(true)
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun showImageBottom() {
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
                            pagerAdapter.getItem(view_pager.currentItem)?.let { item ->
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
                                    ) -> this@DragMediaActivity.getPublicPicturePath().createGifTemp(
                                        false
                                    )
                                    item.mediaMimeType.equals(MimeType.PNG.toString()) -> this@DragMediaActivity.getPublicPicturePath().createPngTemp(
                                        false
                                    )
                                    else -> this@DragMediaActivity.getPublicPicturePath().createImageTemp(
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
                                uiThread { toast(R.string.save_success) }
                            }
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
            shareMedia(false)
            bottomSheet.dismiss()
        }
        view.decode.setOnClickListener {
            findViewPagerChildByTag { viewGroup ->
                decodeQRCode(viewGroup)
            }
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
                        } else {
                            toast(R.string.can_not_recognize)
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

    private fun shareMedia(isVideo: Boolean) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            val url = pagerAdapter.getItem(view_pager.currentItem)?.mediaUrl
            var uri = Uri.parse(url)
            if (ContentResolver.SCHEME_FILE == uri.scheme) {
                val path = uri.getFilePath(this@DragMediaActivity)
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

    private var setTransition = false

    inner class MediaAdapter(
        private val onDismissListener: DismissFrameLayout.OnDismissListener
    ) : PagedListPagerAdapter<MessageItem>(), TextureView.SurfaceTextureListener {
        override fun createItem(container: ViewGroup, position: Int): Any {
            val messageItem = getItem(position) ?: return DismissFrameLayout(container.context)
            val layout = DismissFrameLayout(container.context)
            val circleProgress = layout.inflate(R.layout.view_circle_progress) as CircleProgress
            circleProgress.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.CENTER
            }
            val innerView = if (messageItem.type == MessageCategory.SIGNAL_IMAGE.name ||
                messageItem.type == MessageCategory.PLAIN_IMAGE.name
            ) {
                if (!messageItem.mediaMimeType.equals(
                        MimeType.GIF.toString(),
                        true
                    ) && messageItem.mediaHeight!! / messageItem.mediaWidth!!.toFloat() > displayRatio() * 1.5f
                ) {
                    createLargeImageView(container, position, messageItem, circleProgress)
                } else {
                    createPhotoView(container, position, messageItem, circleProgress)
                }
            } else {
                createVideoView(container, position, messageItem, circleProgress)
            }
            layout.setDismissListener(onDismissListener)
            layout.layoutParams = ViewPager.LayoutParams()
            layout.addView(innerView)
            layout.addView(circleProgress)
            layout.tag = "$PREFIX${messageItem.messageId}"
            container.addView(layout)
            return layout
        }

        override fun removeItem(container: ViewGroup, position: Int, obj: Any) {
            if (obj is View) {
                obj.tag?.let {
                    if (it is Disposable && !it.isDisposed) {
                        it.dispose()
                    }
                }
            }
            container.removeView(obj as View)
        }

        private fun createVideoView(
            container: ViewGroup,
            position: Int,
            messageItem: MessageItem,
            circleProgress: CircleProgress
        ): View {
            val view = View.inflate(container.context, R.layout.item_video_layout, null)
            view.controller.setOnTouchListener(View.OnTouchListener { v, event ->
                val seekRect = Rect()
                v.seek_bar.getHitRect(seekRect)
                if (event.y >= (seekRect.top - dpToPx(16f)) && event.y <= (seekRect.bottom + dpToPx(
                        16f
                    )) &&
                    event.x >= seekRect.left && event.x <= seekRect.right
                ) {
                    val y = seekRect.top + seekRect.height() / 2f
                    var x = event.x - seekRect.left
                    if (x < 0) {
                        x = 0f
                    } else if (x > seekRect.width()) {
                        x = seekRect.width().toFloat()
                    }
                    val me = MotionEvent.obtain(
                        event.downTime, event.eventTime,
                        event.action, x, y, event.metaState
                    )
                    return@OnTouchListener v.seek_bar.onTouchEvent(me)
                }
                return@OnTouchListener false
            })

            val ratio = messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat()
            setSize(ratio, view)
            view.close_iv.setOnClickListener { finishAfterTransition() }
            view.pip_iv.setOnClickListener {
                view.play_view.isVisible = false
                switchToPip()
            }
            view.pip_iv.isEnabled = false
            view.pip_iv.alpha = 0.5f
            view.close_iv.post {
                val statusBarHeight = statusBarHeight()
                view.action_bar.setPadding(0, statusBarHeight, 0, 0)
            }
            view.video_texture.surfaceTextureListener = this

            if (messageItem.isLive()) {
                circleProgress.isVisible = false
                view.play_view.isVisible = true
                view.preview_iv.loadImage(messageItem.thumbUrl, messageItem.thumbImage)
            } else {
                if (messageItem.mediaUrl != null) {
                    view.preview_iv.loadVideo(messageItem.mediaUrl)
                } else {
                    val imageData = messageItem.thumbImage?.decodeBase64()
                    Glide.with(view).load(imageData).into(view.preview_iv)
                }
                view.seek_bar.progress = 0
                view.duration_tv.text = 0L.formatMillis()
                view.remain_tv.text = messageItem.mediaDuration?.toLong()?.formatMillis()
                if (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name) {
                    circleProgress.isVisible = false
                    view.play_view.isVisible = true
                    circleProgress.setBindId(messageItem.messageId)
                } else {
                    view.play_view.isVisible = false
                    circleProgress.isVisible = true
                    circleProgress.setBindId(messageItem.messageId)
                    if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
                        circleProgress.enableLoading()
                    } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
                        if (Session.getAccountId() == messageItem.userId) {
                            circleProgress.enableUpload()
                        } else {
                            circleProgress.enableDownload()
                        }
                    } else {
                        // TODO expired
                    }
                    circleProgress.setOnClickListener { handleCircleProgressClick(messageItem) }
                }
            }
            view.tag = messageItem.isLive()
            if (VideoPlayer.player().mId == messageItem.messageId) {
                val playbackState = VideoPlayer.player()
                    .player.playbackState
                view.play_view.status = when (playbackState) {
                    STATE_IDLE, STATE_ENDED -> STATUS_IDLE
                    STATE_BUFFERING -> STATUS_LOADING
                    else -> {
                        if (VideoPlayer.player().isPlaying()) {
                            STATUS_PLAYING
                        } else {
                            STATUS_IDLE
                        }
                    }
                }
            }
            if (position == initialIndex && !setTransition) {
                setTransition = true
                ViewCompat.setTransitionName(view, "transition")
                setStartPostTransition(view)
            }

            view.play_view.setOnClickListener {
                when (view.play_view.status) {
                    STATUS_IDLE -> {
                        setPreviewIv(false, view_pager.currentItem)
                        play(view_pager.currentItem)
                    }
                    STATUS_LOADING, STATUS_PLAYING -> {
                        pause()
                    }
                    STATUS_REFRESH -> {
                        load(position, force = true) {
                            if (messageItem.isVideo()) {
                                startListenDuration(view)
                            }
                        }
                    }
                }
            }
            view.setOnClickListener {
                onVideoClick(view, messageItem)
            }
            view.setOnLongClickListener {
                onVideoLongClick(messageItem)
                return@setOnLongClickListener true
            }
            view.video_texture.setOnClickListener {
                onVideoClick(view, messageItem)
            }
            view.video_texture.setOnLongClickListener {
                onVideoLongClick(messageItem)
                return@setOnLongClickListener true
            }

            var isPlaying = false
            view.seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isPlaying = VideoPlayer.player()
                        .isPlaying()
                    VideoPlayer.player().pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isPlaying) {
                        VideoPlayer.player().start()
                    }
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        VideoPlayer.player()
                            .seekTo(progress * VideoPlayer.player().duration() / 200)
                    }
                }
            })

            return view
        }

        private fun onVideoClick(view: View, messageItem: MessageItem) {
            if (view.close_iv.isVisible) {
                fadeOut(view, messageItem.isLive())
            } else {
                fadeIn(view, messageItem.isLive())
            }
        }

        private fun onVideoLongClick(messageItem: MessageItem) {
            if (messageItem.isVideo()) {
                showVideoBottom()
            }
        }

        private fun setSize(rotio: Float, view: View) {
            val w = applicationContext.realSize().x
            val h = applicationContext.realSize().y
            val ratioParams = view.video_aspect_ratio.layoutParams
            val previewParams = view.preview_iv.layoutParams
            if (rotio >= 1f) {
                val scaleH = (w / rotio).toInt()
                if (scaleH > h) {
                    ratioParams.width = (h * ratio).toInt()
                    ratioParams.height = h
                } else {
                    ratioParams.width = w
                    ratioParams.height = scaleH
                }
            } else {
                val scaleW = (h * rotio).toInt()
                if (scaleW > w) {
                    ratioParams.width = w
                    ratioParams.height = (w / rotio).toInt()
                } else {
                    ratioParams.width = scaleW
                    ratioParams.height = h
                }
            }
            previewParams.width = ratioParams.width
            previewParams.height = ratioParams.height
            view.video_aspect_ratio.layoutParams = ratioParams
            view.preview_iv.layoutParams = previewParams
        }

        private fun createLargeImageView(
            container: ViewGroup,
            position: Int,
            messageItem: MessageItem,
            circleProgress: CircleProgress
        ): LargeImageView {
            val imageView = LargeImageView(container.context)
            if (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name) {
                circleProgress.isVisible = false
                circleProgress.setBindId(messageItem.messageId)
                imageView.setImage(FileBitmapDecoderFactory(File(messageItem.mediaUrl?.getFilePath())))
            } else {
                val imageData = Base64.decode(messageItem.thumbImage, Base64.DEFAULT)
                imageView.setImage(BitmapFactory.decodeByteArray(imageData, 0, imageData.size))
                circleProgress.isVisible = true
                circleProgress.setBindId(messageItem.messageId)
                if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
                    circleProgress.enableLoading()
                } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
                    if (Session.getAccountId() == messageItem.userId) {
                        circleProgress.enableUpload()
                    } else {
                        circleProgress.enableDownload()
                    }
                } else {
                    // TODO expired
                }
                circleProgress.setOnClickListener { handleCircleProgressClick(messageItem) }
            }
            if (messageItem.mediaWidth!! < screenWidth()) {
                imageView.scale = (screenWidth().toFloat() / messageItem.mediaWidth)
            }
            if (position == initialIndex) {
                ViewCompat.setTransitionName(imageView, "transition")
                setStartPostTransition(imageView)
            }
            imageView.setOnClickListener {
                finishAfterTransition()
            }
            imageView.setOnLongClickListener {
                showImageBottom()
                return@setOnLongClickListener true
            }
            return imageView
        }

        private fun createPhotoView(
            container: ViewGroup,
            position: Int,
            messageItem: MessageItem,
            circleProgress: CircleProgress
        ): PhotoView {
            val imageView = PhotoView(container.context)
            imageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (messageItem.mediaMimeType.equals(MimeType.GIF.toString(), true)) {
                imageView.loadGif(messageItem.mediaUrl, object : RequestListener<GifDrawable?> {
                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (position == initialIndex) {
                            ViewCompat.setTransitionName(imageView, "transition")
                            setStartPostTransition(imageView)
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }, base64Holder = messageItem.thumbImage)
            } else {
                imageView.loadImage(messageItem.mediaUrl, object : RequestListener<Drawable?> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (position == initialIndex) {
                            ViewCompat.setTransitionName(imageView, "transition")
                            setStartPostTransition(imageView)
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }, base64Holder = messageItem.thumbImage)
            }
            if (messageItem.mediaStatus == MediaStatus.DONE.name || messageItem.mediaStatus == MediaStatus.READ.name) {
                circleProgress.isVisible = false
                circleProgress.setBindId(messageItem.messageId)
            } else {
                circleProgress.isVisible = true
                circleProgress.setBindId(messageItem.messageId)
                if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
                    circleProgress.enableLoading()
                } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
                    if (Session.getAccountId() == messageItem.userId) {
                        circleProgress.enableUpload()
                    } else {
                        circleProgress.enableDownload()
                    }
                } else {
                    // TODO expired
                }
                circleProgress.setOnClickListener { handleCircleProgressClick(messageItem) }
            }
            imageView.setOnClickListener {
                finishAfterTransition()
            }
            imageView.setOnLongClickListener {
                showImageBottom()
                return@setOnLongClickListener true
            }
            if (position == initialIndex) {
                ViewCompat.setTransitionName(imageView, "transition")
                setStartPostTransition(imageView)
            }
            return imageView
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = false

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            setTextureView()
        }
    }

    private fun handleCircleProgressClick(messageItem: MessageItem) {
        when {
            messageItem.mediaStatus == MediaStatus.CANCELED.name -> {
                if (Session.getAccountId() == messageItem.userId) {
                    viewModel.retryUpload(messageItem.messageId) {
                        toast(R.string.error_retry_upload)
                    }
                } else {
                    viewModel.retryDownload(messageItem.messageId)
                }
            }
            messageItem.mediaStatus == MediaStatus.PENDING.name -> {
                viewModel.cancel(messageItem.messageId)
            }
        }
    }

    private fun fadeIn(view: View, live: Boolean) {
        if (live) {
            view.pip_iv.fadeIn(
                if (view.pip_iv.isEnabled) {
                    1f
                } else {
                    0.5f
                }
            )
            if (view.live_tv.isEnabled) {
                view.live_tv.fadeIn()
            }
        } else {
            view.controller.fadeIn()
            view.pip_iv.fadeIn()
        }
        if (!view.play_view.isVisible) {
            view.play_view.fadeIn()
        }
        view.close_iv.fadeIn()
        view.action_bar.fadeIn()
    }

    private fun fadeOut(view: View, live: Boolean, withoutPlay: Boolean = false) {
        if (live) {
            if (view.live_tv.isEnabled) {
                view.live_tv.fadeOut()
            }
        } else {
            view.controller.fadeOut()
        }
        if (!withoutPlay) {
            if (view.play_view.isVisible) {
                view.play_view.fadeOut()
            }
        } else {
            view.play_view.fadeIn()
        }
        view.close_iv.fadeOut()
        view.pip_iv.fadeOut()
        view.action_bar.fadeOut()
    }

    private fun setTextureView() {
        findViewPagerChildByTag {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                VideoPlayer.player().setVideoTextureView(parentView.video_texture)
            }
        }
    }

    private fun setPlayViewStatus(status: Int, pos: Int = lastPos) {
        findViewPagerChildByTag(pos) {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                if (parentView.play_view.status == STATUS_REFRESH &&
                    status == STATUS_IDLE
                ) {
                    return
                }
                parentView.play_view.status = status
            }
        }
    }

    private fun setPreviewIv(visible: Boolean, pos: Int = lastPos) {
        findViewPagerChildByTag(pos) {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                parentView.preview_iv.visibility = if (visible) VISIBLE else INVISIBLE
            }
        }
    }

    private fun handleLast() {
        findViewPagerChildByTag(lastPos) {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                fadeOut(parentView, parentView.tag as Boolean, true)
                parentView.preview_iv.visibility = VISIBLE
            }
        }
    }

    private fun setStartPostTransition(sharedView: View) {
        sharedView.doOnPreDraw { startPostponedEnterTransition() }
    }

    override fun onDismissProgress(progress: Float) {
        colorDrawable.alpha = min(ALPHA_MAX, ((1 - progress) * ALPHA_MAX).toInt())
    }

    override fun onDismiss() {
        finishAfterTransition()
    }

    override fun finishAfterTransition() {
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        if (view_pager.currentItem == initialIndex) {
            super.finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun finish() {
        ValueAnimator.ofInt(0, 100)
            .setDuration(400)
            .apply {
                addUpdateListener {
                    (it.animatedValue as Int).apply {
                        val item = pagerAdapter.getItem(view_pager.currentItem)
                            ?: return@addUpdateListener
                        val v =
                            view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX${item.messageId}")
                                ?: return@addUpdateListener
                        v.translationY = (realSize().y * this / 100).toFloat()
                        colorDrawable.alpha = ALPHA_MAX * (100 - this) / 100
                        if (it.animatedValue == 100) {
                            super.finish()
                            overridePendingTransition(R.anim.no_transition, R.anim.no_transition)
                        }
                    }
                }
            }
            .start()
    }

    override fun onCancel() {
        colorDrawable.alpha = ALPHA_MAX
    }

    private inline fun findViewPagerChildByTag(
        pos: Int = view_pager.currentItem,
        action: (v: ViewGroup) -> Unit
    ) {
        if (isFinishing) return
        val id = pagerAdapter.getItem(pos)?.messageId ?: return
        val v = view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX$id")
        if (v != null) {
            action(v as ViewGroup)
        }
    }

    private inline fun findViewPagerChildById(messageId: String, action: (v: ViewGroup) -> Unit) {
        val v = view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX$messageId")
        if (v != null) {
            action(v as ViewGroup)
        }
    }

    private fun pause() {
        setPlayViewStatus(STATUS_IDLE)
        VideoPlayer.player().pause()
    }

    private fun stop() {
        setPlayViewStatus(STATUS_IDLE)
        handleLast()
        VideoPlayer.player().stop()
    }

    private inline fun load(pos: Int, force: Boolean = false, action: () -> Unit = {}) {
        val messageItem = pagerAdapter.getItem(pos) ?: return
        if (messageItem.isVideo() || messageItem.isLive()) {
            messageItem.mediaUrl?.let {
                if (messageItem.isLive()) {
                    VideoPlayer.player()
                        .loadHlsVideo(it, messageItem.messageId, force)
                } else {
                    VideoPlayer.player()
                        .loadVideo(it, messageItem.messageId, force)
                }
            }
            setTextureView()
            action()
        }
    }

    private fun play(pos: Int) = load(pos) {
        view_pager.post {
            findViewPagerChildByTag { viewGroup ->
                val parentView = viewGroup.getChildAt(0)
                if (parentView is FrameLayout) {
                    startListenDuration(parentView)
                }
            }
        }
        VideoPlayer.player().start()
    }

    private fun startListenDuration(view: View) {
        Observable.interval(0, 100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe {
                if (VideoPlayer.player().duration() != 0) {
                    view.seek_bar.progress = (VideoPlayer.player().getCurrentPos() * 200 /
                        VideoPlayer.player().duration()).toInt()
                    view.duration_tv.text = VideoPlayer.player()
                        .getCurrentPos().formatMillis()
                    if (view.remain_tv.text.isEmpty()) { // from google photo
                        view.remain_tv.text =
                            VideoPlayer.player().duration().toLong().formatMillis()
                    }
                }
            }
    }

    private val mediaListener = object : MixinPlayer.MediaPlayerListenerWrapper() {
        override fun onRenderedFirstFrame(mid: String) {
            findViewPagerChildById(mid) {
                val parentView = it.getChildAt(0)
                if (parentView is FrameLayout) {
                    if (!setTransition) {
                        setTransition = true
                        ViewCompat.setTransitionName(parentView, "transition")
                        setStartPostTransition(parentView)
                    }
                    parentView.preview_iv.visibility = INVISIBLE
                    parentView.pip_iv.isEnabled = true
                    parentView.pip_iv.alpha = 1f
                    parentView.live_tv.isEnabled =
                        VideoPlayer.player().player.isCurrentWindowDynamic
                }
            }
        }

        override fun onPlayerError(mid: String, error: ExoPlaybackException) {
            showRefresh(mid)
        }

        override fun onPlayerStateChanged(mid: String, playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                STATE_ENDED -> {
                    stop()
                    if (aodWakeLock.isHeld) {
                        aodWakeLock.release()
                    }
                }
                STATE_IDLE -> {
                    stop()
                    if (aodWakeLock.isHeld) {
                        aodWakeLock.release()
                    }
                }
                STATE_READY -> {
                    onStateReady(mid, playWhenReady)
                    if (!aodWakeLock.isHeld) {
                        aodWakeLock.acquire()
                    }
                }
                STATE_BUFFERING -> showLoading(mid)
            }
        }

        override fun onVideoSizeChanged(
            mid: String,
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            var nWidth = width
            var nHeight = height
            if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                nWidth = height
                nHeight = width
            }
            val ratio = (if (nHeight == 0) 1 else nWidth * pixelWidthHeightRatio / nHeight) as Float
            findViewPagerChildById(mid) {
                val parentView = it.getChildAt(0)
                if (parentView is FrameLayout) {
                    parentView.video_aspect_ratio.setAspectRatio(ratio, unappliedRotationDegrees)
                }
            }
        }
    }

    private fun showRefresh(messageId: String) {
        findViewPagerChildById(messageId) {
            val parentView = it.getChildAt(0)
            parentView.play_view.status = STATUS_REFRESH
        }
    }

    private fun showLoading(messageId: String) {
        findViewPagerChildById(messageId) {
            val parentView = it.getChildAt(0)
            parentView.play_view.status = STATUS_LOADING
        }
    }

    private fun onStateReady(messageId: String, playWhenReady: Boolean) {
        findViewPagerChildById(messageId) {
            val parentView = it.getChildAt(0)
            if (playWhenReady) {
                fadeOut(parentView, parentView.tag as Boolean)
                parentView.play_view.status = STATUS_PLAYING
            } else {
                parentView.play_view.status = STATUS_IDLE
            }
        }
    }

    private val pageListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            if (lastPos == -1) {
                downloadMedia(position)
                return
            }
            if (lastPos == position) return

            stop()
            lastPos = position

            downloadMedia(position)
        }
    }

    private fun downloadMedia(position: Int) {
        val currMessageItem = pagerAdapter.getItem(position) ?: return
        if (currMessageItem.mediaStatus == MediaStatus.CANCELED.name) return
        if (currMessageItem.isMedia() && currMessageItem.mediaUrl == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                conversationRepository.findMessageById(currMessageItem.messageId)?.let {
                    jobManager.addJobInBackground(AttachmentDownloadJob(it))
                }
            }
        }
    }

    private var pipAnimationInProgress = false
    private fun switchToPip() {
        if (!checkInlinePermissions() || pipAnimationInProgress) {
            return
        }
        pipAnimationInProgress = true
        findViewPagerChildByTag {
            val windowView = it.getChildAt(0)
            val rect = PipVideoView.getPipRect(windowView.video_aspect_ratio.aspectRatio)
            val with = windowView.width
            val scale = rect.width / with
            val animatorSet = AnimatorSet()
            val position = IntArray(2)
            windowView.video_aspect_ratio.getLocationOnScreen(position)
            val messageItem = pagerAdapter.getItem(view_pager.currentItem) ?: return
            window.decorView.systemUiVisibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            val changedTextureView = pipVideoView.show(
                this, windowView.video_aspect_ratio.aspectRatio,
                windowView.video_aspect_ratio.videoRotation, conversationId,
                messageItem.messageId, messageItem.isVideo(), messageItem.mediaUrl
            )

            animatorSet.playTogether(
                ObjectAnimator.ofInt(colorDrawable, AnimationProperties.COLOR_DRAWABLE_ALPHA, 0),
                ObjectAnimator.ofFloat(windowView.video_texture, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(windowView.video_texture, View.SCALE_Y, scale),
                ObjectAnimator.ofFloat(
                    windowView.video_aspect_ratio,
                    View.TRANSLATION_X,
                    rect.x - windowView.video_aspect_ratio.x -
                        this.realSize().x * (1f - scale) / 2
                ),
                ObjectAnimator.ofFloat(
                    windowView.video_aspect_ratio,
                    View.TRANSLATION_Y,
                    rect.y - windowView.video_aspect_ratio.y +
                        this.statusBarHeight() - (windowView.video_aspect_ratio.height - rect.height) / 2
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
                    VideoPlayer.player().setVideoTextureView(changedTextureView)
                    if (messageItem.isVideo() && VideoPlayer.player().player.playbackState == STATE_IDLE) {
                        VideoPlayer.player()
                            .loadVideo(messageItem.mediaUrl!!, messageItem.messageId, true)
                        VideoPlayer.player().setVideoTextureView(changedTextureView)
                        VideoPlayer.player().pause()
                    }
                    dismiss()
                }
            })
            animatorSet.start()
        }
    }

    private fun dismiss() {
        container.visibility = INVISIBLE
        overridePendingTransition(0, 0)
        super.finish()
    }

    private val pipVideoView by lazy {
        PipVideoView.getInstance()
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

    companion object {
        private const val MESSAGE_ID = "id"
        private const val RATIO = "ratio"
        private const val CONVERSATION_ID = "conversation_id"
        private const val EXCLUDE_LIVE = "exclude_live"
        private const val ALPHA_MAX = 0xFF
        private const val PREFIX = "media"

        fun show(
            activity: Activity,
            imageView: View,
            conversationId: String,
            messageId: String,
            excludeLive: Boolean = false
        ) {
            val intent = Intent(activity, DragMediaActivity::class.java).apply {
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

        fun show(context: Context, conversationId: String, messageId: String, ratio: Float) {
            val intent = Intent(context, DragMediaActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(MESSAGE_ID, messageId)
                putExtra(RATIO, ratio)
            }
            context.startActivity(intent)
        }
    }
}
