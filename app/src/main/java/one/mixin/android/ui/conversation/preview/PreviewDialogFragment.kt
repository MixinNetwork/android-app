package one.mixin.android.ui.conversation.preview

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_preview.view.*
import kotlinx.android.synthetic.main.fragment_preview_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.widget.VideoTimelineView
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class PreviewDialogFragment : DialogFragment(), VideoTimelineView.VideoTimelineViewDelegate {

    companion object {
        const val IS_VIDEO: String = "IS_VIDEO"
        fun newInstance(isVideo: Boolean = false): PreviewDialogFragment {
            val previewDialogFragment = PreviewDialogFragment()
            val b = Bundle().apply {
                putBoolean(IS_VIDEO, isVideo)
            }
            previewDialogFragment.arguments = b
            return previewDialogFragment
        }
    }

    private val isVideo by lazy {
        arguments!!.getBoolean(IS_VIDEO)
    }

    private val mixinPlayer: MixinPlayer by lazy {
        MixinPlayer().apply {
            setOnVideoPlayerListener(videoListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mixinPlayer.release()
    }

    override fun onPause() {
        super.onPause()
        mixinPlayer.pause()
    }

    private var mediaDialogView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mediaDialogView = inflater.inflate(if (isVideo) {
            R.layout.fragment_preview_video
        } else {
            R.layout.fragment_preview
        }, null, false)
        if (isVideo) {
            mediaDialogView!!.dialog_play.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mixinPlayer.pause()
                } else {
                    mixinPlayer.start()
                }
            }
            mediaDialogView!!.time.setDelegate(this)
            mediaDialogView!!.dialog_cancel.setOnClickListener {
                dismiss()
            }
        } else {
            mediaDialogView!!.dialog_close_iv.setOnClickListener {
                dismiss()
            }
        }

        return mediaDialogView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        super.onActivityCreated(savedInstanceState)
        dialog.window.setBackgroundDrawable(ColorDrawable(0x00000000))
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        dialog.window.setWindowAnimations(R.style.BottomSheet_Animation)
        if (isVideo) {
            val mimeType = getMimeType(uri!!)
            if (mimeType == null || !mimeType.startsWith("video", true)) {
                context?.toast(R.string.error_format)
                dismiss()
            }
            mixinPlayer.loadVideo(uri.toString())
            mixinPlayer.setVideoTextureView(mediaDialogView!!.dialog_video_texture)
            mediaDialogView!!.time.setVideoPath(uri!!.getFilePath(context!!))
            Observable.interval(0, 100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
                if (mixinPlayer.duration() != 0 && mixinPlayer.isPlaying()) {
                    mediaDialogView!!.time.progress = mixinPlayer.getCurrentPos().toFloat() / mixinPlayer.duration()
                }
            }
            mediaDialogView!!.dialog_ok.setOnClickListener {
                action!!(uri!!)
                dismiss()
            }
        } else {
            mediaDialogView!!.dialog_send_ib.setOnClickListener { action!!(uri!!); dismiss() }
            Glide.with(context!!).load(uri).apply(RequestOptions().fitCenter()).into(mediaDialogView!!.dialog_iv)
        }
    }

    private var uri: Uri? = null
    private var action: ((Uri) -> Unit)? = null
    fun show(fragmentManager: FragmentManager?, uri: Uri, action: (Uri) -> Unit) {
        super.show(fragmentManager, if (isVideo) {
            "PreviewVideoDialogFragment"
        } else {
            "PreviewDialogFragment"
        })
        this.uri = uri
        this.action = action
    }

    private val videoListener = object : MixinPlayer.VideoPlayerListenerWrapper() {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            val ratio = width / height.toFloat()
            val lp = mediaDialogView!!.dialog_video_texture.layoutParams
            val screenWidth = context!!.displaySize().x
            val screenHeight = context!!.displaySize().y
            if (screenWidth / ratio > screenHeight) {
                lp.height = screenHeight
                lp.width = (screenHeight * ratio).toInt()
            } else {
                lp.width = screenWidth
                lp.height = (screenWidth / ratio).toInt()
            }
            mediaDialogView!!.dialog_video_texture.layoutParams = lp
        }
    }

    private var currentState = false
    override fun didStopDragging() {
        if (currentState) {
            mixinPlayer.start()
        }
    }

    override fun didStartDragging() {
        currentState = mixinPlayer.isPlaying()
        mixinPlayer.pause()
    }

    override fun onPlayProgressChanged(progress: Float) {
        mixinPlayer.seekTo((progress * mixinPlayer.duration()).toInt())
    }
}
