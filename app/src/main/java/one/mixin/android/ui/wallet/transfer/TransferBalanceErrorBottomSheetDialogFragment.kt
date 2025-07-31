package one.mixin.android.ui.wallet.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTransferBalanceErrorBottomSheetBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.home.web3.swap.SwapActivity
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.DepositFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class TransferBalanceErrorBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    private val binding by viewBinding(FragmentTransferBalanceErrorBottomSheetBinding::inflate)

    companion object {
        const val TAG = "TransferBalanceErrorBottomSheetDialogFragment"
        const val ARGS_TRANSFER = "args_transfer"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferBalanceErrorBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TRANSFER, t)
            }
    }

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TRANSFER, AssetBiometricItem::class.java)!!
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
        binding.root.roundTopOrBottom(8.dp.toFloat(), true, false)
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_wallet_privacy_white)
        drawable?.setBounds(0, 0, 22.dp, 22.dp)
        binding.walletTv.compoundDrawablePadding = 4.dp
        binding.walletTv.setCompoundDrawablesRelative(drawable, null, null, null)
        lifecycleScope.launch {
            val asset = t.asset?:return@launch
            val tokenExtra = transferViewModel.findTokensExtra(asset.assetId)
            val feeExtra = if (t is WithdrawBiometricItem) {
                (t as WithdrawBiometricItem).fee?.token?.assetId?.let {
                    transferViewModel.findTokensExtra(it)
                }
            } else {
                null
            }
            if (asset.assetId in Constants.usdIds) {
                val u = transferViewModel.findTopUsdBalanceAsset(asset.assetId)
                if (u != null) {
                    binding.errorLayout.isVisible = true
                    binding.bottom.isVisible = false
                    binding.contentTv.text = getString(R.string.swap_usdt_hint, u.symbol)
                    binding.positive.setOnClickListener {
                        SwapActivity.show(requireActivity(), input = u.assetId, output = asset.assetId, null, null)
                        dismiss()
                    }
                    binding.negative.setOnClickListener {
                        binding.bottom.isVisible = true
                        binding.errorLayout.isVisible = false
                    }
                }
            }
            if (t is WithdrawBiometricItem && (t as WithdrawBiometricItem).isBalanceEnough(tokenExtra?.balance, feeExtra?.balance) == 3) {
                val fee = (t as WithdrawBiometricItem).fee?.token!!
                binding.bottom.setText("${getString(R.string.Add)} ${fee.symbol}")
                binding.bottom.setOnClickListener({
                    dismiss()
                },{
                    AddFeeBottomSheetDialogFragment.newInstance(fee)
                        .apply {
                            onAction = { type, fee ->
                                if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                    SwapActivity.show(
                                        requireActivity(),
                                        input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                        output = fee.assetId,
                                        null,
                                        null
                                    )
                                    this@TransferBalanceErrorBottomSheetDialogFragment.dismiss()
                                } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                    navTo(DepositFragment.newInstance(fee), DepositFragment.TAG)
                                    this@TransferBalanceErrorBottomSheetDialogFragment.dismiss()
                                }
                            }
                        }.showNow(
                            parentFragmentManager,
                            AddFeeBottomSheetDialogFragment.TAG
                        )
                },{})
            } else {
                binding.bottom.setText("${getString(R.string.Add)} ${asset.symbol}")
                binding.bottom.setOnClickListener({
                    dismiss()
                },{
                    AddFeeBottomSheetDialogFragment.newInstance(asset)
                        .apply {
                            onAction = { type, asset ->
                                if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                    SwapActivity.show(
                                        requireActivity(),
                                        input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                        output = asset.assetId,
                                        null,
                                        null
                                    )
                                    this@TransferBalanceErrorBottomSheetDialogFragment.dismiss()
                                } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                    navTo(DepositFragment.newInstance(asset), DepositFragment.TAG)
                                    this@TransferBalanceErrorBottomSheetDialogFragment.dismiss()
                                }
                            }
                        }.showNow(parentFragmentManager,
                            AddFeeBottomSheetDialogFragment.TAG)

                },{})
            }
            binding.header.balanceError(t, tokenExtra, feeExtra)
            binding.content.renderAsset(t, tokenExtra)
        }
    }
}
