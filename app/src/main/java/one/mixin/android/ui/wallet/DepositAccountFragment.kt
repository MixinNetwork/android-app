package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositAccountBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
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

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepositAccountBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
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
            title.setSubTitle(getString(R.string.filters_deposit), asset.symbol)
            accountNameQrAvatar.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            accountNameQrAvatar.setBorder()
            accountMemoQrAvatar.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            accountMemoQrAvatar.setBorder()
            accountNameKeyCode.text = asset.destination
            accountMemoKeyCode.text = asset.tag
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, "${asset.reserve} ${asset.symbol}").highLight(requireContext(), "${asset.reserve} ${asset.symbol}")
            } else SpannableStringBuilder()
            val confirmation = getString(R.string.deposit_confirmation, asset.confirmations).highLight(requireContext(), asset.confirmations.toString())
            warningTv.text = getString(R.string.deposit_memo_notice, asset.symbol)
            tipTv.text = buildBulletLines(requireContext(), SpannableStringBuilder(getTipsByAsset(asset)), confirmation, reserveTip)
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

            showQR(accountNameQr, accountNameQrAvatar, asset.destination)
            if (!asset.tag.isNullOrEmpty()) {
                showQR(accountMemoQr, accountMemoQrAvatar, asset.tag!!)
            }
        }
        alertDialogBuilder()
            .setTitle(R.string.notice)
            .setCancelable(false)
            .setMessage(getString(R.string.deposit_memo_notice, asset.symbol))
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                binding.warningFl.isVisible = true
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showQR(qr: ImageView, logo: View, code: String) {
        qr.post {
            Observable.create<Pair<Bitmap, Int>> { e ->
                val result = code.generateQRCode(qr.width)
                e.onNext(result)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(scopeProvider)
                .subscribe(
                    { r ->
                        logo.layoutParams = logo.layoutParams.apply {
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
