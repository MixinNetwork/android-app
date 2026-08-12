@file:Suppress("DEPRECATION")

package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
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
import one.mixin.android.db.web3.vo.isGaslessPending
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.dp
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.common.biometric.EmptyUtxoException
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.showGasCheckAndBrowserBottomSheetDialogFragment
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Ticker
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.details.Web3TransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.send.BtcTransactionBuilder
import one.mixin.android.web3.send.InsufficientBtcBalanceException
import one.mixin.android.widget.BottomSheet
import org.web3j.crypto.TransactionDecoder
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
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

    private fun shouldShowValueDetails(): Boolean {
        if (transaction.status != TransactionStatus.SUCCESS.value) return false
        if (transaction.transactionType != TransactionType.TRANSFER_IN.value &&
            transaction.transactionType != TransactionType.TRANSFER_OUT.value) {
            return false
        }
        if (transaction.transactionType == TransactionType.TRANSFER_OUT.value && transaction.senders.size > 1) {
            return false
        }
        if (transaction.transactionType == TransactionType.TRANSFER_IN.value && transaction.receivers.size > 1) {
            return false
        }
        return transaction.getMainAmount().toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) != 0
    }

    private fun getMainAssetSymbol(): String {
        return when (transaction.transactionType) {
            TransactionType.TRANSFER_OUT.value -> transaction.sendAssetSymbol
            TransactionType.TRANSFER_IN.value -> transaction.receiveAssetSymbol
            else -> token.symbol
        } ?: token.symbol
    }

    private fun hideValueDetails() {
        binding.valueAsTv.isVisible = false
        binding.thatVa.isVisible = false
        binding.thatTv.setOnClickListener(null)
        updateStatusBottomMargin()
    }

    private fun bindCurrentValue(
        amount: BigDecimal,
        symbol: String,
        tokenItem: Web3TokenItem?,
    ) {
        binding.valueAsTv.isVisible = true
        if (tokenItem == null) {
            binding.valueAsTv.text = getString(R.string.value_now, getString(R.string.NA))
            return
        }
        val fiatSymbol = Fiats.getSymbol()
        val currentPrice = tokenItem.priceFiat()
        val valueNow = amount.abs().multiply(currentPrice).numberFormat2()
        val pricePerUnit = "(${fiatSymbol}${currentPrice.priceFormat2()}/$symbol)"
        binding.valueAsTv.text = getString(R.string.value_now, "$fiatSymbol$valueNow $pricePerUnit")
    }

    private fun bindHistoricalValue(
        ticker: Ticker,
        amount: BigDecimal,
        symbol: String,
    ) {
        if (!isAdded || view == null) return
        binding.thatVa.isVisible = true
        updateStatusBottomMargin()
        binding.thatVa.displayedChild = 1
        binding.thatTv.apply {
            text = if (ticker.priceUsd == "0") {
                getString(R.string.value_then, getString(R.string.NA))
            } else {
                val fiatSymbol = Fiats.getSymbol()
                val valueThen = amount.abs().multiply(ticker.priceFiat()).numberFormat2()
                val pricePerUnit = if (BuildConfig.DEBUG) {
                    "(${fiatSymbol}${ticker.priceFiat().priceFormat2()}/$symbol)"
                } else {
                    ""
                }
                getString(R.string.value_then, "$fiatSymbol$valueThen $pricePerUnit".trim())
            }
            setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
            setOnClickListener {
                val context = context ?: return@setOnClickListener
                val balloon = createBalloon(context) {
                    setArrowSize(10)
                    setHeight(45)
                    setCornerRadius(4f)
                    setAlpha(0.9f)
                    setAutoDismissDuration(3000L)
                    setBalloonAnimation(BalloonAnimation.FADE)
                    setText(getString(R.string.wallet_transaction_that_time_value_tip))
                    setTextColorResource(R.color.white)
                    setPaddingLeft(10)
                    setPaddingRight(10)
                    setBackgroundColorResource(R.color.colorLightBlue)
                    setLifecycleOwner(viewLifecycleOwner)
                }
                balloon.showAlignTop(this)
            }
        }
    }

    private fun bindHistoricalValueRetry(
        assetId: String,
        amount: BigDecimal,
        symbol: String,
    ) {
        if (!isAdded || view == null) return
        binding.thatVa.isVisible = true
        updateStatusBottomMargin()
        binding.thatVa.displayedChild = 1
        binding.thatTv.apply {
            text = getString(R.string.Click_to_retry)
            setTextColor(requireContext().getColor(R.color.colorDarkBlue))
            setOnClickListener {
                fetchHistoricalValue(assetId, amount, symbol)
            }
        }
    }

    private fun fetchHistoricalValue(
        assetId: String,
        amount: BigDecimal,
        symbol: String,
    ) {
        binding.thatVa.isVisible = true
        updateStatusBottomMargin()
        binding.thatVa.displayedChild = 0
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = { web3ViewModel.ticker(assetId, transaction.transactionAt) },
                successBlock = { response ->
                    val ticker = response.data
                    if (ticker == null) {
                        bindHistoricalValueRetry(assetId, amount, symbol)
                    } else {
                        bindHistoricalValue(ticker, amount, symbol)
                    }
                },
                failureBlock = {
                    bindHistoricalValueRetry(assetId, amount, symbol)
                    true
                },
                exceptionBlock = {
                    bindHistoricalValueRetry(assetId, amount, symbol)
                    true
                },
            )
        }
    }

    private fun showValueDetails() {
        val amount = transaction.getMainAmount().toBigDecimalOrNull() ?: run {
            hideValueDetails()
            return
        }
        val assetId = transaction.getMainAssetId()
        val symbol = getMainAssetSymbol()
        binding.thatVa.isVisible = true
        updateStatusBottomMargin()
        binding.thatVa.displayedChild = 0
        lifecycleScope.launch {
            val mainToken = fetchDisplayToken(assetId)
            if (!isAdded || view == null) return@launch
            bindCurrentValue(amount, symbol, mainToken)
            fetchHistoricalValue(assetId, amount, symbol)
        }
    }

    private suspend fun fetchDisplayToken(assetId: String): Web3TokenItem? {
        if (token.assetId == assetId) return token
        web3ViewModel.web3TokenItemById(wallet.id, assetId)?.let { return it }
        web3ViewModel.web3TokenItemById(Web3Signer.currentWalletId, assetId)?.let { return it }
        val syncedAsset: TokenItem = web3ViewModel.findOrSyncAsset(assetId) ?: return null
        return Web3TokenItem(
            walletId = wallet.id,
            assetId = syncedAsset.assetId,
            chainId = syncedAsset.chainId,
            name = syncedAsset.name,
            assetKey = syncedAsset.assetKey ?: "",
            symbol = syncedAsset.symbol,
            iconUrl = syncedAsset.iconUrl,
            precision = syncedAsset.precision,
            balance = syncedAsset.balance,
            priceUsd = syncedAsset.priceUsd,
            changeUsd = syncedAsset.changeUsd,
            chainIcon = syncedAsset.chainIconUrl,
            chainName = syncedAsset.chainName,
            chainSymbol = syncedAsset.chainSymbol,
            hidden = syncedAsset.hidden,
            level = syncedAsset.level ?: 0,
        )
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
            context?.openUrl(
                Constants.HelpLink.CUSTOMER_SERVICE,
                source = AnalyticsTracker.CustomerServiceSource.TRANSACTION_DETAIL,
                wallet = AnalyticsTracker.TradeWallet.WEB3,
            )
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
            titleView.setWalletNameSubTitleStyle()
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
                    fromLl.isVisible = fromAddress.isBlank().not()
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

            if (valueTv.isVisible && shouldShowValueDetails()) {
                showValueDetails()
            } else {
                hideValueDetails()
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
            feeLl.isVisible = shouldShowFee(transaction.status)
            feeTv.text = "${transaction.displayFeeAmount()} ${transaction.displayFeeSymbol() ?: ""}"
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
                            fetchDisplayToken(assetId)
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
                            fetchDisplayToken(assetId)
                        },
                    )
                }
            } else {
                assetChangesLl.visibility = View.GONE
            }
            if (transaction.status == TransactionStatus.PENDING.value) {
                lifecycleScope.launch {
                    updateFeeVisibility(transaction.status)
                    if (transaction.chainId != Constants.ChainId.SOLANA_CHAIN_ID) {
                        updateActions()
                    }
                }
            }
        }
    }

    private fun shouldShowFee(
        status: String,
        pendingRawTx: Web3RawTransaction? = null,
    ): Boolean {
        if (!transaction.hasSponsorFee() && (transaction.transactionType == TransactionType.TRANSFER_IN.value || transaction.fee.isEmpty())) {
            return false
        }
        if (status != TransactionStatus.PENDING.value) {
            return true
        }
        return pendingRawTx?.isGaslessPending() == false || transaction.displayFeeAmount().isNotEmpty()
    }

    private suspend fun updateFeeVisibility(status: String = transaction.status) {
        val pendingRawTx: Web3RawTransaction? = if (status == TransactionStatus.PENDING.value) {
            web3ViewModel.getRawTransactionByHashAndChain(wallet.id, transaction.transactionHash, transaction.chainId)
        } else {
            null
        }
        binding.feeLl.isVisible = shouldShowFee(status, pendingRawTx)
    }

    private fun updateActions(status: String = transaction.status) {
        lifecycleScope.launch {
            binding.apply {
                if (status != TransactionStatus.PENDING.value) {
                    actions.isVisible = false
                    actions.speedUp.setOnClickListener(null)
                    actions.cancelTx.setOnClickListener(null)
                    updateStatusBottomMargin()
                    return@apply
                }
                val pendingRawTx = web3ViewModel.getRawTransactionByHashAndChain(wallet.id, transaction.transactionHash, transaction.chainId)
                val shouldShowActions = pendingRawTx != null

                if (!shouldShowActions) {
                    actions.isVisible = false
                    actions.speedUp.setOnClickListener(null)
                    actions.cancelTx.setOnClickListener(null)
                    updateStatusBottomMargin()
                    return@apply
                }
                val notNullPendingRawTx: Web3RawTransaction = pendingRawTx
                if (notNullPendingRawTx.isGaslessPending()) {
                    actions.isVisible = false
                    actions.speedUp.setOnClickListener(null)
                    actions.cancelTx.setOnClickListener(null)
                    updateStatusBottomMargin()
                    return@apply
                }
                val chainId: String = transaction.chainId
                val actionRoute: PendingTransactionActionRoute = pendingTransactionActionRoute(chainId)
                if (actionRoute == PendingTransactionActionRoute.Unsupported) {
                    actions.isVisible = false
                    actions.speedUp.setOnClickListener(null)
                    actions.cancelTx.setOnClickListener(null)
                    updateStatusBottomMargin()
                    return@apply
                }
                if (actionRoute == PendingTransactionActionRoute.Utxo) {
                    val pearlReplacementUnavailable: Boolean =
                        chainId == Constants.ChainId.PEARL_CHAIN_ID && !BtcTransactionBuilder.isReplaceable(notNullPendingRawTx.raw)
                    val hasSignedChange: Boolean =
                        web3ViewModel.hasUtxoSignedOutputsByTransactionHash(transaction.transactionHash, chainId)
                    if (pearlReplacementUnavailable || hasSignedChange) {
                        actions.isVisible = false
                        actions.speedUp.setOnClickListener(null)
                        actions.cancelTx.setOnClickListener(null)
                        updateStatusBottomMargin()
                        return@apply
                    }
                    actions.cancelTx.isVisible = BtcTransactionBuilder.canCancel(
                        chainId,
                        notNullPendingRawTx.raw,
                        transaction.getFromAddress(),
                    )
                } else {
                    actions.cancelTx.isVisible = true
                }
                actions.speedUp.isVisible = true
                actions.isVisible = true
                updateStatusBottomMargin()
                actions.speedUp.setOnClickListener {
                    handleSpeedUp(notNullPendingRawTx)
                }
                actions.cancelTx.setOnClickListener {
                    handleCancelTransaction(notNullPendingRawTx)
                }
            }
        }
    }

    private fun updateStatusBottomMargin() {
        val layoutParams = binding.status.layoutParams as MarginLayoutParams
        layoutParams.bottomMargin =
            if (!binding.thatVa.isVisible && !binding.actions.isVisible) 24.dp else 0
        binding.status.layoutParams = layoutParams
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
        binding.feeLl.isVisible = shouldShowFee(newStatus)
        lifecycleScope.launch {
            updateFeeVisibility(newStatus)
        }
        updateActions(newStatus)
    }

    private fun handleSpeedUp(rawTransaction: Web3RawTransaction) {
        lifecycleScope.launch {
            val chainId: String = transaction.chainId
            val actionRoute: PendingTransactionActionRoute = pendingTransactionActionRoute(chainId)
            if (actionRoute == PendingTransactionActionRoute.Utxo) {
                val fromAddress: String = transaction.getFromAddress()
                val localRateString: String = rawTransaction.nonce
                val estimateFeeResponse = web3ViewModel.estimateUtxoFeeRate(chainId, rawTransaction.raw, localRateString)
                    ?: run {
                        toast(R.string.error_connection_error)
                        return@launch
                    }
                val currentRate: BigDecimal? = estimateFeeResponse.feeRate?.toBigDecimalOrNull()
                val localRate: BigDecimal? = localRateString.toBigDecimalOrNull()
                if (currentRate != null && localRate != null && currentRate.compareTo(localRate) != 1) {
                    toast(getString(R.string.web3_btc_speed_up_not_needed))
                    return@launch
                }
                val replacementRate: BigDecimal = currentRate ?: localRate?.plus(BigDecimal.ONE) ?: run {
                    toast(R.string.Data_error)
                    return@launch
                }
                val t = web3ViewModel.web3TokenItemById(Web3Signer.currentWalletId, token.assetId) ?:return@launch
                if ((t.balance.toBigDecimalOrNull()?: BigDecimal.ZERO)<= BigDecimal.ZERO) {
                    toast(R.string.insufficient_balance)
                    return@launch
                }
                val jsSignMessage = try {
                    createUtxoSpeedUpMessage(rawTransaction, replacementRate)
                } catch (e: InsufficientBtcBalanceException) {
                    toast(R.string.insufficient_balance)
                    return@launch
                } catch (e: EmptyUtxoException) {
                    toast(R.string.insufficient_balance)
                    return@launch
                } catch (e: IllegalArgumentException) {
                    toast(R.string.Data_error)
                    return@launch
                }
                showBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    amount = transaction.getMainAmount().removePrefix("-"),
                    token = token,
                    chainToken = token,
                    feeToken = token,
                    currentTitle = getString(R.string.Speed_Up_Transaction),
                    onDone = { _ ->
                        lifecycleScope.launch {
                            binding.actions.isVisible = false
                            binding.actions.speedUp.setOnClickListener(null)
                            binding.actions.cancelTx.setOnClickListener(null)
                            updateStatusBottomMargin()
                            web3ViewModel.deleteUtxoUnspentChangeOutputs(wallet.id, fromAddress, rawTransaction.raw, chainId)
                        }
                    },
                )
            } else if (actionRoute == PendingTransactionActionRoute.Evm) {
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
        val localRate: BigDecimal? = rawTransaction.nonce.toBigDecimalOrNull()
        if (localRate == null) {
            toast(R.string.Data_error)
            return
        }
        lifecycleScope.launch {
            val chainId: String = transaction.chainId
            val actionRoute: PendingTransactionActionRoute = pendingTransactionActionRoute(chainId)
            if (actionRoute == PendingTransactionActionRoute.Utxo) {
                val fromAddress: String = transaction.getFromAddress()
                val canCancelUtxoTransaction: Boolean = BtcTransactionBuilder.canCancel(chainId, rawTransaction.raw, fromAddress)
                if (!canCancelUtxoTransaction) {
                    toast(R.string.web3_btc_cancel_not_possible)
                    return@launch
                }
                val estimateFeeResponse = web3ViewModel.estimateUtxoFeeRate(chainId, rawTransaction.raw, localRate.toPlainString())
                    ?: run {
                        toast(R.string.error_connection_error)
                        return@launch
                    }
                val apiRate: BigDecimal? = estimateFeeResponse.feeRate?.toBigDecimalOrNull()
                if (apiRate == null) {
                    toast(R.string.Data_error)
                    return@launch
                }
                val jsSignMessage = try {
                    createUtxoCancelMessage(rawTransaction, localRate, apiRate)
                } catch (e: InsufficientBtcBalanceException) {
                    toast(R.string.insufficient_balance)
                    return@launch
                } catch (e: IllegalArgumentException) {
                    toast(R.string.Data_error)
                    return@launch
                }
                showBrowserBottomSheetDialogFragment(
                    requireActivity(),
                    jsSignMessage,
                    amount = transaction.getMainAmount().removePrefix("-"),
                    token = token,
                    chainToken = token,
                    feeToken = token,
                    currentTitle = getString(R.string.Cancel_Transaction),
                    onDone = { _ ->
                        lifecycleScope.launch {
                            binding.actions.isVisible = false
                            binding.actions.speedUp.setOnClickListener(null)
                            binding.actions.cancelTx.setOnClickListener(null)
                            updateStatusBottomMargin()
                            web3ViewModel.deleteUtxoUnspentChangeOutputs(wallet.id, fromAddress, rawTransaction.raw, chainId)
                        }
                    },
                )
            } else if (actionRoute == PendingTransactionActionRoute.Evm) {
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

    private suspend fun createUtxoSpeedUpMessage(rawTransaction: Web3RawTransaction, feeRate: BigDecimal): JsSignMessage {
        val chainId: String = transaction.chainId
        val fromAddress: String = transaction.getFromAddress()
        val localUtxos: List<WalletOutput> = web3ViewModel.outputsByAddressForSigning(fromAddress, chainId)
        val unsignedReplacementHex: String = BtcTransactionBuilder.buildSpeedUpReplacement(
            chainId = chainId,
            rawTransactionHex = rawTransaction.raw,
            fromAddress = fromAddress,
            localUtxos = localUtxos,
            feeRate = feeRate,
            maxExtraInputs = 2,
        )
        val estimatedFee: BigDecimal = BtcTransactionBuilder.fee(unsignedReplacementHex, localUtxos)
        val replacementVirtualSize: Int = BtcTransactionBuilder.virtualSize(chainId, unsignedReplacementHex)
        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_UTXO_TRANSACTION,
            data = unsignedReplacementHex,
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isSpeedUp = true,
            fee = estimatedFee,
            virtualSize = replacementVirtualSize,
            utxoChainId = chainId,
        )
    }

    private suspend fun createUtxoCancelMessage(
        rawTransaction: Web3RawTransaction,
        localRate: BigDecimal,
        apiRate: BigDecimal,
    ): JsSignMessage {
        val chainId: String = transaction.chainId
        val fromAddress: String = transaction.getFromAddress()
        val localUtxos: List<WalletOutput> = web3ViewModel.outputsByAddressForSigning(fromAddress, chainId)
        val selectedRate: BigDecimal = if (apiRate > localRate) apiRate else localRate + BigDecimal.ONE
        val unsignedReplacementHex: String = BtcTransactionBuilder.buildCancelReplacement(
            chainId = chainId,
            rawTransactionHex = rawTransaction.raw,
            fromAddress = fromAddress,
            localUtxos = localUtxos,
            feeRate = selectedRate,
        )
        val replacementVirtualSize: Int = BtcTransactionBuilder.virtualSize(chainId, unsignedReplacementHex)
        val estimatedFee: BigDecimal = BtcTransactionBuilder.fee(unsignedReplacementHex, localUtxos)
        return JsSignMessage(
            callbackId = System.currentTimeMillis(),
            type = JsSignMessage.TYPE_UTXO_TRANSACTION,
            data = unsignedReplacementHex,
            solanaTxSource = SolanaTxSource.InnerTransfer,
            isCancelTx = true,
            fee = estimatedFee,
            virtualSize = replacementVirtualSize,
            utxoChainId = chainId,
        )
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
