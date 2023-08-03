package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentOrderPreviewBottomBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class OrderPreviewBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "OrderPreviewBottomSheetDialogFragment"

        fun newInstance(asset: AssetItem) = OrderPreviewBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, asset)
        }
    }

    private val binding by viewBinding(FragmentOrderPreviewBottomBinding::inflate)

    private val asset: AssetItem by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_ASSET, AssetItem::class.java)) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            chainTv.text = requireNotNull(getChainName(asset.chainId, asset.chainName, asset.assetKey)) {
                "required chain name must not be null"
            }
            closeIv.setOnClickListener { dismiss() }
            actionTv.setOnClickListener { dismiss() }
            assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            valueTv.text = "+51.123 USDC"
            payWith.tail.text = "EMARITES NBD BANK"
            price.tail.text = "0.995 USD / USDC"
            purchase.tail.text = "48.78 USD"
            gatewayFee.tail.text = "1.123 USD"
            networkFee.tail.text = "0 USD"
            total.tail.text = "50 USD"
        }
    }
}
