package one.mixin.android.ui.wallet

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_address.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.saveQRCode
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.support.v4.toast

class AddressFragment : Fragment() {
    companion object {
        const val TAG = "AddressFragment"

        fun newInstance(asset: AssetItem): AddressFragment {
            val f = AddressFragment()
            val b = Bundle()
            b.putParcelable(ARGS_ASSET, asset)
            f.arguments = b
            return f
        }
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_address, container, false)

    val user: User? by lazy {
        Session.getAccount()?.toUser()
    }

    val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        title.title_tv.text = asset.symbol
        user?.let { user ->
            val info = if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0] else ' '
            avatar.setInfo(info, user.avatarUrl, user.identityNumber)
            qr_avatar.setUrl(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            name.text = user.fullName
            user_id.text = context?.getString(R.string.contact_mixin_id, user.identityNumber)
        }
        address_layout.setOnClickListener {
            context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, asset.publicKey)
            toast(R.string.copy_success)
        }
        key_code.text = asset.publicKey
        if (context!!.isQRCodeFileExists(asset.publicKey)) {
            qr.setImageBitmap(BitmapFactory.decodeFile(context!!.getQRCodePath(asset.publicKey).absolutePath))
        } else {
            qr.post {
                Observable.create<Bitmap> { e ->
                    val b = asset.publicKey.generateQRCode(qr.width)
                    if (b != null) {
                        b.saveQRCode(context!!, asset.publicKey)
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