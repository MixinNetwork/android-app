package one.mixin.android.ui.conversation

import android.app.Dialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_sticker.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.maxSizeScale
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Sticker
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StickerAddFragment : BaseFragment() {
    companion object {
        const val TAG = "StickerAddFragment"
        const val ARGS_URL = "args_url"

        const val MIN_SIZE = 64
        const val MAX_SIZE = 512

        fun newInstance(url: String) = StickerAddFragment().apply {
            arguments = bundleOf(ARGS_URL to url)
        }
    }

    private val url: String by lazy { arguments!!.getString(ARGS_URL) }
    private var dialog: Dialog? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_tv.textColor = Color.BLACK
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            if (dialog == null) {
                dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message,
                    title = R.string.group_adding).apply {
                    setCancelable(false)
                }
            }
            dialog?.show()
            addSticker()
        }
        sticker_iv.loadImage(url)
    }

    private fun addSticker() {
        doAsync {
            try {
                val f = File(url.toUri().getFilePath(requireContext()))
                if (f.length() < 1000 || f.length() > 500 * 1000) {
                    dialog?.dismiss()
                    uiThread { requireContext().toast(R.string.sticker_add_invalid) }
                    return@doAsync
                }
                val uri = url.toUri()
                val mimeType = getMimeType(uri)
                if (mimeType?.isImageSupport() != true) {
                    dialog?.dismiss()
                    uiThread { requireContext().toast(R.string.sticker_add_invalid) }
                    return@doAsync
                }
                val stickerAddRequest = if (mimeType == MimeType.GIF.toString() || mimeType == MimeType.WEBP.toString()) {
                    val byteArray = Glide.with(MixinApplication.appContext)
                        .`as`(ByteArray::class.java)
                        .load(url)
                        .submit()
                        .get(10, TimeUnit.SECONDS)
                    StickerAddRequest(Base64.encodeToString(byteArray, Base64.NO_WRAP))
                } else {
                    var bitmap = Glide.with(MixinApplication.appContext)
                        .asBitmap()
                        .load(url)
                        .submit()
                        .get(10, TimeUnit.SECONDS)
                    if (bitmap.width < MIN_SIZE || bitmap.height < MIN_SIZE) {
                        dialog?.dismiss()
                        uiThread { requireContext().toast(R.string.sticker_add_invalid) }
                        return@doAsync
                    }
                    bitmap = bitmap.maxSizeScale(MAX_SIZE, MAX_SIZE)
                    StickerAddRequest(Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP))
                }
                stickerViewModel.addSticker(stickerAddRequest)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
                        dialog?.dismiss()
                        if (it != null && it.isSuccess) {
                            stickerViewModel.addStickerLocal(it.data as Sticker)
                            uiThread { requireFragmentManager().popBackStackImmediate() }
                        }
                    }, {
                        ErrorHandler.handleError(it)
                        dialog?.dismiss()
                        uiThread { requireContext().toast(R.string.sticker_add_failed) }
                    })
            } catch (e: Exception) {
                dialog?.dismiss()
                uiThread { requireContext().toast(R.string.sticker_add_failed) }
            }
        }
    }
}