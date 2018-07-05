package one.mixin.android.ui.wallet

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
import kotlinx.android.synthetic.main.fragment_deposit_qr_bottom.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.saveQRCode
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

class DepositQrBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "DepositQrBottomFragment"
        const val ARGS_TYPE = "args_type"

        const val TYPE_NAME = 0
        const val TYPE_MEMO = 1

        fun newInstance(asset: AssetItem, type: Int = TYPE_NAME) = DepositQrBottomFragment().apply {
            arguments = bundleOf(
                ARGS_ASSET to asset,
                ARGS_TYPE to type
            )
        }
    }

    private val asset: AssetItem by lazy { arguments!!.getParcelable<AssetItem>(TransactionsFragment.ARGS_ASSET) }
    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_deposit_qr_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title.left_ib.setOnClickListener { dialog?.dismiss() }
        if (type == TYPE_NAME) {
            contentView.title.setSubTitle(getString(R.string.account_name), asset.accountName!!)
        } else {
            contentView.title.setSubTitle(getString(R.string.account_memo), asset.accountMemo!!)
        }
        contentView.badge_view.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.badge_view.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        val name = if (type == TYPE_NAME) {
            "${BuildConfig.VERSION_CODE}-${asset.accountName}"
        } else {
            "${BuildConfig.VERSION_CODE}-${asset.accountMemo}"
        }
        if (requireContext().isQRCodeFileExists(name)) {
            contentView.qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(name).absolutePath))
        } else {
            contentView.qr.post {
                Observable.create<Bitmap> { e ->
                    val code = if (type == TYPE_NAME) {
                        asset.accountName
                    } else {
                        asset.accountMemo
                    }
                    val b = code!!.generateQRCode(contentView.qr.width)
                    if (b != null) {
                        b.saveQRCode(requireContext(), name)
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