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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.fragment_add_sticker.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isStickerSupport
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.maxSizeScale
import one.mixin.android.extension.toByteArray
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toPNGBytes
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Sticker
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.textColor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StickerAddFragment : BaseFragment() {
    companion object {
        const val TAG = "StickerAddFragment"
        const val ARGS_URL = "args_url"
        const val ARGS_FROM_MANAGEMENT = "args_from_management"

        const val MIN_SIZE = 64
        const val MAX_SIZE = 512
        const val MIN_FILE_SIZE = 1024
        const val MAX_FILE_SIZE = 800 * 1024

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

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_sticker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.right_tv.textColor = requireContext().colorFromAttribute(R.attr.text_primary)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            if (dialog == null) {
                dialog = indeterminateProgressDialog(
                    message = R.string.pb_dialog_message,
                    title = R.string.group_adding
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
        if (!isAdded) return@launch

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
            sticker_iv.updateLayoutParams<ViewGroup.LayoutParams> {
                width = w
                height = w
            }
        } else {
            sticker_iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        sticker_iv.loadImage(url)
    }

    private fun addSticker() = lifecycleScope.launch {
        if (!isAdded) return@launch

        val request = try {
            val uri = url.toUri()
            val mimeType = getMimeType(uri, true)
            if (mimeType?.isStickerSupport() != true) {
                handleBack(R.string.sticker_add_invalid_format)
                return@launch
            }
            getStickerAddRequest(mimeType, uri)
        } catch (e: Exception) {
            handleBack(R.string.sticker_add_failed)
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
            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription, getString(R.string.error_image))
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
        Glide.with(requireContext()).load(r.data?.assetUrl).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                handleBack(R.string.add_success)
                return true
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                handleBack(R.string.add_success)
                return true
            }
        }).submit(r.data!!.assetWidth, r.data!!.assetHeight)
    }

    private suspend fun getStickerAddRequest(
        mimeType: String?,
        uri: Uri
    ): StickerAddRequest? = withContext(Dispatchers.IO) {
        return@withContext if (mimeType == MimeType.GIF.toString() || mimeType == MimeType.WEBP.toString()) {
            val path = uri.getFilePath(requireContext())
            if (path == null) {
                withContext(Dispatchers.Main) {
                    handleBack(R.string.sticker_add_failed)
                }
                return@withContext null
            }
            val f = File(path)
            if (f.length() < MIN_FILE_SIZE || f.length() > MAX_FILE_SIZE) {
                handleBack(R.string.sticker_add_invalid_size)
                return@withContext null
            }
            val byteArray = if (mimeType == MimeType.GIF.toString()) {
                Glide.with(MixinApplication.appContext)
                    .`as`(ByteArray::class.java)
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS)
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
            if (bitmap.width < MIN_SIZE || bitmap.height < MIN_SIZE) {
                handleBack(R.string.sticker_add_invalid_size)
                return@withContext null
            }

            bitmap = bitmap.maxSizeScale(MAX_SIZE, MAX_SIZE)
            StickerAddRequest(
                Base64.encodeToString(
                    if (mimeType == MimeType.PNG.toString()) bitmap.toPNGBytes() else bitmap.toBytes(), Base64.NO_WRAP
                )
            )
        }
    }

    private fun handleBack(toastRes: Int? = null) = lifecycleScope.launch {
        if (!isAdded) return@launch

        dialog?.dismiss()
        toastRes?.let { requireContext().toast(it) }
        if (fromManagement) {
            parentFragmentManager.popBackStackImmediate()
        } else {
            requireActivity().finish()
        }
    }
}
