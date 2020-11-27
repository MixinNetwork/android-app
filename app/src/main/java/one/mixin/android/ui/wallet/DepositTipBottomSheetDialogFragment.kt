package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ChainId.EOS_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositTipBottomSheetBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.needShowReserve
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColor

@AndroidEntryPoint
class DepositTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositTipBottomSheetDialogFragment"

        fun newInstance(assetItem: AssetItem) = DepositTipBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
        }
    }

    private var _binding: FragmentDepositTipBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _badgeBinding: ViewBadgeCircleImageBinding? = null
    private val badgeBinding get() = requireNotNull(_badgeBinding)

    private val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentDepositTipBottomSheetBinding.bind(View.inflate(context, R.layout.fragment_deposit_tip_bottom_sheet, null))
        _badgeBinding = ViewBadgeCircleImageBinding.bind(binding.assetIcon)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            titleTv.text = getString(R.string.bottom_deposit_title, asset.symbol)
            badgeBinding.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            tipsTv.text = getTipsByAsset(asset) + " " + getString(R.string.deposit_confirmation, asset.confirmations)
            continueTv.setOnClickListener { dismiss() }
            val reserveTip = if (asset.needShowReserve()) {
                getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
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
        _binding = null
        _badgeBinding = null
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
                        requireContext().getString(R.string.got_it_count, l / 1000)
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
