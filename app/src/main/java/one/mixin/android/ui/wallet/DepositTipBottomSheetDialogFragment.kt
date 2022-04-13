package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ChainId.EOS_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositTipBottomSheetBinding
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.textColor
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.needShowReserve
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositTipBottomSheetDialogFragment"

        fun newInstance(assetItem: AssetItem) = DepositTipBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
        }
    }

    private val binding by viewBinding(FragmentDepositTipBottomSheetBinding::inflate)

    private val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            titleTv.text = getString(R.string.deposit_title, asset.symbol)
            binding.assetIcon.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            tipsTv.text = getTipsByAsset(asset) + " " + requireContext().resources.getQuantityString(R.plurals.deposit_confirmation, asset.confirmations, asset.confirmations)
            continueTv.setOnClickListener { dismiss() }
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, "${asset.reserve} ${asset.symbol}")
            } else ""
            warningTv.text = when (asset.chainId) {
                EOS_CHAIN_ID -> {
                    "${getString(R.string.deposit_account_attention, asset.symbol)} $reserveTip"
                }
                else -> {
                    "${getString(R.string.deposit_attention)} $reserveTip"
                }
            }
        }

        startCountDown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private var countDownTimer: CountDownTimer? = null

    private fun startCountDown() {
        countDownTimer?.cancel()
        binding.apply {
            continueTv.isEnabled = false
            continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
            countDownTimer = object : CountDownTimer(3000, 1000) {

                override fun onTick(l: Long) {
                    continueTv.text =
                        requireContext().getString(R.string.got_it_count_down, l / 1000)
                }

                override fun onFinish() {
                    continueTv.text = getString(R.string.got_it)
                    continueTv.isEnabled = true
                    continueTv.textColor = requireContext().getColor(R.color.white)
                }
            }
            countDownTimer?.start()
        }
    }
}
