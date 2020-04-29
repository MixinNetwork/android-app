package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import android.view.View
import kotlinx.android.synthetic.main.fragment_deposit_tip_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.Constants.ChainId.EOS_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.needShowReserve
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColor

class DepositTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositTipBottomSheetDialogFragment"

        fun newInstance(assetItem: AssetItem) = DepositTipBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
        }
    }

    private val asset: AssetItem by lazy {
        requireArguments().getParcelable<AssetItem>(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_deposit_tip_bottom_sheet, null)
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        contentView.title_tv.text = getString(R.string.bottom_deposit_title, asset.symbol)
        contentView.asset_icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_icon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.tips_tv.text = getTipsByAsset(asset) + " " + getString(R.string.deposit_confirmation, asset.confirmations)
        contentView.continue_tv.setOnClickListener { dismiss() }
        val reserveTip = if (asset.needShowReserve()) {
            getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
        } else ""
        contentView.warning_tv.text = when (asset.chainId) {
            EOS_CHAIN_ID -> {
                "${getString(R.string.deposit_account_attention, asset.symbol)} $reserveTip"
            }
            else -> {
                "${getString(R.string.deposit_attention)} $reserveTip"
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
        contentView.continue_tv.isEnabled = false
        contentView.continue_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
        countDownTimer = object : CountDownTimer(3000, 1000) {

            override fun onTick(l: Long) {
                contentView.continue_tv.text =
                    requireContext().getString(R.string.got_it_count, l / 1000)
            }

            override fun onFinish() {
                contentView.continue_tv.text = getString(R.string.got_it)
                contentView.continue_tv.isEnabled = true
                contentView.continue_tv.textColor = requireContext().getColor(R.color.white)
            }
        }
        countDownTimer?.start()
    }
}
