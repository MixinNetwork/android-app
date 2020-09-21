package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_deposit_qr_bottom.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.extension.capture
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
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

        fun getSize(context: Context) = context.screenWidth() - context.dpToPx(64f)
    }

    private val asset: AssetItem by lazy { requireArguments().getParcelable<AssetItem>(ARGS_ASSET)!! }
    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_deposit_qr_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.title.right_iv.setOnClickListener { dismiss() }
        when (type) {
            TYPE_NAME -> {
                contentView.title.title_tv.text = getString(R.string.account_name)
                contentView.addr_tv.text = asset.destination
            }
            TYPE_TAG -> {
                contentView.title.title_tv.text = getString(R.string.account_memo)
                contentView.addr_tv.text = asset.tag
            }
            else -> {
                contentView.title.title_tv.text = getString(R.string.address)
                contentView.addr_tv.text = asset.destination
            }
        }
        contentView.badge_view.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.badge_view.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.save_iv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (!isAdded) return@launch
                                contentView.content_ll.capture(requireContext())
                            }
                            requireContext().toast(R.string.save_success)
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    },
                    {
                        requireContext().toast(R.string.save_failure)
                    }
                )
        }

        val name = when (type) {
            TYPE_NAME -> "${BuildConfig.VERSION_CODE}-${asset.destination}"
            TYPE_TAG -> "${BuildConfig.VERSION_CODE}-${asset.tag}"
            else -> "${BuildConfig.VERSION_CODE}-${asset.destination}"
        }
        if (requireContext().isQRCodeFileExists(name)) {
            contentView.qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(name).absolutePath))
        } else {
            contentView.qr.post {
                Observable.create<Bitmap> { e ->
                    val code = when (type) {
                        TYPE_NAME -> asset.destination
                        TYPE_TAG -> asset.tag
                        else -> asset.destination
                    }
                    val b = code!!.generateQRCode(getSize(requireContext()))
                    if (b != null) {
                        b.saveQRCode(requireContext(), name)
                        e.onNext(b)
                    }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(stopScope)
                    .subscribe(
                        { r ->
                            contentView.qr.setImageBitmap(r)
                        },
                        {
                        }
                    )
            }
        }
    }
}
