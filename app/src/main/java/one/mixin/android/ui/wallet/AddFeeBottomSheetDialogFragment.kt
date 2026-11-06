package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddFeeBottomSheetBinding
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AddFeeBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    companion object {
        const val TAG = "AddFeeBottomSheetDialogFragment"
        private const val ARGS_TOKEN = "args_token"
        private const val ARGS_WEB3_TOKEN = "args_web3_token"

        fun newInstance(networkFee: TokenItem) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_TOKEN, networkFee)
            }

        fun newInstance(fee: Web3TokenItem) =
            AddFeeBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_WEB3_TOKEN, fee)
            }
    }

    var onAction: ((type: ActionType, tokenItem: TokenItem) -> Unit)? = null
    var onWeb3Action: ((type: ActionType, tokenItem: Web3TokenItem) -> Unit)? = null

    enum class ActionType {
        SWAP, DEPOSIT
    }

    private val binding by viewBinding(FragmentAddFeeBottomSheetBinding::inflate)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        val tokenItem = requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
        val web3TokenItem = requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
        binding.apply {
            titleTv.text = "${getString(R.string.Add)} ${tokenItem?.symbol ?: web3TokenItem?.symbol ?: ""}"
            subtitleTv.text = tokenItem?.chainName ?: web3TokenItem?.chainName ?: ""
            rightIv.setOnClickListener {
                dismiss()
            }
            if (tokenItem != null) {
                swapTv.text = getString(R.string.swap_token, tokenItem.symbol)
                swapDescTv.text = getString(R.string.swap_token_description, tokenItem.symbol)
                depositTv.text = getString(R.string.deposit_token, tokenItem.symbol)
                transferTv.text = getString(R.string.transfer_token, tokenItem.symbol)
                transferLayout.setOnClickListener {
                    pickSourceWallet(excludeWalletId = null, chainId = tokenItem.chainId, includePrivacyWallet = false) { source ->
                        source?.let { transferToPrivacyWallet(it, tokenItem) }
                    }
                }
                swapLayout.setOnClickListener {
                    onAction?.invoke(ActionType.SWAP, tokenItem)
                    dismiss()
                }
                depositLayout.setOnClickListener {
                    onAction?.invoke(ActionType.DEPOSIT, tokenItem)
                    dismiss()
                }
            } else if (web3TokenItem != null) {
                swapTv.text = getString(R.string.swap_token, web3TokenItem.symbol)
                swapDescTv.text = getString(R.string.swap_token_description, web3TokenItem.symbol)
                depositTv.text = getString(R.string.deposit_token, web3TokenItem.symbol)
                transferTv.text = getString(R.string.transfer_token, web3TokenItem.symbol)
                transferLayout.setOnClickListener {
                    pickSourceWallet(excludeWalletId = web3TokenItem.walletId, chainId = web3TokenItem.chainId, includePrivacyWallet = true) { source ->
                        transferToWeb3Wallet(source, web3TokenItem)
                    }
                }
                swapLayout.setOnClickListener {
                    onWeb3Action?.invoke(ActionType.SWAP, web3TokenItem)
                    dismiss()
                }
                depositLayout.setOnClickListener {
                    onWeb3Action?.invoke(ActionType.DEPOSIT, web3TokenItem)
                    dismiss()
                }
            }
        }
    }

    private fun pickSourceWallet(
        excludeWalletId: String?,
        chainId: String,
        includePrivacyWallet: Boolean,
        onPicked: (WalletItem?) -> Unit,
    ) {
        WalletListBottomSheetDialogFragment.newSourceInstance(excludeWalletId, chainId, includePrivacyWallet)
            .apply { setOnWalletClickListener(onPicked) }
            .show(parentFragmentManager, WalletListBottomSheetDialogFragment.TAG)
    }

    private fun transferToPrivacyWallet(source: WalletItem, target: TokenItem) {
        lifecycleScope.launch {
            val toAddress = runCatching { web3ViewModel.findAndSyncDepositEntry(target.chainId, target.assetId)?.destination }
                .onFailure { ErrorHandler.handleError(it) }
                .getOrNull()
            if (toAddress.isNullOrBlank()) {
                toast(R.string.Alert_Not_Support)
                return@launch
            }
            transferFromWeb3Wallet(source, target.assetId, target.chainId, toAddress)
        }
    }

    private fun transferToWeb3Wallet(source: WalletItem?, target: Web3TokenItem) {
        lifecycleScope.launch {
            val toAddress = web3ViewModel.getAddressesByChainId(target.walletId, target.chainId)?.destination
            if (toAddress.isNullOrBlank()) {
                toast(R.string.Alert_Not_Support)
                return@launch
            }
            if (source != null) {
                transferFromWeb3Wallet(source, target.assetId, target.chainId, toAddress)
                return@launch
            }
            val token = web3ViewModel.findAssetItemById(target.assetId) ?: web3ViewModel.syncAsset(target.assetId)
            if (token == null) {
                toast(R.string.Alert_Not_Support)
                return@launch
            }
            WalletActivity.showInputForAddress(requireActivity(), token, toAddress)
            dismiss()
        }
    }

    private suspend fun transferFromWeb3Wallet(
        source: WalletItem,
        assetId: String,
        chainId: String,
        toAddress: String,
    ) {
        val token = web3ViewModel.web3TokenItemById(source.id, assetId)
        if (token == null) {
            toast(R.string.insufficient_balance)
            return
        }
        val chainToken = web3ViewModel.web3TokenItemById(source.id, chainId)
        val fromAddress = web3ViewModel.getAddressesByChainId(source.id, chainId)?.destination
        if (chainToken == null || fromAddress.isNullOrBlank()) {
            toast(R.string.Alert_Not_Support)
            return
        }
        // The send flow signs with the global wallet, so switch it to the source and let InputFragment switch it back
        val previousWalletId = Web3Signer.currentWalletIdSnapshot()
        val addresses = web3ViewModel.getAddresses(source.id)
        val switched = Web3Signer.setWalletIfCurrent(previousWalletId, source.id, source.category) { queryWalletId ->
            if (queryWalletId == source.id) addresses else emptyList()
        }
        if (!switched) {
            toast(R.string.wallet_changed_try_again)
            return
        }
        startActivity(
            Intent(requireContext(), WalletActivity::class.java).apply {
                putExtra(WalletActivity.DESTINATION, WalletActivity.Destination.Input)
                putExtra(InputFragment.ARGS_FROM_ADDRESS, fromAddress)
                putExtra(InputFragment.ARGS_TO_ADDRESS, toAddress)
                putExtra(InputFragment.ARGS_WEB3_TOKEN, token)
                putExtra(InputFragment.ARGS_WEB3_CHAIN_TOKEN, chainToken)
                putExtra(InputFragment.ARGS_RESTORE_SIGNER_WALLET_ID, previousWalletId)
            },
        )
        dismiss()
    }
}
