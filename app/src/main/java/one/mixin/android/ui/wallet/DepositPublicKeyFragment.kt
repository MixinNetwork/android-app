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
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositKeyBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.databinding.ViewTitleBinding
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

@AndroidEntryPoint
class DepositPublicKeyFragment : DepositFragment() {

    companion object {
        const val TAG = "DepositPublicKeyFragment"
    }

    private var _binding: FragmentDepositKeyBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _qrBinding: ViewBadgeCircleImageBinding? = null
    private val qrBinding get() = requireNotNull(_qrBinding)

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDepositKeyBinding.inflate(layoutInflater, container, false).apply { root.setOnClickListener { } }
        _titleBinding = ViewTitleBinding.bind(binding.title)
        _qrBinding = ViewBadgeCircleImageBinding.bind(binding.qrAvatar)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.apply {
            leftIb.setOnClickListener { activity?.onBackPressed() }
            rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.DEPOSIT) }
        }
        binding.apply {
            title.setSubTitle(getString(R.string.filters_deposit), asset.symbol)
            qrBinding.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            qrAvatar.setBorder()
            copyTv.setOnClickListener {
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.destination))
                context?.toast(R.string.copy_success)
            }
            keyCode.text = asset.destination
            confirmTv.text = getTipsByAsset(asset) + " " + getString(R.string.deposit_confirmation, asset.confirmations)
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
            } else ""
            warningTv.text = "${getString(R.string.deposit_attention)} $reserveTip"
            qrFl.setOnClickListener {
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
        }
        showTip()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _qrBinding = null
    }
}
