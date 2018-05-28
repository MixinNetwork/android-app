package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_qr_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.saveQRCode
import one.mixin.android.util.Session
import one.mixin.android.widget.BottomSheet

class QrBottomSheetDialogFragment: MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QrBottomSheetDialogFragment"

        fun newInstance(userId: String) = QrBottomSheetDialogFragment().apply {
            arguments = bundleOf(
                ARGS_USER_ID to userId
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_qr_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title.left_ib.setOnClickListener { activity?.onBackPressed() }

        if (context!!.isQRCodeFileExists()) {
            contentView.qr.setImageBitmap(BitmapFactory.decodeFile(context!!.getQRCodePath().absolutePath))
        } else {
            contentView.qr.post {
                Observable.create<Bitmap> { e ->
                    val account = Session.getAccount() ?: return@create
                    val b = account.code_url.generateQRCode(contentView.qr.width)
                    if (b != null) {
                        b.saveQRCode(context!!)
                        e.onNext(b)
                    }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDisposable(scopeProvider)
                    .subscribe({ r ->
                        contentView.qr.setImageBitmap(r)
                    }, { _ ->
                    })
            }
        }
    }
}