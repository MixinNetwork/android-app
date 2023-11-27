package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Build
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
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositQrBottomBinding
import one.mixin.android.extension.capture
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositQrBottomFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositQrBottomFragment"
        const val ARGS_TYPE = "args_type"
        const val ARGS_DEPOSIT_ENTRY = "args_deposit_entry"
        const val ARGS_SELECTED_DESTINATION = "args_selected_destination"

        const val TYPE_TAG = 0
        const val TYPE_ADDRESS = 1

        fun newInstance(
            asset: TokenItem,
            depositEntry: DepositEntry,
            type: Int,
            selectedDestination: String?,
        ) = DepositQrBottomFragment().apply {
            arguments =
                bundleOf(
                    ARGS_ASSET to asset,
                    ARGS_DEPOSIT_ENTRY to depositEntry,
                    ARGS_TYPE to type,
                    ARGS_SELECTED_DESTINATION to selectedDestination,
                )
        }
    }

    private val binding by viewBinding(FragmentDepositQrBottomBinding::inflate)

    private val asset: TokenItem by lazy { requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)!! }
    private val depositEntry: DepositEntry by lazy { requireArguments().getParcelableCompat(ARGS_DEPOSIT_ENTRY, DepositEntry::class.java)!! }
    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }
    private val selectedDestination: String? by lazy { requireArguments().getString(ARGS_SELECTED_DESTINATION) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            title.rightIv.setOnClickListener { dismiss() }
            when (type) {
                TYPE_TAG -> {
                    title.titleTv.text = getString(R.string.withdrawal_memo)
                    addrTv.text = depositEntry.tag
                }
                else -> {
                    title.titleTv.text = getString(R.string.Address)
                    addrTv.text = selectedDestination ?: depositEntry.destination
                }
            }
            badgeView.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }

            saveIv.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(
                        *mutableListOf(android.Manifest.permission.CAMERA).apply {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }.toTypedArray(),
                    )
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (!isAdded) return@launch
                                    val path = contentLl.capture(requireContext()).getOrNull()
                                    withContext(Dispatchers.Main) {
                                        if (path.isNullOrBlank()) {
                                            toast(getString(R.string.Save_failure))
                                        } else {
                                            toast(R.string.Save_success)
                                        }
                                    }
                                }
                            } else {
                                requireContext().openPermissionSetting()
                            }
                        },
                        {
                            toast(R.string.Save_failure)
                        },
                    )
            }

            qr.post {
                Observable.create<Pair<Bitmap, Int>?> { e ->
                    val code =
                        when (type) {
                            TYPE_TAG -> depositEntry.tag
                            else -> selectedDestination ?: depositEntry.destination
                        }
                    val r = code?.generateQRCode(qr.width)
                    r?.let { e.onNext(it) }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(stopScope)
                    .subscribe(
                        { r ->
                            badgeView.layoutParams =
                                badgeView.layoutParams.apply {
                                    width = r.second
                                    height = r.second
                                }
                            qr.setImageBitmap(r.first)
                        },
                        {
                        },
                    )
            }
        }
    }
}
