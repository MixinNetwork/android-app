package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_asset_key_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AssetKeyBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetKeyBottomSheetDialogFragment"

        fun newInstance(asset: AssetItem) = AssetKeyBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, asset)
        }
    }

    private val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_asset_key_bottom, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.title_view.rightIv.setOnClickListener { dismiss() }
        contentView.title_view.titleTv.text = asset.name
        contentView.title_view.showBadgeCircleView(asset)
        contentView.symbol_as_tv.text = asset.symbol
        contentView.chain_as_tv.text = asset.chainName
        contentView.asset_key_as_tv.text = asset.assetKey
    }
}
