package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.os.Bundle
import android.text.SpannableStringBuilder
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
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
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

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepositKeyBinding.inflate(layoutInflater, container, false).apply { root.setOnClickListener { } }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.apply {
                leftIb.setOnClickListener { activity?.onBackPressed() }
                rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.DEPOSIT) }
            }
            title.setSubTitle(getString(R.string.Deposit), asset.symbol)
            qrAvatar.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            qrAvatar.setBorder()
            copyIv.setOnClickListener {
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, asset.destination))
                toast(R.string.copied_to_clipboard)
            }
            keyCode.text = asset.destination
            val confirmation = requireContext().resources.getQuantityString(R.plurals.deposit_confirmation, asset.confirmations, asset.confirmations)
                .highLight(requireContext(), asset.confirmations.toString())
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, "${asset.reserve} ${asset.symbol}")
                    .highLight(requireContext(), "${asset.reserve} ${asset.symbol}")
            } else SpannableStringBuilder()
            tipTv.text = buildBulletLines(
                requireContext(),
                SpannableStringBuilder(getTipsByAsset(asset)),
                confirmation,
                reserveTip
            )
            qrFl.setOnClickListener {
                DepositQrBottomFragment.newInstance(asset, TYPE_ADDRESS).show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            qr.post {
                Observable.create<Pair<Bitmap, Int>> { e ->
                    val r = asset.destination.generateQRCode(qr.width)
                    e.onNext(r)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qrAvatar.layoutParams = qrAvatar.layoutParams.apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
