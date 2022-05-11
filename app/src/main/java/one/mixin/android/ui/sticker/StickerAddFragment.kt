package one.mixin.android.ui.sticker

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.databinding.FragmentAddStickerBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isStickerSupport
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.scaleDown
import one.mixin.android.extension.scaleUp
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toByteArray
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toPNGBytes
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Sticker
import one.mixin.android.widget.gallery.MimeType
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@Suppress("BlockingMethodInNonBlockingContext")
@AndroidEntryPoint
class StickerAddFragment : BaseFragment() {
    companion object {
        const val TAG = "StickerAddFragment"
        const val ARGS_URL = "args_url"
        const val ARGS_FROM_MANAGEMENT = "args_from_management"

        private const val MIN_SIZE = 128
        private const val MAX_SIZE = 1024
        private const val RATIO_MIN_MAX = MIN_SIZE / MAX_SIZE.toFloat()
        private const val RATIO_MAX_MIN = MAX_SIZE / MIN_SIZE.toFloat()
        private const val MIN_FILE_SIZE = 1024
        private const val MAX_FILE_SIZE = 1024 * 1024

        fun newInstance(url: String, fromManagement: Boolean = false) = StickerAddFragment().apply {
            arguments = bundleOf(
                ARGS_URL to url,
                ARGS_FROM_MANAGEMENT to fromManagement
            )
        }
    }

    private val url: String by lazy { requireArguments().getString(ARGS_URL)!! }
    private val fromManagement: Boolean by lazy { requireArguments().getBoolean(ARGS_FROM_MANAGEMENT) }
    private var dialog: Dialog? = null
    private val dp100 by lazy {
        requireContext().dpToPx(100f)
    }

    private val stickerViewModel by viewModels<ConversationViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_sticker, container, false)

    private val binding by viewBinding(FragmentAddStickerBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.rightTv.textColor = requireContext().colorFromAttribute(R.attr.text_primary)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        binding.titleView.rightAnimator.setOnClickListener {
            if (dialog == null) {
                dialog = indeterminateProgressDialog(
                    message = R.string.Please_wait_a_bit,
                    title = R.string.Adding
                ).apply {
                    setCancelable(false)
                }
            }
            dialog?.show()
            addSticker()
        }
        loadImage()
    }

    private fun loadImage() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        val w = withContext(Dispatchers.IO) {
            try {
                val byteArray = Glide.with(MixinApplication.appContext)
                    .`as`(ByteArray::class.java)
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, BitmapFactory.Options())
                if (bitmap.width < dp100) {
                    dp100
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }

        if (w == dp100) {
            binding.stickerIv.updateLayoutParams<ViewGroup.LayoutParams> {
                width = w
                height = w
            }
        } else {
            binding.stickerIv.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        binding.stickerIv.loadImage(url)
    }

    private fun addSticker() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        val request = try {
            val uri = url.toUri()
            val mimeType = getMimeType(uri, true)
            if (mimeType?.isStickerSupport() != true) {
                handleBack(R.string.Invalid_sticker_format)
                return@launch
            }
            getStickerAddRequest(mimeType, uri)
        } catch (e: Exception) {
            handleBack(R.string.Add_sticker_failed)
            null
        } ?: return@launch

        val r = try {
            stickerViewModel.addStickerAsync(request).await()
        } catch (e: Exception) {
            ErrorHandler.handleError(e)
            dialog?.dismiss()
            return@launch
        }
        if (r.isSuccess) {
            doAfterStickerAdded(r)
        } else {
            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription, getString(R.string.File_error))
            handleBack()
        }
    }

    private suspend fun doAfterStickerAdded(r: MixinResponse<Sticker>) = withContext(Dispatchers.IO) {
        val personalAlbum = stickerViewModel.getPersonalAlbums()
        if (personalAlbum == null) { // not add any personal sticker yet
            stickerViewModel.refreshStickerAlbums()
        } else {
            stickerViewModel.addStickerLocal(r.data as Sticker, personalAlbum.albumId)
        }
        Glide.with(requireContext()).load(r.data?.assetUrl).listener(
            object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    handleBack(R.string.Add_success)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    handleBack(R.string.Add_success)
                    return true
                }
            }
        ).submit(r.data!!.assetWidth, r.data!!.assetHeight)
    }

    private suspend fun getStickerAddRequest(
        mimeType: String?,
        uri: Uri
    ): StickerAddRequest? = withContext(Dispatchers.IO) {
        return@withContext if (mimeType == MimeType.GIF.toString() || mimeType == MimeType.WEBP.toString()) {
            val path = uri.getFilePath(requireContext())
            if (path == null) {
                withContext(Dispatchers.Main) {
                    handleBack(R.string.Add_sticker_failed)
                }
                return@withContext null
            }
            val f = File(path)
            if (f.length() < MIN_FILE_SIZE || f.length() > MAX_FILE_SIZE) {
                handleBack(R.string.sticker_add_invalid_size)
                return@withContext null
            }

            val byteArray = if (mimeType == MimeType.GIF.toString()) {
                val gifDrawable = Glide.with(MixinApplication.appContext)
                    .asGif()
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS)
                val w = gifDrawable.intrinsicWidth
                val h = gifDrawable.intrinsicHeight
                if (min(w, h) >= MIN_SIZE && max(w, h) <= MAX_SIZE) {
                    val buffer = gifDrawable.buffer
                    val bytes = ByteArray(buffer.capacity())
                    (buffer.duplicate().clear() as ByteBuffer).get(bytes)
                    bytes
                } else {
                    handleBack(R.string.sticker_add_invalid_size)
                    return@withContext null
                }
            } else {
                Glide.with(MixinApplication.appContext)
                    .asFile()
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS).toByteArray()
            }

            StickerAddRequest(Base64.encodeToString(byteArray, Base64.NO_WRAP))
        } else {
            var bitmap = Glide.with(MixinApplication.appContext)
                .asBitmap()
                .load(url)
                .submit()
                .get(10, TimeUnit.SECONDS)

            val ratio = bitmap.width / bitmap.height.toFloat()
            if (ratio in RATIO_MIN_MAX..RATIO_MAX_MIN) {
                if (min(bitmap.width, bitmap.height) < MIN_SIZE) {
                    bitmap = bitmap.scaleUp(MIN_SIZE)
                } else if (max(bitmap.width, bitmap.height) > MAX_SIZE) {
                    bitmap = bitmap.scaleDown(MAX_SIZE)
                }
                StickerAddRequest(
                    Base64.encodeToString(
                        if (mimeType == MimeType.PNG.toString()) bitmap.toPNGBytes() else bitmap.toBytes(),
                        Base64.NO_WRAP
                    )
                )
            } else {
                handleBack(R.string.sticker_add_invalid_size)
                return@withContext null
            }
        }
    }

    private fun handleBack(toastRes: Int? = null) = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        dialog?.dismiss()
        toastRes?.let { toast(it) }
        if (fromManagement) {
            parentFragmentManager.popBackStackImmediate()
        } else {
            requireActivity().finish()
        }
    }
}
