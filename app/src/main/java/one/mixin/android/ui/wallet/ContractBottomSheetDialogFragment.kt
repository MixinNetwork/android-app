package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.fragment_contract_bottom.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

class ContractBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ContractBottomSheetDailogFragment"

        fun newInstance(asset: AssetItem) = ContractBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, asset)
        }
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_contract_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.title_view.title_tv.text = asset.name
        contentView.title_view.circle_iv.visibility = VISIBLE
        contentView.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.symbol_as_tv.text = asset.symbol
        contentView.chain_as_tv.text = asset.chainName
        contentView.contract_as_tv.text = asset.assetKey
        contentView.contract_as_tv.setOnLongClickListener {
            requireContext().getClipboardManager().primaryClip = ClipData.newPlainText(null, asset.assetKey)
            requireContext().toast(R.string.copy_success)
            return@setOnLongClickListener true
        }
    }
}