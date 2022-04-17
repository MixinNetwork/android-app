package one.mixin.android.ui.qr

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.mlkit.vision.common.InputImage
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.FragmentEditBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.bounce
import one.mixin.android.extension.copy
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.realSize
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.websocket.VideoMessagePayload
import java.io.File

@AndroidEntryPoint
class EditFragment : VisionFragment() {

    companion object {
        const val TAG = "EditFragment"
        const val ARGS_PATH = "args_path"
        const val ARGS_FROM_GALLERY = "args_from_gallery"
        const val ARGS_FROM_SCAN = "args_from_scan"
        private const val IS_VIDEO: String = "IS_VIDEO"
        fun newInstance(
            path: String,
            isVideo: Boolean = false,
            fromGallery: Boolean = false,
            fromScan: Boolean = false
        ) = EditFragment().withArgs {
            putString(ARGS_PATH, path)
            putBoolean(IS_VIDEO, isVideo)
            putBoolean(ARGS_FROM_GALLERY, fromGallery)
            putBoolean(ARGS_FROM_SCAN, fromScan)
        }
    }

    private val path: String by lazy {
        requireArguments().getString(ARGS_PATH)!!
    }

    private val isVideo by lazy {
        requireArguments().getBoolean(IS_VIDEO)
    }

    private val fromGallery by lazy {
        requireArguments().getBoolean(ARGS_FROM_GALLERY)
    }

    private val fromScan by lazy {
        requireArguments().getBoolean(ARGS_FROM_SCAN)
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

    private var _binding: FragmentEditBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendFl.post {
            val navigationBarHeight = requireContext().navigationBarHeight()
            binding.sendFl.translationY = -navigationBarHeight.toFloat()
            binding.downloadIv.translationY = -navigationBarHeight.toFloat()
        }
        binding.closeIv.setOnClickListener { activity?.onBackPressed() }
        binding.downloadIv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        save()
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            binding.downloadIv.bounce()
        }
        binding.sendFl.setOnClickListener {
            if (isVideo) {
                ForwardActivity.show(
                    requireContext(),
                    arrayListOf(
                        ForwardMessage(
                            ForwardCategory.Video,
                            GsonHelper.customGson.toJson(VideoMessagePayload(File(path).toUri().toString()))
                        )
                    ),
                    ForwardAction.System(name = getString(R.string.Send), needEdit = false)
                )
            } else {
                ForwardActivity.show(
                    requireContext(),
                    arrayListOf(
                        ForwardMessage(
                            ShareCategory.Image,
                            GsonHelper.customGson.toJson(ShareImageData(File(path).toUri().toString())),
                        )
                    ),
                    ForwardAction.System(name = getString(R.string.Send), needEdit = false)
                )
            }
        }
        if (fromScan) {
            binding.sendFl.isVisible = false
            binding.downloadIv.isVisible = false
        }
        if (isVideo) {
            setBg()
            mixinPlayer.loadVideo(path)
            binding.previewVideoTexture.visibility = VISIBLE
            mixinPlayer.setVideoTextureView(binding.previewVideoTexture)
            mixinPlayer.start()
        } else {
            binding.previewIv.visibility = VISIBLE
            if (fromGallery) {
                binding.previewIv.loadImage(path)
                scan()
                setBg()
            } else {
                binding.previewIv.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.previewIv.loadImage(path, requestListener = glideRequestListener)
            }
        }
        binding.downloadIv.isVisible = !fromGallery
    }

    override fun onBackPressed(): Boolean {
        val captureFragment = activity?.supportFragmentManager?.findFragmentByTag(CaptureFragment.TAG) as? CaptureFragment
        captureFragment?.startImageAnalysisIfNeeded()
        return super.onBackPressed()
    }

    private fun scan() = lifecycleScope.launch(Dispatchers.IO) {
        if (viewDestroyed()) return@launch

        val bitmap = BitmapFactory.decodeFile(path) ?: return@launch
        try {
            val visionImage = InputImage.fromBitmap(bitmap, 0)
            scanner.process(visionImage)
                .addOnSuccessListener { result ->
                    val content = result.firstOrNull()?.rawValue
                    if (!content.isNullOrBlank()) {
                        lifecycleScope.launch innerLaunch@{
                            if (viewDestroyed()) return@innerLaunch
                            if (fromScan) {
                                handleResult(content)
                            } else {
                                pseudoNotificationView?.addContent(content)
                            }
                        }
                    } else {
                        lifecycleScope.launch innerLaunch@{
                            if (viewDestroyed()) return@innerLaunch
                            if (fromScan) {
                                showNoResultDialog()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            decodeWithZxing(bitmap)
        }
    }

    private suspend fun decodeWithZxing(bitmap: Bitmap) {
        val result = bitmap.decodeQR()
        if (!result.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                if (fromScan) {
                    handleResult(result)
                } else {
                    pseudoNotificationView?.addContent(result)
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                if (fromScan) {
                    showNoResultDialog()
                }
            }
        }
    }

    private fun showNoResultDialog() {
        alertDialogBuilder()
            .setMessage(getString(R.string.Qr_Code_not_found))
            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                activity?.onBackPressed()
            }
            .show()
    }

    private fun setBg() {
        binding.rootView.setBackgroundColor(resources.getColor(R.color.black, null))
    }

    private fun save() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        val outFile = if (isVideo) {
            requireContext().getPublicPicturePath().createVideoTemp("mp4", false)
        } else {
            requireContext().getPublicPicturePath().createImageTemp(noMedia = false)
        }
        withContext(Dispatchers.IO) {
            File(path).copy(outFile)
        }
        MediaScannerConnection.scanFile(context, arrayOf(outFile.toString()), null, null)
        toast(getString(R.string.save_to, outFile.absolutePath))
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

            if (viewDestroyed()) return

            binding.previewVideoTexture.setTransform(matrix)
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
