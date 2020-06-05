package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bottom_qr_scan.view.*
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.toast
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode
import java.util.concurrent.TimeUnit

class QrScanBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QrScanBottomSheetDialogFragment"
        const val ARGS_TEXT = "args_text"

        fun newInstance(text: String, conversationId: String? = null) = QrScanBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_TEXT, text)
                putString(ARGS_CONVERSATION_ID, conversationId)
            }
        }
    }

    private val text: String by lazy { requireArguments().getString(ARGS_TEXT)!! }
    private val conversationId: String? by lazy { requireArguments().getString(ARGS_CONVERSATION_ID) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.bottom_qr_scan, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.qr_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        contentView.qr_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        contentView.qr_tv.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(conversationId, parentFragmentManager, lifecycleScope)
            dismiss()
        }
        contentView.qr_tv.text = text
        contentView.copy.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, text))
            toast(R.string.copy_success)
            dismiss()
        }
        if (text.isWebUrl()) {
            contentView.open_fl.visibility = VISIBLE
            contentView.open.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .throttleFirst(1, TimeUnit.SECONDS)
                .autoDispose(stopScope).subscribe {
                    WebBottomSheetDialogFragment.newInstance(text, conversationId)
                        .showNow(parentFragmentManager, WebBottomSheetDialogFragment.TAG)
                    dismiss()
                }
        } else {
            contentView.open_fl.visibility = GONE
        }
    }
}
