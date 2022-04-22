package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositQrBottomBinding
import one.mixin.android.extension.capture
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositQrBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "DepositQrBottomFragment"
        const val ARGS_TYPE = "args_type"

        const val TYPE_TAG = 0
        const val TYPE_ADDRESS = 1

        fun newInstance(asset: AssetItem, type: Int) = DepositQrBottomFragment().apply {
            arguments = bundleOf(
                ARGS_ASSET to asset,
                ARGS_TYPE to type
            )
        }
    }

    private val binding by viewBinding(FragmentDepositQrBottomBinding::inflate)

    private val asset: AssetItem by lazy { requireArguments().getParcelable(ARGS_ASSET)!! }
    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            title.rightIv.setOnClickListener { dismiss() }
            when (type) {
                TYPE_TAG -> {
                    title.titleTv.text = getString(R.string.withdrawal_memo)
                    addrTv.text = asset.tag
                }
                else -> {
                    title.titleTv.text = getString(R.string.Address)
                    addrTv.text = asset.destination
                }
            }
            badgeView.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }

            saveIv.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (!isAdded) return@launch
                                    contentLl.capture(requireContext())
                                }
                                toast(R.string.Save_success)
                            } else {
                                requireContext().openPermissionSetting()
                            }
                        },
                        {
                            toast(R.string.Save_failure)
                        }
                    )
            }

            qr.post {
                Observable.create<Pair<Bitmap, Int>?> { e ->
                    val code = when (type) {
                        TYPE_TAG -> asset.tag
                        else -> asset.destination
                    }
                    val r = code?.generateQRCode(qr.width)
                    r?.let { e.onNext(it) }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(stopScope)
                    .subscribe(
                        { r ->
                            badgeView.layoutParams = badgeView.layoutParams.apply {
                                width = r.second
                                height = r.second
                            }
                            qr.setImageBitmap(r.first)
                        },
                        {
                        }
                    )
            }
        }
    }
}
