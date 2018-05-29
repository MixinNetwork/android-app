package one.mixin.android.ui.contacts

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_qr.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.Constants.MY_QR
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.saveQRCode
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.User

class QRFragment : BaseFragment() {
    companion object {
        const val TAG = "QRFragment"

        const val WHITE = -0x1
        const val BLACK = -0x1000000

        fun newInstance(user: User?): QRFragment {
            val f = QRFragment()
            val b = Bundle()
            b.putParcelable(Constants.ARGS_USER, user)
            f.arguments = b
            return f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_qr, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        val user: User? = arguments!!.getParcelable(Constants.ARGS_USER)
        if (user != null) {
            val info = if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0] else ' '
            avatar.setInfo(info, user.avatarUrl, user.identityNumber)
            qr_avatar.setInfo(info, user.avatarUrl, user.identityNumber)
            name.text = user.fullName
            user_id.text = context?.getString(R.string.contact_mixin_id, user.identityNumber)

            if (context!!.isQRCodeFileExists(MY_QR)) {
                qr.setImageBitmap(BitmapFactory.decodeFile(context!!.getQRCodePath(MY_QR).absolutePath))
            } else {
                qr.post {
                    Observable.create<Bitmap> { e ->
                        val account = Session.getAccount() ?: return@create
                        val b = account.code_url.generateQRCode(qr.width)
                        if (b != null) {
                            b.saveQRCode(context!!, account.code_url)
                            e.onNext(b)
                        }
                    }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDisposable(scopeProvider)
                        .subscribe({ r ->
                            qr.setImageBitmap(r)
                        }, { _ ->
                        })
                }
            }
        }
    }
}