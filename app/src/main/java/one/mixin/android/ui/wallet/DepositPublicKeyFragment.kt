package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_deposit_key.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_ADDRESS
import one.mixin.android.vo.needShowReserve

class DepositPublicKeyFragment : DepositFragment() {

    companion object {
        const val TAG = "DepositPublicKeyFragment"
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_deposit_key, container, false).apply { this.setOnClickListener { } }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        title.right_animator.setOnClickListener { context?.openUrl(Constants.HelpLink.DEPOSIT) }
        title.setSubTitle(getString(R.string.filters_deposit), asset.symbol)
        qr_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        qr_avatar.setBorder()
        qr_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        copy_tv.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.destination))
            context?.toast(R.string.copy_success)
        }
        key_code.text = asset.destination
        confirm_tv.text = getTipsByAsset(asset) + getString(R.string.deposit_confirmation, asset.confirmations)
        val reserveTip = if (asset.needShowReserve()) {
            getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
        } else ""
        warning_tv.text = "${getString(R.string.deposit_attention)} $reserveTip"
        qr_fl.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset, TYPE_ADDRESS).show(parentFragmentManager, DepositQrBottomFragment.TAG)
        }
        if (requireContext().isQRCodeFileExists(asset.destination)) {
            qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(asset.destination).absolutePath))
        } else {
            qr.post {
                Observable.create<Bitmap> { e ->
                    val b = asset.destination.generateQRCode(qr.width)
                    if (b != null) {
                        b.saveQRCode(requireContext(), asset.destination)
                        e.onNext(b)
                    }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qr.setImageBitmap(r)
                        },
                        {
                        }
                    )
            }
        }
        showTip()
    }
}
