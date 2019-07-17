package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_deposit_account.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.BuildConfig
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
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_TAG

class DepositAccountFragment : DepositFragment() {

    companion object {
        const val TAG = "DepositAccountFragment"
    }

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_deposit_account, container, false).apply { this.setOnClickListener { } }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        title.right_animator.setOnClickListener { context?.openUrl(Constants.HelpLink.DEPOSIT_ACCOUNT) }
        title.setSubTitle(getString(R.string.filters_deposit), asset.symbol)
        account_name_qr_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        account_name_qr_avatar.setBorder()
        account_name_qr_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        account_memo_qr_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        account_memo_qr_avatar.setBorder()
        account_memo_qr_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        account_name_key_code.text = asset.accountName
        account_memo_key_code.text = asset.accountTag
        tip_tv.text = getTipsByAsset(asset) + getString(R.string.deposit_confirmation, asset.confirmations)
        warning_tv.text = getString(R.string.deposit_account_attention, asset.symbol)
        account_name_qr_fl.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
        account_memo_qr_fl.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset, TYPE_TAG).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
        account_name_copy_tv.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.accountName))
            context?.toast(R.string.copy_success)
        }
        account_memo_copy_tv.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.accountTag))
            context?.toast(R.string.copy_success)
        }

        asset.accountName?.let {
            showQR(account_name_qr, "${BuildConfig.VERSION_CODE}-${asset.accountName}", it)
        }
        asset.accountTag?.let {
            showQR(account_memo_qr, "${BuildConfig.VERSION_CODE}-${asset.accountTag}", it)
        }
        showTip()
    }

    private fun showQR(qr: ImageView, name: String, code: String) {
        if (requireContext().isQRCodeFileExists(name)) {
            qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(name).absolutePath))
        } else {
            qr.post {
                Observable.create<Bitmap> { e ->
                    val b = code.generateQRCode(DepositQrBottomFragment.getSize(requireContext()))
                    if (b != null) {
                        b.saveQRCode(requireContext(), name)
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