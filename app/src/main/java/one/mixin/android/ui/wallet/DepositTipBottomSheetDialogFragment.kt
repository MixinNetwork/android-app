package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_deposit_tip_bottom_sheet.view.*
import one.mixin.android.Constants.Account.PREF_SHOW_DEPOSIT_TIP_CHAIN_SET
import one.mixin.android.Constants.ChainId.EOS_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.putStringSet
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

class DepositTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositTipBottomSheetDialogFragment"

        fun newInstance(assetItem: AssetItem) = DepositTipBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
        }
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_deposit_tip_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_tv.text = getString(R.string.bottom_deposit_title, asset.symbol)
        contentView.tips_tv.text = getTipsByAsset(asset) + getString(R.string.deposit_confirmation, asset.confirmations)
        contentView.continue_tv.setOnClickListener { dismiss() }
        contentView.hide_tv.setOnClickListener {
            var depositChainSet = defaultSharedPreferences.getStringSet(PREF_SHOW_DEPOSIT_TIP_CHAIN_SET, null)
            val chainId = asset.chainId
            if (depositChainSet == null) {
                depositChainSet = setOf(chainId)
            } else {
                depositChainSet.add(chainId)
            }
            defaultSharedPreferences.putStringSet(PREF_SHOW_DEPOSIT_TIP_CHAIN_SET, depositChainSet)
            dismiss()
        }
        contentView.warning_tv.text = if (asset.chainId == EOS_CHAIN_ID) {
            getString(R.string.deposit_account_attention, asset.symbol)
        } else {
            getString(R.string.deposit_attention)
        }
    }
}
