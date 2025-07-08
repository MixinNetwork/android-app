package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransferBalanceErrorBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenFeeItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapActivity
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.receive.Web3AddressActivity
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferWeb3BalanceErrorBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    private val binding by viewBinding(FragmentTransferBalanceErrorBottomSheetBinding::inflate)

    companion object {
        const val TAG = "TransferWeb3BalanceErrorBottomSheetDialogFragment"
        const val ARGS_FEE = "args_fee"

        fun newInstance(t: Web3TokenFeeItem) =
            TransferWeb3BalanceErrorBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_FEE, t)
            }
    }

    private val t: Web3TokenFeeItem by lazy {
        requireArguments().getParcelableCompat(ARGS_FEE, Web3TokenFeeItem::class.java)!!
    }

    private val transferViewModel by viewModels<TransferViewModel>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCanceledOnTouchOutside(false)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            setCustomViewHeight(requireActivity().visibleDisplayHeight())
        }
        lifecycleScope.launch {
            val asset = t.token ?: return@launch
            if (asset.assetId in Constants.usdIds) {
                val u = transferViewModel.findTopWeb3UsdBalanceAsset(asset.assetId)
                if (u != null) {
                    binding.errorLayout.isVisible = true
                    binding.bottom.isVisible = false
                    binding.contentTv.text = getString(R.string.swap_usdt_hint, u.symbol)
                    binding.positive.setOnClickListener {
                        SwapActivity.show(
                            requireActivity(),
                            input = u.assetId,
                            output = asset.assetId,
                            null,
                            null,
                            inMixin = false
                        )
                        dismiss()
                    }
                    binding.negative.setOnClickListener {
                        binding.bottom.isVisible = true
                        binding.errorLayout.isVisible = false
                    }
                }
            }
            binding.header.balanceError(asset, t.amount, t.fee)
            binding.content.renderAsset(asset, t.amount, t.fee)
            binding.bottom.setText("${getString(R.string.Add)} ${asset.symbol}")
            binding.bottom.setOnClickListener({
                dismiss()
            }, {
                AddFeeBottomSheetDialogFragment.newInstance(asset)
                    .apply {
                        onWeb3Action = { type, fee ->
                            if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                SwapActivity.show(
                                    requireActivity(),
                                    input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                    output = asset.assetId,
                                    null,
                                    null,
                                    inMixin = false
                                )
                                this@TransferWeb3BalanceErrorBottomSheetDialogFragment.dismiss()
                            } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                Web3AddressActivity.show(requireActivity(), JsSigner.evmAddress)
                                this@TransferWeb3BalanceErrorBottomSheetDialogFragment.dismiss()
                            }
                        }
                    }.showNow(parentFragmentManager, AddFeeBottomSheetDialogFragment.TAG)
            }, {})
        }
    }
}
