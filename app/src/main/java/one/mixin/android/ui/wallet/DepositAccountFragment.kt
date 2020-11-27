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
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositAccountBinding
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
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_TAG
import one.mixin.android.vo.needShowReserve

@AndroidEntryPoint
class DepositAccountFragment : DepositFragment() {

    companion object {
        const val TAG = "DepositAccountFragment"
    }

    private var _binding: FragmentDepositAccountBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _nameQrBinding: ViewBadgeCircleImageBinding? = null
    private val nameQrBinding get() = requireNotNull(_nameQrBinding)
    private var _memoQrBinding: ViewBadgeCircleImageBinding? = null
    private val memoQrBinding get() = requireNotNull(_memoQrBinding)

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDepositAccountBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        _titleBinding = ViewTitleBinding.bind(binding.title)
        _nameQrBinding = ViewBadgeCircleImageBinding.bind(binding.accountNameQrAvatar)
        _memoQrBinding = ViewBadgeCircleImageBinding.bind(binding.accountMemoQrAvatar)
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
            nameQrBinding.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            accountNameQrAvatar.setBorder()
            memoQrBinding.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            accountMemoQrAvatar.setBorder()
            accountNameKeyCode.text = asset.destination
            accountMemoKeyCode.text = asset.tag
            tipTv.text = getTipsByAsset(asset) + " " + getString(R.string.deposit_confirmation, asset.confirmations)
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
            } else ""
            warningTv.text = "${getString(R.string.deposit_account_attention, asset.symbol)} $reserveTip"
            accountNameQrFl.setOnClickListener {
                DepositQrBottomFragment.newInstance(asset, TYPE_ADDRESS).show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            accountMemoQrFl.setOnClickListener {
                DepositQrBottomFragment.newInstance(asset, TYPE_TAG).show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            accountNameCopyTv.setOnClickListener {
                context?.getClipboardManager()
                    ?.setPrimaryClip(ClipData.newPlainText(null, asset.destination))
                context?.toast(R.string.copy_success)
            }
            accountMemoCopyTv.setOnClickListener {
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.tag))
                context?.toast(R.string.copy_success)
            }

            showQR(accountNameQr, "${BuildConfig.VERSION_CODE}-${asset.destination}", asset.destination)
            if (!asset.tag.isNullOrEmpty()) {
                showQR(accountMemoQr, "${BuildConfig.VERSION_CODE}-${asset.tag}", asset.tag!!)
            }
        }
        showTip()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _titleBinding = null
        _nameQrBinding = null
        _memoQrBinding = null
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
}
