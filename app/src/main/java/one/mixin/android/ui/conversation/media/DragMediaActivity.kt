package one.mixin.android.ui.conversation.media

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_drag_media.*
import kotlinx.android.synthetic.main.item_video_layout.view.*
import kotlinx.android.synthetic.main.view_drag_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadVideoUseMark
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.save
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PhotoView.DismissFrameLayout
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.PlayView
import one.mixin.android.widget.PlayView.Companion.STATUS_BUFFERING
import one.mixin.android.widget.PlayView.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView.Companion.STATUS_LOADING
import one.mixin.android.widget.PlayView.Companion.STATUS_PAUSING
import one.mixin.android.widget.PlayView.Companion.STATUS_PLAYING
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DragMediaActivity : BaseActivity(), DismissFrameLayout.OnDismissListener {
    private lateinit var colorDrawable: ColorDrawable
    private val conversationId by lazy {
        intent.getStringExtra(CONVERSATION_ID)
    }
    private val messageId by lazy {
        intent.getStringExtra(MESSAGE_ID)
    }

    private var index: Int = 0
    private var lastPos: Int = -1
    private lateinit var pagerAdapter: MediaAdapter
    private var disposable: Disposable? = null

    private val mixinPlayer: MixinPlayer by lazy {
        MixinPlayer().apply {
            setOnVideoPlayerListener(videoListener)
        }
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_drag_media)
        colorDrawable = ColorDrawable(Color.BLACK)
        view_pager.backgroundDrawable = colorDrawable
        Observable.just(conversationId).observeOn(Schedulers.io())
            .map { conversationRepository.getMediaMessages(it).reversed() }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe {
                mainThread {
                    index = it.indexOfFirst { item -> messageId == item.messageId }
                    it.map {
                        if (it.type == MessageCategory.SIGNAL_VIDEO.name ||
                            it.type == MessageCategory.PLAIN_VIDEO.name) {
                        }
                    }
                    pagerAdapter = MediaAdapter(it, this)
                    view_pager.adapter = pagerAdapter
                    if (index != -1) {
                        view_pager.currentItem = index
                        lastPos = index
                    } else {
                        view_pager.currentItem = 0
                        lastPos = 0
                    }
                    play(index)
                }
            }
        view_pager.addOnPageChangeListener(pageListener)
    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mixinPlayer.release()
    }

    private fun showBottom() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(this, R.layout.view_drag_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.save.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDisposable(scopeProvider)
                .subscribe({ granted ->
                    if (granted) {
                        doAsync {
                            val outFile = this@DragMediaActivity.getImagePath().createImageTemp()
                            findViewPagerChildByTag {
                                val imageView = it.getChildAt(0) as ImageView
                                (imageView.drawable as BitmapDrawable).bitmap.save(outFile)
                                try {
                                    MediaStore.Images.Media.insertImage(contentResolver,
                                        outFile.absolutePath, outFile.name, null)
                                } catch (e: FileNotFoundException) {
                                    e.printStackTrace()
                                }
                                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))

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
        view.decode.setOnClickListener {
            findViewPagerChildByTag {
                val imageView = it.getChildAt(0) as ImageView
                val url = (imageView.drawable as BitmapDrawable).bitmap.decodeQR()
                if (url != null) {
                    if (isMixinUrl(url)) {
                        LinkBottomSheetDialogFragment.newInstance(url)
                            .show(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
                    } else {
                        QrBottomSheetDialogFragment.newInstance(url)
                            .show(supportFragmentManager, QrBottomSheetDialogFragment.TAG)
                    }
                } else {
                    toast(R.string.can_not_recognize)
                }
            }
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    private fun shareVideo() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            val url = pagerAdapter.list?.get(view_pager.currentItem)?.mediaUrl
            var uri = Uri.parse(url)
            if (ContentResolver.SCHEME_FILE == uri.scheme) {
                uri = getUriForFile(File(url))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            type = "video/*"
        }
        startActivity(Intent.createChooser(sendIntent, "Share video to.."))
    }

    inner class MediaAdapter(
        val list: List<MessageItem>?,
        private val onDismissListener: DismissFrameLayout.OnDismissListener
    ) : PagerAdapter(), TextureView.SurfaceTextureListener {

        fun getItem(position: Int): MessageItem = list!![position]

        override fun getCount(): Int = notNullElse(list, { it.size }, 0)

        override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val messageItem = getItem(position)
            val innerView = if (messageItem.type == MessageCategory.SIGNAL_IMAGE.name ||
                messageItem.type == MessageCategory.PLAIN_IMAGE.name) {
                createPhotoView(container, position, messageItem)
            } else {
                createVideoView(container, position, messageItem)
            }
            val layout = DismissFrameLayout(container.context)
            layout.setDismissListener(onDismissListener)
            layout.layoutParams = ViewPager.LayoutParams()
            layout.addView(innerView)
            layout.tag = "$PREFIX$position"
            container.addView(layout)
            return layout
        }

        private fun createVideoView(container: ViewGroup, position: Int, messageItem: MessageItem): View {
            val view = View.inflate(container.context, R.layout.item_video_layout, null)
            view.close_iv.setOnClickListener { finish() }
            view.share_iv.setOnClickListener { shareVideo() }
            view.close_iv.post {
                val statusBarHeight = statusBarHeight().toFloat()
                view.close_iv.translationY = statusBarHeight
                view.share_iv.translationY = statusBarHeight
            }
            view.video_texture.surfaceTextureListener = this
            val textureParams = view.video_texture.layoutParams
            val previewParams = view.video_texture.layoutParams
            val scaleW = container.width / messageItem.mediaWidth!!.toFloat()
            val scaleH = container.height / messageItem.mediaHeight!!.toFloat()
            when {
                scaleW > scaleH -> {
                    textureParams.height = container.height
                    previewParams.height = container.height
                    textureParams.width = (messageItem.mediaWidth * scaleH).toInt()
                    previewParams.width = (messageItem.mediaWidth * scaleH).toInt()
                }
                scaleW < scaleH -> {
                    textureParams.width = container.width
                    previewParams.width = container.width
                    textureParams.height = (messageItem.mediaHeight * scaleW).toInt()
                    previewParams.height = (messageItem.mediaHeight * scaleW).toInt()
                }
                else -> {
                    textureParams.height = container.height
                    previewParams.height = container.height
                    textureParams.width = (messageItem.mediaWidth * scaleH).toInt()
                    previewParams.width = (messageItem.mediaWidth * scaleH).toInt()
                }
            }
            view.video_texture.layoutParams = textureParams
            view.preview_iv.layoutParams = previewParams
            view.preview_iv.visibility = VISIBLE
            view.preview_iv.loadVideoUseMark(messageItem.mediaUrl ?: "", R.drawable.image_holder, R.drawable.chat_mark_image)

            if (position == index) {
                ViewCompat.setTransitionName(view.video_texture, "transition")
                setStartPostTransition(view.video_texture)
            }

            if (position != view_pager.currentItem) {
                view.play_view.visibility = VISIBLE
            }

            view.play_view.setOnClickListener {
                when (view.play_view.status) {
                    STATUS_IDLE -> {
                        setPreviewIv(false, view_pager.currentItem)
                        play(view_pager.currentItem)
                    }
                    STATUS_LOADING, STATUS_PLAYING, STATUS_BUFFERING -> {
                        pause()
                    }
                    STATUS_PAUSING -> {
                        start()
                        fadeOut(view)
                    }
                }
            }
            view.setOnClickListener {
                if (view.controller.isVisible) {
                    fadeOut(view)
                } else {
                    fadeIn(view)
                }
            }
            view.video_texture.setOnClickListener {
                if (view.controller.isVisible) {
                    fadeOut(view)
                } else {
                    fadeIn(view)
                }
            }

            var isPlaying = false
            view.seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isPlaying = mixinPlayer.isPlaying()
                    mixinPlayer.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isPlaying) {
                        mixinPlayer.start()
                    }
                }

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mixinPlayer.seekTo(progress * mixinPlayer.duration() / 200)
                    }
                }
            })

            return view
        }

        private fun createPhotoView(container: ViewGroup, position: Int, messageItem: MessageItem): PhotoView {
            val imageView = PhotoView(container.context)
            imageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (messageItem.mediaUrl.equals("image/gif", true)) {
                imageView.loadGif(messageItem.mediaUrl, object : RequestListener<GifDrawable?> {
                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (position == index) {
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
                })
            } else {
                imageView.loadImage(messageItem.mediaUrl, object : RequestListener<Drawable?> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (position == index) {
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
                })
            }
            imageView.setOnClickListener {
                finishAfterTransition()
            }
            imageView.setOnLongClickListener {
                showBottom()
                return@setOnLongClickListener true
            }
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            if (obj is View) {
                obj.tag?.let {
                    if (it is Disposable && !it.isDisposed) {
                        it.dispose()
                    }
                }
            }
            container.removeView(obj as View)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = false

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            setTextureView()
        }
    }

    private fun fadeIn(view: View, withoutPlay: Boolean = false) {
        if (!withoutPlay) {
            view.play_view.fadeIn()
        }
        view.controller.fadeIn()
        view.close_iv.fadeIn()
        view.share_iv.fadeIn()
    }

    private fun fadeOut(view: View, withoutPlay: Boolean = false) {
        if (!withoutPlay) {
            view.play_view.fadeOut()
        }
        view.controller.fadeOut()
        view.close_iv.fadeOut()
        view.share_iv.fadeOut()
    }

    private fun setTextureView() {
        findViewPagerChildByTag {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                mixinPlayer.setVideoTextureView(parentView.getChildAt(0) as TextureView)
            }
        }
    }

    private fun setPlayViewStatus(status: Int, pos: Int = lastPos) {
        findViewPagerChildByTag(pos) {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                (parentView.getChildAt(2) as PlayView).status = status
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
                fadeOut(parentView, true)
                parentView.preview_iv.visibility = VISIBLE
            }
        }
    }

    private fun listenDuration() {
        findViewPagerChildByTag {
            val parentView = it.getChildAt(0)
            if (parentView is FrameLayout) {
                disposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
                    if (mixinPlayer.duration() != 0) {
                        parentView.seek_bar.progress = (mixinPlayer.getCurrentPos() * 200 /
                            mixinPlayer.duration()).toInt()
                        parentView.duration_tv.text = mixinPlayer.getCurrentPos().formatMillis()
                    }
                }
                parentView.remain_tv.text = mixinPlayer.duration().toLong().formatMillis()
            }
        }
    }

    private fun setStartPostTransition(sharedView: View) {
        sharedView.doOnPreDraw { startPostponedEnterTransition() }
    }

    override fun onScaleProgress(scale: Float) {
        colorDrawable.alpha = Math.min(ALPHA_MAX, colorDrawable.alpha - (scale * ALPHA_MAX).toInt())
    }

    override fun onDismiss() {
        finishAfterTransition()
    }

    override fun finishAfterTransition() = if (view_pager.currentItem == index) {
        super.finishAfterTransition()
    } else {
        finish()
    }

    override fun onCancel() {
        colorDrawable.alpha = ALPHA_MAX
    }

    private inline fun findViewPagerChildByTag(pos: Int = view_pager.currentItem, action: (v: ViewGroup) -> Unit) {
        val v = view_pager.findViewWithTag<DismissFrameLayout>("$PREFIX$pos")
        if (v != null) {
            action(v as ViewGroup)
        }
    }

    private fun start() {
        setPlayViewStatus(STATUS_PLAYING)
        listenDuration()
        mixinPlayer.start()
    }

    private fun pause() {
        setPlayViewStatus(STATUS_PAUSING)
        disposable?.dispose()
        mixinPlayer.pause()
    }

    private fun stop() {
        setPlayViewStatus(STATUS_IDLE)
        handleLast()
        disposable?.dispose()
        mixinPlayer.stop()
    }

    private inline fun load(pos: Int, action: () -> Unit = {}) {
        val messageItem = pagerAdapter.getItem(pos)
        if (messageItem.type == MessageCategory.SIGNAL_VIDEO.name ||
            messageItem.type == MessageCategory.PLAIN_VIDEO.name) {
            messageItem.mediaUrl?.let {
                mixinPlayer.loadVideo(it)
            }
            setTextureView()
            action()
        }
    }

    private fun play(pos: Int) = load(pos, { start() })

    private val videoListener = object : MixinPlayer.VideoPlayerListenerWrapper() {
        override fun onRenderedFirstFrame() {
            setPreviewIv(false, view_pager.currentItem)
            findViewPagerChildByTag {
                val parentView = it.getChildAt(0)
                if (parentView is FrameLayout) {
                    fadeOut(parentView)
                }
            }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            if (mixinPlayer.isPlaying() && isLoading && mixinPlayer.player.playbackState == STATE_BUFFERING) {
                setPlayViewStatus(STATE_BUFFERING)
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d("@@@", "onPlayerStateChanged  status:$playbackState")
        }
    }

    private val pageListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            if (lastPos == -1 || lastPos == position) return

            stop()
            lastPos = position
        }
    }

    companion object {
        private const val MESSAGE_ID = "id"
        private const val CONVERSATION_ID = "conversation_id"
        private const val ALPHA_MAX = 0xFF
        private const val PREFIX = "media"

        fun show(activity: Activity, imageView: View, messageItem: MessageItem?) {
            messageItem?.let {
                val intent = Intent(activity, DragMediaActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, messageItem.conversationId)
                    putExtra(MESSAGE_ID, messageItem.messageId)
                }
                activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity, imageView,
                    "transition").toBundle())
            }
        }
    }
}