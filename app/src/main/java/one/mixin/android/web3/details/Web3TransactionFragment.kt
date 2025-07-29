package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.databinding.ViewWalletWeb3TransactionBottomBinding
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showGasCheckAndBrowserBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.details.Web3TransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.widget.BottomSheet
import org.web3j.crypto.TransactionDecoder
import org.web3j.utils.Numeric
import javax.inject.Inject

@AndroidEntryPoint
class Web3TransactionFragment : BaseFragment(R.layout.fragment_web3_transaction) {
    companion object {
        const val TAG = "Web3TransactionFragment"
        const val ARGS_TRANSACTION = "args_transaction"
        const val ARGS_CHAIN = "args_chain"
        const val ARGS_WALLET = "args_wallet"

        fun newInstance(
            transaction: Web3TransactionItem,
            chain: String,
            web3Token: Web3TokenItem,
            wallet: Web3Wallet,
        ) = Web3TransactionFragment().withArgs {
            putParcelable(ARGS_TRANSACTION, transaction)
            putString(ARGS_CHAIN, chain)
            putParcelable(ARGS_TOKEN, web3Token)
            putParcelable(ARGS_WALLET, wallet)
        }
    }

    private val binding by viewBinding(FragmentWeb3TransactionBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val token: Web3TokenItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java)!!
    }

    private val wallet: Web3Wallet by lazy {
        requireArguments().getParcelableCompat(ARGS_WALLET, Web3Wallet::class.java)!!
    }

    private val transaction by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_TRANSACTION,
                Web3TransactionItem::class.java
            )
        )
    }

    private val chain by lazy {
        requireNotNull(requireArguments().getString(ARGS_CHAIN))
    }
    
    @Inject
    lateinit var jobManager: MixinJobManager
    private var refreshJob: Job? = null
    lateinit var rpc: Rpc

    private fun formatAmountWithSign(amount: String, positive: Boolean): String {
        return if (positive) {
            if (amount.startsWith("+")) amount else "+$amount"
        } else {
            if (amount.startsWith("-")) amount else "-$amount"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightIb.setOnClickListener {
            showBottom()
        }
        binding.titleView.rightExtraIb.visibility = View.VISIBLE
        binding.titleView.rightExtraIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightExtraIb.setOnClickListener {
            context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
        }
        binding.root.isClickable = true
        binding.apply {
            if (wallet.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                wallet.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                wallet.category == WalletCategory.WATCH_ADDRESS.value) {
                titleView.setSubTitle(getString(R.string.Transaction), wallet.name)
            } else {
                titleView.setSubTitle(getString(R.string.Transaction), getString(R.string.Common_Wallet))
            }
            spamLl.isVisible = transaction.isNotVerified()
            transactionHashTv.text = transaction.transactionHash
            val amountColor = if (transaction.status == TransactionStatus.PENDING.value || transaction.status == TransactionStatus.NOT_FOUND.value || transaction.status == TransactionStatus.FAILED.value) {
                requireContext().colorFromAttribute(R.attr.text_assist)
            } else if (transaction.transactionType == TransactionType.TRANSFER_OUT.value) {
                requireContext().getColor(R.color.wallet_pink)
            } else if (transaction.transactionType == TransactionType.TRANSFER_IN.value) {
                requireContext().getColor(R.color.wallet_green)
            } else {
                requireContext().colorFromAttribute(R.attr.text_primary)
            }

            val symbolColor = requireContext().colorFromAttribute(R.attr.text_primary)

            val mainAmount = transaction.getFormattedAmount()

            valueTv.text = when (transaction.transactionType) {
                TransactionType.SWAP.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Swap)
                }
                TransactionType.UNKNOWN.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Unknown)
                }
                TransactionType.APPROVAL.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Approval)
                }
                else -> {
                    buildAmountSymbol(
                        requireContext(),
                        formatAmountWithSign(mainAmount, transaction.transactionType == TransactionType.TRANSFER_IN.value),
                        when (transaction.transactionType) {
                            TransactionType.TRANSFER_OUT.value -> transaction.sendAssetSymbol ?: ""
                            TransactionType.APPROVAL.value -> transaction.sendAssetSymbol ?: ""
                            TransactionType.TRANSFER_IN.value -> transaction.receiveAssetSymbol ?: ""
                            else -> ""
                        },
                        amountColor, symbolColor
                    )
                }
            }

            when (transaction.status) {
                TransactionStatus.SUCCESS.value -> {
                    status.text = getString(R.string.Completed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_green))
                    status.setBackgroundResource(R.drawable.bg_status_success)
                }

                TransactionStatus.PENDING.value -> {
                    status.text = getString(R.string.Pending)
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }

                TransactionStatus.FAILED.value -> {
                    status.text = getString(R.string.Failed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                TransactionStatus.NOT_FOUND.value -> {
                    status.text = getString(R.string.Expired)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                else -> {
                    status.text = transaction.status
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }
            }

            val fromAddress = transaction.getFromAddress()
            val toAddress = transaction.getToAddress()
            
            when  {
                transaction.status == TransactionStatus.NOT_FOUND.value -> {
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
                transaction.status == TransactionStatus.FAILED.value-> {
                    valueTv.isVisible = false
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    fromTv.text = fromAddress
                    fromLl.isVisible = true
                    toLl.isVisible = false
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    toTv.text = toAddress
                    fromLl.isVisible = false
                    toLl.isVisible = true
                }
                transaction.transactionType ==TransactionType.APPROVAL.value -> {
                    toTv.text = toAddress
                    fromLl.isVisible = false
                    toLl.isVisible = true
                }
                transaction.transactionType ==TransactionType.UNKNOWN.value -> {
                    valueTv.isVisible = false
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
                else -> {
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
            }

            when {
                transaction.status == TransactionStatus.NOT_FOUND.value || transaction.status == TransactionStatus.FAILED.value || transaction.status == TransactionStatus.PENDING.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_contract)
                }

                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    avatar.bg.loadImage(transaction.sendAssetIconUrl, R.drawable.ic_avatar_place_holder)
                }

                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    avatar.bg.loadImage(transaction.receiveAssetIconUrl, R.drawable.ic_avatar_place_holder)
                }

                transaction.transactionType == TransactionType.SWAP.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_swap)
                }

                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_approval)
                }

                else -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_unknown)
                }
            }

            avatar.setOnClickListener {
                tokenClick(transaction)
            }

            avatar.badge.isVisible = false

            dateTv.text = transaction.transactionAt.fullDate()
            feeLl.isVisible = transaction.transactionType != TransactionType.TRANSFER_IN.value && transaction.fee.isNotEmpty()
            feeTv.text = "${transaction.fee} ${transaction.chainSymbol ?: ""}"
            statusLl.isVisible = false
            
            networkLl.isVisible = true
            networkTv.text = token.chainName
            
            typeLl.isVisible = true
            typeTv.text = when (transaction.transactionType) {
                TransactionType.TRANSFER_OUT.value -> getString(R.string.Send)
                TransactionType.TRANSFER_IN.value -> getString(R.string.Receive)
                TransactionType.APPROVAL.value -> getString(R.string.Approval)
                TransactionType.SWAP.value -> getString(R.string.Swap)
                else -> transaction.transactionType
            }

            if (transaction.transactionType == TransactionType.SWAP.value && transaction.senders.isNotEmpty()) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesContainer.setContent {
                    AssetChangesList(
                        status = transaction.status,
                        senders = transaction.senders,
                        receivers = transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(JsSigner.currentWalletId, assetId)
                        }
                    )
                }
            } else if (transaction.transactionType == TransactionType.APPROVAL.value) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesTitle.setText(R.string.TOKEN_ACCESS_APPROVAL)
                
                assetChangesContainer.setContent {
                    AssetChangesList(
                        status = transaction.status,
                        senders = transaction.senders,
                        receivers = transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(JsSigner.currentWalletId, assetId)
                        },
                        approvals = transaction.approvals,
                    )
                }
            } else {
                assetChangesLl.visibility = View.GONE
            }
            
            if (transaction.status == TransactionStatus.PENDING.value
                && transaction.chainId != Constants.ChainId.SOLANA_CHAIN_ID) {
                lifecycleScope.launch {
                    val pendingRawTx = web3ViewModel.getPendingRawTransactions(transaction.chainId)
                        .firstOrNull { it.hash == transaction.transactionHash }
                    
                    val shouldShowActions = pendingRawTx != null
                    
                    if (shouldShowActions) {
                        actions.isVisible = true
                        
                        actions.speedUp.setOnClickListener {
                            handleSpeedUp(pendingRawTx)
                        }
                        
                        actions.cancelTx.setOnClickListener {
                            handleCancelTransaction(pendingRawTx)
                        }
                    }
                }
            }
        }
    }

    private fun tokenClick(transaction: Web3TransactionItem) {
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomBinding = ViewWalletWeb3TransactionBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(
                    requireActivity(),
                    R.style.Custom
                ), R.layout.view_wallet_web3_transaction_bottom, null
            )
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            explorer.setOnClickListener {
                val url =
                    "${Constants.API.URL}external/explore/${token.chainId}/transactions/${transaction.transactionHash}"
                context?.openUrl(url)
                bottomSheet.dismiss()
            }
            
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        refreshJob = PendingTransactionRefreshHelper.startRefreshData(
            fragment = this,
            web3ViewModel = web3ViewModel,
            jobManager = jobManager,
            refreshJob = refreshJob,
            onTransactionStatusUpdated = { hash, newStatus ->
                if (hash == transaction.transactionHash) {
                    updateTransactionStatus(newStatus)
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        refreshJob = PendingTransactionRefreshHelper.cancelRefreshData(refreshJob)
    }

    private fun updateTransactionStatus(newStatus: String) {
        binding.apply {
            when (newStatus) {
                TransactionStatus.SUCCESS.value -> {
                    status.text = getString(R.string.Completed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_green))
                    status.setBackgroundResource(R.drawable.bg_status_success)
                }

                TransactionStatus.PENDING.value -> {
                    status.text = getString(R.string.Pending)
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }

                TransactionStatus.FAILED.value -> {
                    status.text = getString(R.string.Failed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                TransactionStatus.NOT_FOUND.value -> {
                    status.text = getString(R.string.Expired)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                else -> {
                    status.text = newStatus
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }
            }
        }
    }

    private fun handleSpeedUp(rawTransaction: Web3RawTransaction) {
        lifecycleScope.launch {
            val jsSignMessage = createSpeedUpMessage(rawTransaction)
            
            showGasCheckAndBrowserBottomSheetDialogFragment(
                requireActivity(),
                jsSignMessage,
                token = token,
                chainToken = token,
                currentTitle = getString(R.string.Speed_Up_Transaction),
                onDone = { result ->

                }
            )
        }
    }
    
    private fun handleCancelTransaction(rawTransaction: Web3RawTransaction) {
        lifecycleScope.launch {
            val jsSignMessage = createCancelMessage(rawTransaction)
            
            showGasCheckAndBrowserBottomSheetDialogFragment(
                requireActivity(),
                jsSignMessage,
                token = token,
                chainToken = token,
                currentTitle = getString(R.string.Cancel_Transaction),
                onDone = { result ->

                }
            )
        }
    }
    
    private suspend fun createSpeedUpMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
        val decodedTx = TransactionDecoder.decode(rawTransaction.raw)
        
        val nonce = rawTransaction.nonce
        val data = decodedTx.data
        val value = decodedTx.value
        val to = decodedTx.to
        
        val formattedData = if (data.isNullOrEmpty()) {
            data
        } else if (!data.startsWith("0x", ignoreCase = true)) {
            "0x$data"
        } else {
            data
        }

        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_TRANSACTION,
            wcEthereumTransaction = WCEthereumTransaction(
                from = transaction.getFromAddress(),
                to = to,
                data = formattedData,
                value = Numeric.toHexStringWithPrefix(value),
                nonce = nonce,
                gasPrice = null,
                gas = null,
                gasLimit = null,
                maxFeePerGas = null,
                maxPriorityFeePerGas = null
            ),
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isSpeedUp = true
        )
    }
    
    private suspend fun createCancelMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
        val decodedTx = TransactionDecoder.decode(rawTransaction.raw)
        val nonce = rawTransaction.nonce

        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_TRANSACTION,
            wcEthereumTransaction = WCEthereumTransaction(
                from = transaction.getFromAddress(),
                to = transaction.getFromAddress(), // self address
                data = null,
                value = "0x0",
                nonce = nonce,
                gasPrice = null,
                gas = null,
                gasLimit = null,
                maxFeePerGas = null,
                maxPriorityFeePerGas = null
            ),
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isCancelTx = true
        )
    }
}
