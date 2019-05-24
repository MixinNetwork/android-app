package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_edit.*
import one.mixin.android.R
import one.mixin.android.extension.copy
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getPublicPictyresPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

class EditFragment : BaseFragment() {

    companion object {
        const val TAG = "EditFragment"
        const val ARGS_PATH = "args_path"
        private const val IS_VIDEO: String = "IS_VIDEO"
        fun newInstance(path: String, isVideo: Boolean = false): EditFragment {
            return EditFragment().withArgs {
                putString(ARGS_PATH, path)
                putBoolean(IS_VIDEO, isVideo)
            }
        }
    }

    private val path: String by lazy {
        arguments!!.getString(ARGS_PATH)
    }

    private val isVideo by lazy {
        arguments!!.getBoolean(PreviewDialogFragment.IS_VIDEO)
    }

    private val mixinPlayer: MixinPlayer by lazy {
        MixinPlayer().apply {
            setOnVideoPlayerListener(videoListener)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVideo) {
            mixinPlayer.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isVideo) {
            mixinPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isVideo) {
            mixinPlayer.release()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_edit, container, false)

    @SuppressLint("AutoDispose")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        send_fl.post {
            val params = send_fl.layoutParams as RelativeLayout.LayoutParams
            val b = send_fl.bottom + params.bottomMargin
            val hasNavigationBar = context!!.hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = context!!.navigationBarHeight()
                send_fl.translationY = -navigationBarHeight.toFloat()
                download_iv.translationY = -navigationBarHeight.toFloat()
            }
        }
        close_iv.setOnClickListener { activity?.onBackPressed() }
        download_iv.setOnClickListener {
            RxPermissions(activity!!)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        doAsync {
                            val outFile = if (isVideo) {
                                requireContext().getPublicPictyresPath().createVideoTemp("mp4", false)
                            } else {
                                requireContext().getPublicPictyresPath().createImageTemp(noMedia = false)
                            }
                            File(path).copy(outFile)
                            requireContext().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
                            uiThread { requireContext().toast(R.string.save_success) }
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
        send_fl.setOnClickListener {
            if (isVideo) {
                ForwardActivity.show(context!!, arrayListOf(ForwardMessage(
                    ForwardCategory.VIDEO.name, mediaUrl = path)), true)
            } else {
                ForwardActivity.show(context!!, arrayListOf(ForwardMessage(
                    ForwardCategory.IMAGE.name, mediaUrl = path)), true)
            }
        }
        if (isVideo) {
            mixinPlayer.loadVideo(path)
            preview_video_texture.visibility = VISIBLE
            mixinPlayer.setVideoTextureView(preview_video_texture)
            mixinPlayer.start()
        } else {
            preview_iv.visibility = VISIBLE
            Glide.with(preview_iv).load(path).into(preview_iv)
        }
    }

    private val videoListener = object : MixinPlayer.VideoPlayerListenerWrapper() {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            val ratio = width / height.toFloat()
            val screenWidth = requireContext().realSize().x
            val screenHeight = requireContext().realSize().y
            val matrix = Matrix()
            if (screenWidth / ratio < screenHeight) {
                matrix.postScale(screenHeight * ratio / screenWidth, 1f, screenWidth / 2f, screenHeight / 2f)
            } else {
                matrix.postScale(1f, screenWidth / ratio / screenHeight, screenWidth / 2f, screenHeight / 2f)
            }
            preview_video_texture.setTransform(matrix)
        }
    }
}