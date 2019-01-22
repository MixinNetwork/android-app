package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
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
import one.mixin.android.extension.displayWidth
import one.mixin.android.extension.dpToPx
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
        const val TYPE_TAG = 1
        const val TYPE_ADDRESS = 2

        fun newInstance(asset: AssetItem, type: Int = TYPE_NAME) = DepositQrBottomFragment().apply {
            arguments = bundleOf(
                ARGS_ASSET to asset,
                ARGS_TYPE to type
            )
        }

        fun getSize(context: Context) = context.displayWidth() - context.dpToPx(64f)
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
        contentView.title.left_ib.setOnClickListener { dismiss() }
        when (type) {
            TYPE_NAME -> contentView.title.setSubTitle(getString(R.string.account_name), asset.accountName!!)
            TYPE_TAG -> contentView.title.setSubTitle(getString(R.string.account_memo), asset.accountTag!!)
            else -> contentView.title.setSubTitle(getString(R.string.address), asset.publicKey!!)
        }
        contentView.badge_view.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.badge_view.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        val name = when (type) {
            TYPE_NAME -> "${BuildConfig.VERSION_CODE}-${asset.accountName}"
            TYPE_TAG -> "${BuildConfig.VERSION_CODE}-${asset.accountTag}"
            else -> "${BuildConfig.VERSION_CODE}-${asset.publicKey}"
        }
        if (requireContext().isQRCodeFileExists(name)) {
            contentView.qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(name).absolutePath))
        } else {
            contentView.qr.post {
                Observable.create<Bitmap> { e ->
                    val code = when (type) {
                        TYPE_NAME -> asset.accountName
                        TYPE_TAG -> asset.accountTag
                        else -> asset.publicKey
                    }
                    val b = code!!.generateQRCode(getSize(requireContext()))
                    if (b != null) {
                        b.saveQRCode(requireContext(), name)
                        e.onNext(b)
                    }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDisposable(scopeProvider)
                    .subscribe({ r ->
                        contentView.qr.setImageBitmap(r)
                    }, {
                    })
            }
        }
    }
}