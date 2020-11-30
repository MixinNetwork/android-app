package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.BottomQrScanBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.toast
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class QrScanBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QrScanBottomSheetDialogFragment"
        const val ARGS_TEXT = "args_text"

        fun newInstance(text: String, conversationId: String? = null) =
            QrScanBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_TEXT, text)
                    putString(ARGS_CONVERSATION_ID, conversationId)
                }
            }
    }

    private val text: String by lazy { requireArguments().getString(ARGS_TEXT)!! }
    private val conversationId: String? by lazy { requireArguments().getString(ARGS_CONVERSATION_ID) }

    private val binding by viewBinding(BottomQrScanBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            qrTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
            qrTv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
            qrTv.setAutoLinkOnClickListener { _, url ->
                url.openAsUrlOrWeb(requireActivity(), conversationId, parentFragmentManager, lifecycleScope)
                dismiss()
            }
            qrTv.text = text
            copy.setOnClickListener {
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, text))
                toast(R.string.copy_success)
                dismiss()
            }
            if (text.isWebUrl()) {
                openFl.visibility = VISIBLE
                open.clicks()
                    .observeOn(AndroidSchedulers.mainThread())
                    .throttleFirst(1, TimeUnit.SECONDS)
                    .autoDispose(stopScope).subscribe {
                        WebActivity.show(requireActivity(), text, conversationId)
                        dismiss()
                    }
            } else {
                openFl.visibility = GONE
            }
        }
    }
}
