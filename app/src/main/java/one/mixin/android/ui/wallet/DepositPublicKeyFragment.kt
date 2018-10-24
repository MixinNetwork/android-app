package one.mixin.android.ui.wallet

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_deposit_key.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser

class DepositPublicKeyFragment : Fragment() {
    companion object {
        const val TAG = "DepositPublicKeyFragment"

        fun newInstance(asset: AssetItem): DepositPublicKeyFragment {
            val f = DepositPublicKeyFragment()
            val b = Bundle()
            b.putParcelable(ARGS_ASSET, asset)
            f.arguments = b
            return f
        }
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_deposit_key, container, false).apply { this.setOnClickListener { } }

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
            avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
            qr_avatar.setUrl(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            name.text = user.fullName
            user_id.text = context?.getString(R.string.contact_mixin_id, user.identityNumber)
        }
        key_code.setOnClickListener {
            context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, asset.publicKey)
            context?.toast(R.string.copy_success)
        }
        key_code.text = asset.publicKey
        confirm_tv.text = getString(R.string.wallet_block_confirmations, asset.confirmations)
        if (asset.publicKey != null) {
            if (context!!.isQRCodeFileExists(asset.publicKey!!)) {
                qr.setImageBitmap(BitmapFactory.decodeFile(context!!.getQRCodePath(asset.publicKey!!).absolutePath))
            } else {
                qr.post {
                    Observable.create<Bitmap> { e ->
                        val b = asset.publicKey!!.generateQRCode(qr.width)
                        if (b != null) {
                            b.saveQRCode(context!!, asset.publicKey!!)
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