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
import kotlinx.android.synthetic.main.view_dialog_media.view.*
import one.mixin.android.R

class PreviewDialogFragment : DialogFragment() {

    private var mediaDialogView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mediaDialogView = inflater.inflate(R.layout.view_dialog_media, null, false)
        mediaDialogView!!.dialog_close_iv.setOnClickListener {
            dismiss()
        }
        return mediaDialogView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        super.onActivityCreated(savedInstanceState)
        dialog.window.setBackgroundDrawable(ColorDrawable(0x00000000))
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        dialog.window.setWindowAnimations(R.style.BottomSheet_Animation)
        mediaDialogView!!.dialog_send_ib.setOnClickListener { action!!(uri!!); dismiss() }
        Glide.with(context!!).load(uri).apply(RequestOptions().fitCenter()).into(mediaDialogView!!.dialog_iv)
    }

    private var uri: Uri? = null
    private var action: ((Uri) -> Unit)? = null
    fun show(fragmentManager: FragmentManager?, uri: Uri, action: (Uri) -> Unit) {
        super.show(fragmentManager, "PreviewDialogFragment")
        this.uri = uri
        this.action = action
    }
}
