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
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.databinding.ViewWalletWeb3TransactionBottomBinding
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toHex
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showGasCheckAndBrowserBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.details.Web3TransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.BottomSheet
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.Coin
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.Transaction as BtcTransaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.web3j.crypto.TransactionDecoder
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.collections.indexOfFirst

@AndroidEntryPoint
class Web3TransactionFragment : BaseFragment(R.layout.fragment_web3_transaction) {
    companion object {
        const val TAG = "Web3TransactionFragment"
        const val ARGS_TRANSACTION = "args_transaction"
        const val ARGS_CHAIN = "args_chain"
        const val ARGS_WALLET = "args_wallet"

        private val BTC_NETWORK_PARAMS = MainNetParams.get()
        private const val BTC_RBF_SEQUENCE: Long = 0xfffffffdL
        private val BTC_DUST_THRESHOLD: Coin = Coin.valueOf(546L)
        private val BTC_SPEED_UP_MINIMUM_INCREMENT: Coin = Coin.valueOf(500L)
        private const val BTC_SPEED_UP_MULTIPLIER_NUMERATOR: Long = 5L
        private const val BTC_SPEED_UP_MULTIPLIER_DENOMINATOR: Long = 4L
        private val BTC_SATOSHIS_PER_BTC: BigDecimal = BigDecimal("100000000")

        fun newInstance(
            transaction: Web3TransactionItem,
            chain: String,
            web3Token: Web3TokenItem,
            wallet: WalletItem,
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
            if (wallet.category == WalletCategory.CLASSIC.value ||
                wallet.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
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
                    if ((transaction.transactionType == TransactionType.TRANSFER_OUT.value && transaction.senders.size > 1) || (transaction.transactionType == TransactionType.TRANSFER_IN.value && transaction.receivers.size > 1)) {
                        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                        valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                        if (transaction.transactionType == TransactionType.TRANSFER_OUT.value) {
                            getString(R.string.Send)
                        } else {
                            getString(R.string.Deposit)
                        }
                    } else {
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
                    if (transaction.senders.size > 1) {
                        avatar.bg.setImageResource(R.drawable.ic_snapshot_withdrawal)
                    } else {
                        avatar.bg.loadImage(transaction.sendAssetIconUrl, R.drawable.ic_avatar_place_holder)
                    }
                }

                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    if (transaction.receivers.size > 1) {
                        avatar.bg.setImageResource(R.drawable.ic_snapshot_deposit)
                    } else {
                        avatar.bg.loadImage(transaction.receiveAssetIconUrl, R.drawable.ic_avatar_place_holder)
                    }
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
            if (transaction.transactionType == TransactionType.APPROVAL.value) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesTitle.setText(R.string.TOKEN_ACCESS_APPROVAL)

                assetChangesContainer.setContent {
                    AssetChangesList(
                        status = transaction.status,
                        senders = transaction.senders,
                        receivers = transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(Web3Signer.currentWalletId, assetId)
                        },
                        approvals = transaction.approvals,
                    )
                }
            } else if (transaction.transactionType == TransactionType.SWAP.value || (transaction.transactionType == TransactionType.TRANSFER_OUT.value && transaction.senders.size > 1) || (transaction.transactionType == TransactionType.TRANSFER_IN.value && transaction.receivers.size > 1)) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesContainer.setContent {
                    AssetChangesList(
                        status = transaction.status,
                        senders = if (transaction.transactionType == TransactionType.TRANSFER_IN.value) emptyList() else transaction.senders,
                        receivers = if (transaction.transactionType == TransactionType.TRANSFER_OUT.value) emptyList() else transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(Web3Signer.currentWalletId, assetId)
                        }
                    )
                }
            } else {
                assetChangesLl.visibility = View.GONE
            }
            if (transaction.status == TransactionStatus.PENDING.value
                && transaction.chainId != Constants.ChainId.SOLANA_CHAIN_ID) {
                lifecycleScope.launch {
                    val pendingRawTx = web3ViewModel.getRawTransactionByHashAndChain(wallet.id, transaction.transactionHash, transaction.chainId)
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
            if (token.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                val jsSignMessage = createBtcSpeedUpMessage(rawTransaction)
                val fromAddress: String = transaction.getFromAddress()
                showBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    token = token,
                    chainToken = token,
                    currentTitle = getString(R.string.Speed_Up_Transaction),
                    onDone = { _ ->
                        lifecycleScope.launch {
                            web3ViewModel.deleteBitcoinUnspentChangeOutputs(wallet.id, fromAddress, rawTransaction.raw)
                        }
                    },
                )
            } else {
                val jsSignMessage = createSpeedUpMessage(rawTransaction)
                showGasCheckAndBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    token = token,
                    chainToken = token,
                    currentTitle = getString(R.string.Speed_Up_Transaction),
                    onDone = { _ ->
                    },
                )
            }
        }
    }
    
    private fun handleCancelTransaction(rawTransaction: Web3RawTransaction) {
        lifecycleScope.launch {
            if (token.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                val jsSignMessage = createBtcCancelMessage(rawTransaction)
                val fromAddress: String = transaction.getFromAddress()
                showBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    token = token,
                    chainToken = token,
                    currentTitle = getString(R.string.Cancel_Transaction),
                    onDone = { _ ->
                        lifecycleScope.launch {
                            web3ViewModel.deleteBitcoinUnspentChangeOutputs(wallet.id, fromAddress, rawTransaction.raw)
                        }
                    },
                )
            } else {
                val jsSignMessage = createCancelMessage(rawTransaction)
                showGasCheckAndBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    token = token,
                    chainToken = token,
                    currentTitle = getString(R.string.Cancel_Transaction),
                    onDone = { _ ->
                    },
                )
            }
        }
    }
    
    private fun createSpeedUpMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
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

    private suspend fun createBtcSpeedUpMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
        val fromAddress: String = transaction.getFromAddress()
        val localUtxos: List<WalletOutput> = web3ViewModel.outputsByAddressForSigning(fromAddress, Constants.ChainId.BITCOIN_CHAIN_ID)
        val unsignedReplacementHex: String = buildBtcReplacementTransactionHex(rawTransaction.raw, fromAddress, localUtxos)
        val estimatedFeeBtc: BigDecimal = estimateBtcFeeFromUnsignedTransaction(unsignedReplacementHex, localUtxos)
        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_BTC_TRANSACTION,
            data = unsignedReplacementHex,
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isSpeedUp = true,
            fee = estimatedFeeBtc,
        )
    }

    private suspend fun createBtcCancelMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
        val fromAddress: String = transaction.getFromAddress()
        val localUtxos: List<WalletOutput> = web3ViewModel.outputsByAddressForSigning(fromAddress, Constants.ChainId.BITCOIN_CHAIN_ID)
        val unsignedReplacementHex: String = buildBtcCancelTransactionHex(rawTransaction.raw, fromAddress, localUtxos)
        val estimatedFeeBtc: BigDecimal = estimateBtcFeeFromUnsignedTransaction(unsignedReplacementHex, localUtxos)
        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_BTC_TRANSACTION,
            data = unsignedReplacementHex,
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isCancelTx = true,
            fee = estimatedFeeBtc,
        )
    }

    private fun buildBtcCancelTransactionHex(
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
    ): String {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val originalTx: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val originalInputs: List<TransactionInput> = originalTx.inputs
        val inputAmount: Coin = calculateInputAmount(originalInputs, localUtxos)
        val originalOutputAmount: Coin = originalTx.outputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
        val currentFee: Coin = inputAmount.subtract(originalOutputAmount)
        val desiredFee: Coin = calculateDesiredBtcSpeedUpFee(currentFee)
        val feeDelta: Coin = desiredFee.subtract(currentFee)
        val replacementTx = BtcTransaction()
        for (input: TransactionInput in originalInputs) {
            val outPoint = TransactionOutPoint(input.outpoint.index(), input.outpoint.hash())
            val txInput = TransactionInput(replacementTx, byteArrayOf(), outPoint)
            txInput.withSequence(BTC_RBF_SEQUENCE)
            replacementTx.addInput(txInput)
        }
        val selfScript: Script = buildP2wpkhScript(fromAddress)
        val originalOutputValues: List<Coin> = originalTx.outputs.map { it.value }
        if (feeDelta.isZero || feeDelta.isNegative) {
            for (value: Coin in originalOutputValues) {
                replacementTx.addOutput(value, selfScript)
            }
            return replacementTx.serialize().toHex()
        }
        val maxIndex: Int = originalOutputValues.indices.maxByOrNull { index -> originalOutputValues[index].value } ?: -1
        if (maxIndex < 0) {
            throw IllegalArgumentException("insufficient balance")
        }
        val adjustedMax: Coin = originalOutputValues[maxIndex].subtract(feeDelta)
        if (adjustedMax.isNegative || adjustedMax.isZero || adjustedMax.isLessThan(BTC_DUST_THRESHOLD)) {
            val sendToSelf: Coin = inputAmount.subtract(desiredFee)
            if (sendToSelf.isNegative || sendToSelf.isZero || sendToSelf.isLessThan(BTC_DUST_THRESHOLD)) {
                throw IllegalArgumentException("insufficient balance")
            }
            replacementTx.addOutput(sendToSelf, selfScript)
            return replacementTx.serialize().toHex()
        }
        originalOutputValues.forEachWithIndex { index, value ->
            val outputValue: Coin = if (index == maxIndex) adjustedMax else value
            replacementTx.addOutput(outputValue, selfScript)
        }
        return replacementTx.serialize().toHex()
    }

    private fun buildBtcReplacementTransactionHex(
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
    ): String {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val originalTx: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val fromScriptBytes: ByteArray = buildP2wpkhScript(fromAddress).program()
        val originalInputs = originalTx.inputs
        val originalOutputs = originalTx.outputs
        val inputAmount: Coin = calculateInputAmount(originalInputs, localUtxos)
        val outputAmount: Coin = originalOutputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
        val currentFee: Coin = inputAmount.subtract(outputAmount)
        val desiredFee: Coin = calculateDesiredBtcSpeedUpFee(currentFee)
        val feeDelta: Coin = desiredFee.subtract(currentFee)
        if (feeDelta.isZero || feeDelta.isNegative) {
            return cleanedRawHex
        }
        val changeOutputIndex: Int? = originalOutputs.indexOfFirst { output ->
            output.scriptBytes.contentEquals(fromScriptBytes)
        }.takeIf { index -> index >= 0 }
        val additionalUtxo: WalletOutput? = if (changeOutputIndex == null) {
            findAdditionalUtxo(originalInputs, localUtxos)
        } else {
            val currentChange: Coin = originalOutputs[changeOutputIndex].value
            if (currentChange.isGreaterThan(feeDelta)) null else findAdditionalUtxo(originalInputs, localUtxos)
        }
        val replacementTx = BtcTransaction()
        for (input in originalInputs) {
            val outPoint = TransactionOutPoint(input.outpoint.index, input.outpoint.hash)
            val txInput = TransactionInput(replacementTx, byteArrayOf(), outPoint)
            txInput.withSequence(BTC_RBF_SEQUENCE)
            replacementTx.addInput(txInput)
        }
        if (additionalUtxo != null) {
            val outPoint = TransactionOutPoint(additionalUtxo.outputIndex, Sha256Hash.wrap(additionalUtxo.transactionHash))
            val txInput = TransactionInput(replacementTx, byteArrayOf(), outPoint)
            txInput.withSequence(BTC_RBF_SEQUENCE)
            replacementTx.addInput(txInput)
        }
        val adjustedOutputs: List<Pair<Coin, Script>> = buildAdjustedOutputs(
            originalOutputs = originalOutputs,
            fromScriptBytes = fromScriptBytes,
            feeDelta = feeDelta,
            additionalInput = additionalUtxo,
        )
        for ((value, script) in adjustedOutputs) {
            replacementTx.addOutput(value, script)
        }
        return replacementTx.serialize().toHex()
    }

    private fun buildAdjustedOutputs(
        originalOutputs: List<TransactionOutput>,
        fromScriptBytes: ByteArray,
        feeDelta: Coin,
        additionalInput: WalletOutput?,
    ): List<Pair<Coin, Script>> {
        val outputs: MutableList<Pair<Coin, Script>> = mutableListOf()
        val changeIndex: Int = originalOutputs.indexOfFirst { output -> output.scriptBytes.contentEquals(fromScriptBytes) }
        val extraInputAmount: Coin = if (additionalInput == null) Coin.ZERO else Coin.parseCoin(additionalInput.amount)
        for ((index, output) in originalOutputs.withIndex()) {
            if (index != changeIndex) {
                outputs.add(output.value to Script.parse(output.scriptBytes))
                continue
            }
            val updatedChange: Coin = output.value.add(extraInputAmount).subtract(feeDelta)
            if (updatedChange.isGreaterThan(BTC_DUST_THRESHOLD) || updatedChange == BTC_DUST_THRESHOLD) {
                outputs.add(updatedChange to Script.parse(output.scriptBytes))
            }
        }
        if (changeIndex < 0 && additionalInput != null) {
            val updatedChange: Coin = extraInputAmount.subtract(feeDelta)
            if (updatedChange.isGreaterThan(BTC_DUST_THRESHOLD) || updatedChange == BTC_DUST_THRESHOLD) {
                outputs.add(updatedChange to Script.parse(fromScriptBytes))
            }
        }
        return outputs
    }

    private fun buildP2wpkhScript(address: String): Script {
        val addressParser: AddressParser = AddressParser.getDefault()
        val parsedAddress = addressParser.parseAddress(address)
        return ScriptBuilder.createOutputScript(parsedAddress)
    }

    private fun calculateInputAmount(
        inputs: List<TransactionInput>,
        localUtxos: List<WalletOutput>,
    ): Coin {
        var total: Coin = Coin.ZERO
        for (input: TransactionInput in inputs) {
            val utxo = localUtxos.firstOrNull { local ->
                local.transactionHash.equals(input.outpoint.hash().toString(), ignoreCase = true) &&
                    local.outputIndex == input.outpoint.index()
            } ?: continue
            total = total.add(Coin.parseCoin(utxo.amount))
        }
        return total
    }

    private fun findAdditionalUtxo(
        existingInputs: List<TransactionInput>,
        localUtxos: List<WalletOutput>,
    ): WalletOutput? {
        for (utxo: WalletOutput in localUtxos) {
            val exists: Boolean = existingInputs.any { input ->
                utxo.transactionHash.equals(input.outpoint.hash().toString(), ignoreCase = true) &&
                    utxo.outputIndex == input.outpoint.index()
            }
            if (!exists) {
                return utxo
            }
        }
        return null
    }

    private fun calculateDesiredBtcSpeedUpFee(currentFee: Coin): Coin {
        val multiplied: Coin = currentFee.multiply(BTC_SPEED_UP_MULTIPLIER_NUMERATOR).divide(BTC_SPEED_UP_MULTIPLIER_DENOMINATOR)
        val minimum: Coin = currentFee.add(BTC_SPEED_UP_MINIMUM_INCREMENT)
        return if (multiplied.isGreaterThan(minimum)) multiplied else minimum
    }

    private fun estimateBtcFeeFromUnsignedTransaction(
        unsignedTransactionHex: String,
        localUtxos: List<WalletOutput>,
    ): BigDecimal {
        val cleanedHex: String = unsignedTransactionHex.removePrefix("0x").trim()
        val tx: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedHex.hexStringToByteArray()))
        val inputAmount: Coin = calculateInputAmount(tx.inputs, localUtxos)
        val outputAmount: Coin = tx.outputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
        val feeSatoshi: BigDecimal = BigDecimal.valueOf(inputAmount.subtract(outputAmount).value)
        return feeSatoshi.divide(BTC_SATOSHIS_PER_BTC)
    }


    private fun createCancelMessage(rawTransaction: Web3RawTransaction): JsSignMessage {
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
