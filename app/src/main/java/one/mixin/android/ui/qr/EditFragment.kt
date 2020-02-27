package one.mixin.android.ui.qr

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.io.File
import kotlinx.android.synthetic.main.fragment_edit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.bounce
import one.mixin.android.extension.copy
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

class EditFragment : CaptureVisionFragment() {

    companion object {
        const val TAG = "EditFragment"
        const val ARGS_PATH = "args_path"
        const val ARGS_FROM_GALLERY = "args_from_gallery"
        private const val IS_VIDEO: String = "IS_VIDEO"
        fun newInstance(
            path: String,
            isVideo: Boolean = false,
            fromGallery: Boolean = false
        ) = EditFragment().withArgs {
            putString(ARGS_PATH, path)
            putBoolean(IS_VIDEO, isVideo)
            putBoolean(ARGS_FROM_GALLERY, fromGallery)
        }
    }

    private val path: String by lazy {
        arguments!!.getString(ARGS_PATH)!!
    }

    private val isVideo by lazy {
        arguments!!.getBoolean(IS_VIDEO)
    }

    private val fromGallery by lazy {
        arguments!!.getBoolean(ARGS_FROM_GALLERY)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        send_fl.post {
            val params = send_fl.layoutParams as RelativeLayout.LayoutParams
            val b = send_fl.bottom + params.bottomMargin
            val hasNavigationBar = requireContext().hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = requireContext().navigationBarHeight()
                send_fl.translationY = -navigationBarHeight.toFloat()
                download_iv.translationY = -navigationBarHeight.toFloat()
            }
        }
        close_iv.setOnClickListener { activity?.onBackPressed() }
        download_iv.setOnClickListener {
            RxPermissions(activity!!)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        save()
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            download_iv.bounce()
        }
        send_fl.setOnClickListener {
            if (isVideo) {
                ForwardActivity.show(requireContext(), arrayListOf(ForwardMessage(
                    ForwardCategory.VIDEO.name, mediaUrl = path)), isShare = true)
            } else {
                ForwardActivity.show(requireContext(), arrayListOf(ForwardMessage(
                    ForwardCategory.IMAGE.name, mediaUrl = path)), isShare = true)
            }
        }
        if (isVideo) {
            setBg()
            mixinPlayer.loadVideo(path)
            preview_video_texture.visibility = VISIBLE
            mixinPlayer.setVideoTextureView(preview_video_texture)
            mixinPlayer.start()
        } else {
            preview_iv.visibility = VISIBLE
            if (fromGallery) {
                preview_iv.loadImage(path)
                scan()
                setBg()
            } else {
                preview_iv.loadImage(path, requestListener = glideRequestListener)
            }
        }
        download_iv.isVisible = !fromGallery
    }

    private fun scan() = lifecycleScope.launch(Dispatchers.IO) {
        if (!isAdded) return@launch

        val bitmap = BitmapFactory.decodeFile(path) ?: return@launch
        if (requireContext().isGooglePlayServicesAvailable()) {
            val visionImage = FirebaseVisionImage.fromBitmap(bitmap)
            detector.use { d ->
                d.detectInImage(visionImage)
                    .addOnSuccessListener { result ->
                        result.firstOrNull()?.rawValue?.let {
                            lifecycleScope.launch {
                                if (!isAdded) return@launch
                                pseudoNotificationView.addContent(it)
                            }
                        }
                    }
            }
        } else {
            bitmap.decodeQR()?.let {
                withContext(Dispatchers.Main) {
                    pseudoNotificationView.addContent(it)
                }
            }
        }
    }

    private fun setBg() {
        root_view?.setBackgroundColor(resources.getColor(R.color.black, null))
    }

    private fun save() = lifecycleScope.launch {
        if (!isAdded) return@launch

        val outFile = if (isVideo) {
            requireContext().getPublicPicturePath().createVideoTemp("mp4", false)
        } else {
            requireContext().getPublicPicturePath().createImageTemp(noMedia = false)
        }
        withContext(Dispatchers.IO) {
            File(path).copy(outFile)
        }
        requireContext().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
        requireContext().toast(getString(R.string.save_to, outFile.absolutePath))
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

    private val glideRequestListener = object : RequestListener<Drawable?> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable?>?,
            isFirstResource: Boolean
        ): Boolean {
            setBg()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable?>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            setBg()
            return false
        }
    }
}
