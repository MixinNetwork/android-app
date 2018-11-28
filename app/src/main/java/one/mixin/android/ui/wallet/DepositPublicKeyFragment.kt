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
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_ADDRESS
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

class DepositPublicKeyFragment : Fragment() {
    companion object {
        const val TAG = "DepositPublicKeyFragment"
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_deposit_key, container, false).apply { this.setOnClickListener { } }

    val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        title.setSubTitle(getString(R.string.filters_deposit), asset.symbol)
        qr_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        qr_avatar.setBorder()
        qr_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        copy_tv.setOnClickListener {
            context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, asset.publicKey)
            context?.toast(R.string.copy_success)
        }
        key_code.text = asset.publicKey
        confirm_tv.text = getString(R.string.wallet_block_confirmations, asset.confirmations)
        qr_fl.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset, TYPE_ADDRESS).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
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
                        }, {
                        })
                }
            }
        }
    }
}