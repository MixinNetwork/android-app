package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonElement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.request.web3.GaslessFeeRequest
import one.mixin.android.api.request.web3.GaslessTxRequest
import one.mixin.android.api.request.web3.SubmitGaslessTxRequest
import one.mixin.android.api.request.web3.WEB3_FEE_TYPE_FREE
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.web3.EthGaslessTxPayload
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainSymbolFromName
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putLong
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshWeb3BitCoinJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.address.ReceiveSelectionBottom.OnReceiveSelectionClicker
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.ReceiveQrActivity
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.WaitingBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.EmptyUtxoException
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.safe.toWeb3TokenItem
import one.mixin.android.vo.toUser
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.SOLANA_RENT_EXEMPTION
import one.mixin.android.web3.hasSolBalanceAfterFeeAndRent
import one.mixin.android.web3.isNativeSolAsset
import one.mixin.android.web3.nativeSolSpendableBalance
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.send.InsufficientBtcBalanceException
import one.mixin.android.widget.Keyboard
import org.sol4k.Base58
import org.sol4k.PublicKey
import org.sol4k.Constants.SIGNATURE_LENGTH
import org.sol4kt.VersionedTransactionCompat
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class InputFragment : BaseFragment(R.layout.fragment_input), OnReceiveSelectionClicker {
    companion object {
        const val TAG = "InputFragment"
        private const val GASLESS_EIP7702_AUTHORIZED_ADDRESS = "0xe6cae83bde06e4c305530e199d7217f42808555b"
        const val ARGS_TO_ADDRESS = "args_to_address"
        const val ARGS_FROM_ADDRESS = "args_from_address"
        const val ARGS_TO_ADDRESS_TAG = "args_to_address_tag"
        const val ARGS_TO_USER = "args_to_user"

        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_WEB3_CHAIN_TOKEN = "args_web3_chain_token"

        const val ARGS_TOKEN = "args_token"

        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        enum class TransferType {
            USER,
            ADDRESS,
            WEB3,
            BIOMETRIC_ITEM
        }
    }

    private val transferType: TransferType by lazy {
        when {
            arguments?.containsKey(ARGS_WEB3_TOKEN) == true -> TransferType.WEB3
            arguments?.containsKey(ARGS_TO_USER) == true -> TransferType.USER
            arguments?.containsKey(ARGS_BIOMETRIC_ITEM) == true -> TransferType.BIOMETRIC_ITEM
            else -> TransferType.ADDRESS
        }
    }

    private val binding by viewBinding(FragmentInputBinding::bind)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var isReverse: Boolean = false

    private val toAddress by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS) ?: (assetBiometricItem as? WithdrawBiometricItem)?.address?.destination
    }

    private val fromAddress by lazy {
        requireArguments().getString(ARGS_FROM_ADDRESS)
    }
    private val user: User? by lazy {
        arguments?.getParcelableCompat(ARGS_TO_USER, User::class.java)
    }
    private val web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
    }
    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_CHAIN_TOKEN, Web3TokenItem::class.java)
    }

    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java) ?: assetBiometricItem?.asset
    }

    private val assetBiometricItem: AssetBiometricItem? by lazy {
        requireArguments().getParcelableCompat(ARGS_BIOMETRIC_ITEM, AssetBiometricItem::class.java)
    }

    private val addressTag by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_TAG) ?: (assetBiometricItem as? WithdrawBiometricItem)?.address?.tag
    }

    private val currencyName by lazy {
        Fiats.getAccountCurrencyAppearance()
    }

    private val tokenPrice: BigDecimal by lazy {
        ((token?.priceUsd ?: web3Token?.priceUsd)?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(Fiats.getRate().toBigDecimal())
    }
    private val tokenSymbol by lazy {
        token?.symbol ?: web3Token!!.symbol
    }
    private val tokenIconUrl by lazy {
        token?.iconUrl ?: web3Token!!.iconUrl
    }
    private val tokenChainIconUrl by lazy {
        token?.chainIconUrl ?: web3Token?.chainIcon ?: ""
    }
    private val tokenBalance by lazy {
        token?.balance ?: web3Token!!.balance
    }
    private val tokenName by lazy {
        token?.name ?: web3Token!!.name
    }

    private var currentNote: String? = null

    private var isSolanaToAccountExists = true

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var rpc: Rpc

    override fun onResume() {
        super.onResume()
        bindingOrNull()?.root?.hideKeyboard()
    }

    override fun onDestroyView() {
        btcFeeRecalculateJob?.cancel()
        btcFeeRecalculateJob = null
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        if (alertDialog.isShowing) {
            alertDialog.dismiss()
        }
        super.onDestroyView()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(SyncOutputJob())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.apply {
                if (requireActivity() !is WalletActivity){
                    root.fitsSystemWindows = false
                }
                binding.feeTv.setOnClickListener {
                    CrossWalletFeeFreeBottomSheetDialogFragment
                        .newInstance()
                        .show(parentFragmentManager, CrossWalletFeeFreeBottomSheetDialogFragment.TAG)
                }
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.rightIb.setOnClickListener {
                    requireContext().openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }
                binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, getString(R.string.Token))
                binding.insufficientFunds.text =
                    getString(
                        R.string.send_sol_for_rent,
                        SOLANA_RENT_EXEMPTION.stripTrailingZeros().toPlainString(),
                    )
                initTitle()
                keyboard.tipTitleEnabled = false
                keyboard.disableNestedScrolling()
                setupPrimaryPasteOnly()
                keyboard.setOnClickKeyboardListener(
                    object : Keyboard.OnClickKeyboardListener {
                        override fun onKeyClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.tickVibrate()
                            pendingWeb3ShortcutPercentage = null
                            if (position == 11) {
                                v =
                                    if (v == "0") {
                                        "0"
                                    } else if (v.length == 1) {
                                        "0"
                                    } else {
                                        v.substring(0, v.length - 1)
                                    }
                            } else {
                                if (v == "0" && value != ".") {
                                    v = value
                                } else if (!isReverse && ((isEightDecimal(v) && web3Token == null))) {
                                    // do nothing
                                    return
                                } else if (isReverse && isTwoDecimal(v)) {
                                    // do nothing
                                    return
                                } else if (value == "." && v.contains(".")) {
                                    // do nothing
                                    return
                                } else {
                                    v += value
                                }
                            }
                            updateUI()
                        }

                        override fun onLongClick(
                            position: Int,
                            value: String,
                        ) {
                            if (position == 11) {
                                pendingWeb3ShortcutPercentage = null
                                v = "0"
                                updateUI()
                            }
                            context?.clickVibrate()
                        }
                    },
                )
                avatar.bg.loadImage(tokenIconUrl, R.drawable.ic_avatar_place_holder)
                avatar.badge.loadImage(tokenChainIconUrl, R.drawable.ic_avatar_place_holder)
                name.text = tokenName
                balanceTv.text = getString(R.string.available_balance, "${tokenBalance.let {
                    if (web3Token == null) {
                        it.numberFormat8()
                    } else {
                        it.numberFormat12()
                    }}} $tokenSymbol")
                max.setOnClickListener {
                    valueClick(BigDecimal.ONE)
                }
                quarter.setOnClickListener {
                    valueClick(BigDecimal.valueOf(0.25))
                }
                half.setOnClickListener {
                    valueClick(BigDecimal.valueOf(0.5))
                }
                keyboard.initPinKeys(
                    requireContext(),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<<"),
                    force = true,
                    white = true,
                )
                binding.addTv.setOnClickListener {
                    if (insufficientBalance.isVisible) {
                        if (web3Token != null) { // Insufficient web token Balance
                            AddFeeBottomSheetDialogFragment.newInstance(web3Token!!)
                                .apply {
                                    onWeb3Action = { type, t ->
                                        if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                            AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.FEE)
                                            SwapActivity.show(
                                                requireActivity(),
                                                input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                                output = t.assetId,
                                                null,
                                                null,
                                                walletId = Web3Signer.currentWalletId,
                                                inMixin = false
                                            )
                                        } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                            val address =
                                                when (web3Token?.chainId) {
                                                    Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
                                                    Constants.ChainId.BITCOIN_CHAIN_ID -> Web3Signer.btcAddress
                                                    in Constants.Web3EvmChainIds -> Web3Signer.evmAddress
                                                    else -> null
                                                }
                                            this@InputFragment.view?.navigate(
                                                R.id.action_input_fragment_to_web3_address_fragment,
                                                Bundle().apply {
                                                    putString("address", address)
                                                    putParcelable("web3_token", t)
                                                    putBoolean("args_hide_network_switch", true)
                                                }
                                            )
                                        }
                                    }
                                }.showNow(
                                    parentFragmentManager,
                                    AddFeeBottomSheetDialogFragment.TAG
                                )
                        } else if (token != null) { // Insufficient token Balance
                            AddFeeBottomSheetDialogFragment.newInstance(token!!)
                                .apply {
                                    onAction = { type, t ->
                                        if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                            AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.FEE)
                                            SwapActivity.show(
                                                requireActivity(),
                                                input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                                output = t.assetId,
                                                null,
                                                null
                                            )
                                        } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                            view.navigate(
                                                R.id.action_input_fragment_to_deposit_fragment,
                                                Bundle().apply {
                                                    putParcelable("args_asset", token!!)
                                                    putBoolean("args_hide_network_switch", true)
                                                }
                                            )
                                        }
                                    }
                                }.showNow(
                                    parentFragmentManager,
                                    AddFeeBottomSheetDialogFragment.TAG
                                )
                        }
                    } else if (transferType == TransferType.WEB3 && shouldUseGaslessFlow() && currentGaslessFee != null) { // Insufficient gasless fee Balance
                        val gaslessToken = gaslessFeeToken ?: currentGaslessFee!!.token.toWeb3TokenItem(requireNotNull(web3Token).walletId)
                        AddFeeBottomSheetDialogFragment.newInstance(gaslessToken)
                            .apply {
                                onWeb3Action = { type, t ->
                                    if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.FEE)
                                        SwapActivity.show(
                                            requireActivity(),
                                            input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                            output = t.assetId,
                                            walletId = Web3Signer.currentWalletId,
                                            inMixin = false
                                        )
                                    } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                        val address =
                                            when (web3Token?.chainId) {
                                                Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
                                                Constants.ChainId.BITCOIN_CHAIN_ID -> Web3Signer.btcAddress
                                                in Constants.Web3EvmChainIds -> Web3Signer.evmAddress
                                                else -> null
                                            }
                                        this@InputFragment.view?.navigate(
                                            R.id.action_input_fragment_to_web3_address_fragment,
                                            Bundle().apply {
                                                putString("address", address)
                                                putParcelable("web3_token", t)
                                                putBoolean("args_hide_network_switch", true)
                                            }
                                        )
                                    }
                                }
                            }.showNow(
                                parentFragmentManager,
                                AddFeeBottomSheetDialogFragment.TAG
                            )
                    } else if (gas != null && chainToken != null) { // Insufficient gas Balance
                        AddFeeBottomSheetDialogFragment.newInstance(chainToken!!)
                            .apply {
                                onWeb3Action = { type, t ->
                                    if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.FEE)
                                        SwapActivity.show(
                                            requireActivity(),
                                            input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                            output = t.assetId,
                                            walletId = Web3Signer.currentWalletId,
                                            inMixin = false
                                        )
                                    } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                        val address =
                                            when (web3Token?.chainId) {
                                                Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
                                                Constants.ChainId.BITCOIN_CHAIN_ID -> Web3Signer.btcAddress
                                                in Constants.Web3EvmChainIds -> Web3Signer.evmAddress
                                                else -> null
                                            }
                                        this@InputFragment.view?.navigate(
                                            R.id.action_input_fragment_to_web3_address_fragment,
                                            Bundle().apply {
                                                putString("address", address)
                                                putParcelable("web3_token", t)
                                                putBoolean("args_hide_network_switch", true)
                                            }
                                        )
                                    }
                                }
                            }.showNow(
                                parentFragmentManager,
                                AddFeeBottomSheetDialogFragment.TAG
                            )
                    } else if (currentFee != null) { // Insufficient fee Balance
                        AddFeeBottomSheetDialogFragment.newInstance(currentFee!!.token)
                            .apply {
                                onAction = { type, t ->
                                    if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.FEE)
                                        SwapActivity.show(
                                            requireActivity(),
                                            input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                            output = t.assetId,
                                            null,
                                            null
                                        )
                                    } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                        view.navigate(
                                            R.id.action_input_fragment_to_deposit_fragment,
                                            Bundle().apply {
                                                putParcelable("args_asset", currentFee!!.token)
                                                putBoolean("args_hide_network_switch", true)
                                            }
                                        )
                                    }
                                }
                            }.showNow(
                                parentFragmentManager,
                                AddFeeBottomSheetDialogFragment.TAG
                            )
                    }
                }
                when(transferType) {
                    TransferType.USER, TransferType.BIOMETRIC_ITEM -> {
                        if (assetBiometricItem is WithdrawBiometricItem) {
                            binding.titleTextView.setText(R.string.Network_Fee)
                        } else {
                            binding.infoLinearLayout.setOnClickListener {
                                noteDialog()
                            }
                            binding.titleTextView.setText(R.string.Note_Optional)
                            if (assetBiometricItem?.memo != null) {
                                binding.contentTextView.text = assetBiometricItem?.memo
                            } else {
                                binding.contentTextView.setText(R.string.add_a_note)
                            }
                            binding.iconImageView.isVisible = true
                            binding.iconImageView.setImageResource(R.drawable.ic_arrow_right)
                            currentNote = assetBiometricItem?.memo
                        }
                    }
                    else -> {
                        binding.titleTextView.setText(R.string.Network_Fee)
                    }
                }
                continueVa.setOnClickListener {
                    when  {
                        transferType == TransferType.ADDRESS  || (transferType == TransferType.BIOMETRIC_ITEM && assetBiometricItem is WithdrawBiometricItem)-> {
                            val toAddress = requireNotNull(toAddress)
                            val assetId = requireNotNull(token?.assetId)
                            val chainId = requireNotNull(token?.chainId)
                            val amount =
                                if (isReverse) {
                                    binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                                } else {
                                    v
                                }
                            viewLifecycleOwner.lifecycleScope.launch(
                                CoroutineExceptionHandler { _, error ->
                                    ErrorHandler.handleError(error)
                                    alertDialog.dismiss()
                                },
                            ) {
                                if (currentFee == null) {
                                    alertDialog.show()
                                    refreshFee(token!!)
                                    alertDialog.dismiss()
                                    return@launch
                                }
                                feeTokensExtra = web3ViewModel.findTokensExtra(currentFee!!.token.assetId)
                                var feeItem = web3ViewModel.findAssetItemById(currentFee!!.token.assetId)
                                if (feeItem == null) {
                                    alertDialog.show()
                                    feeItem = web3ViewModel.syncAsset(currentFee!!.token.assetId)
                                    alertDialog.dismiss()
                                }
                                if (feeItem == null) {
                                    toast(R.string.insufficient_balance)
                                    return@launch
                                }
                                val totalAmount =
                                    if (currentFee?.token?.assetId == assetId) {
                                        (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) + (currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                    } else {
                                        currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    }
                                if (feeTokensExtra == null || (feeTokensExtra?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < totalAmount) {
                                    binding.insufficientFeeBalance.isVisible = true
                                    binding.insufficientBalance.isVisible = false
                                    binding.insufficientFunds.isVisible = false
                                    binding.addTv.text = "${getString(R.string.Add)} ${currentFee?.token?.symbol ?: ""}"
                                    return@launch
                                } else {
                                    binding.insufficientFeeBalance.isVisible = false
                                }
                                val address = (assetBiometricItem as? WithdrawBiometricItem)?.address
                                    ?: Address("", "address", assetId, chainId, toAddress, addressLabel ?: "", nowInUtc(), addressTag, null)
                                val trace = (assetBiometricItem as? WithdrawBiometricItem)?.traceId ?: UUID.randomUUID().toString()
                                val networkFee = NetworkFee(feeItem, currentFee?.fee ?: "0")
                                val toWallet = web3ViewModel.anyAddressExists(listOf(address.destination))
                                val (_, index, isSafeWalletOwner) = web3ViewModel.checkAddressAndGetDisplayName(address.destination, null, chainId = chainId) ?: Triple(null, 0, null)
                                val withdrawBiometricItem = WithdrawBiometricItem(
                                    address,
                                    networkFee,
                                    addressLabel,
                                    trace,
                                    token,
                                    amount,
                                    null,
                                    PaymentStatus.pending.name,
                                    null,
                                    toWallet,
                                    isFeeWaived = index == 1 || index == 2 || index == 4,  // Privacy(1), Safe(2), Fee-free(4)
                                    isSafeWallet = index == 2,
                                    isSafeWalletOwner = isSafeWalletOwner,
                                )

                                prepareCheck(withdrawBiometricItem)
                            }
                        }
                        transferType == TransferType.WEB3 -> {
                            val token = requireNotNull(web3Token)
                            val fromAddress = requireNotNull(fromAddress)
                            val toAddress = requireNotNull(toAddress)
                            val amount = currentInputAmount()
                            viewLifecycleOwner.lifecycleScope.launch(
                                CoroutineExceptionHandler { _, error ->
                                    Timber.e("Error: ${error.message}")
                                    ErrorHandler.handleError(error)
                                    alertDialog.dismiss()
                                },
                            ) {
                                if (shouldUseGaslessFlow()) {
                                    if (!isGaslessFeeEnough(amount)) {
                                        binding.insufficientFeeBalance.isVisible = true
                                        binding.insufficientBalance.isVisible = false
                                        binding.insufficientFunds.isVisible = false
                                        binding.addTv.text = "${getString(R.string.Add)} ${currentGaslessFee?.token?.symbol ?: ""}"
                                        return@launch
                                    }
                                    val previewFeeToken = gaslessFeeToken ?: currentGaslessFee?.token?.toWeb3TokenItem(token.walletId)
                                    showBrowserBottomSheetDialogFragment(
                                        requireActivity(),
                                        JsSignMessage(
                                            callbackId = 0L,
                                            type = JsSignMessage.TYPE_GASLESS_TRANSFER,
                                        ),
                                        amount = amount,
                                        token = token,
                                        chainToken = chainToken,
                                        feeAmount = currentGaslessFee?.fee,
                                        feeToken = previewFeeToken,
                                        toAddress = toAddress,
                                        toUser = user,
                                        onCustomPinAction = { pin ->
                                            submitGaslessTransfer(pin)
                                        },
                                        onDismiss = { isDone ->
                                            if (isDone) {
                                                handleSuccessfulWeb3Transfer()
                                            }
                                        },
                                        isFeeWaived = isFeeWaived,
                                    )
                                    return@launch
                                }
                                if (token.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                                    val minBtcAmount = BigDecimal("0.00001") // 1,000 sat
                                    val inputAmount: BigDecimal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                    if (inputAmount < minBtcAmount) {
                                        toast(getString(R.string.single_transaction_should_be_greater_than, minBtcAmount.toPlainString(), token.symbol))
                                        return@launch
                                    }
                                }
                                val transaction = if (token.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                                    runCatching {
                                        token.buildTransaction(rpc, fromAddress, toAddress, amount, web3ViewModel.outputsByAddress(fromAddress, token.assetId), rate, miniFee)
                                    }.onFailure { e ->
                                        Timber.e("Build Transaction Error: ${e.message}")
                                        if (e is EmptyUtxoException) {
                                            ErrorHandler.handleError(e)
                                        }
                                    }.getOrNull()
                                } else {
                                    token.buildTransaction(rpc, fromAddress, toAddress, amount)
                                } ?: return@launch
                                showBrowserBottomSheetDialogFragment(
                                    requireActivity(),
                                    transaction,
                                    token = token,
                                    amount = amount,
                                    toAddress = toAddress,
                                    toUser = user,
                                    chainToken = chainToken,
                                    isFeeWaived = isFeeWaived,
                                    onTxhash = { _, serializedTx ->
                                    },
                                    onDismiss = { isDone ->
                                        if (isDone) {
                                            handleSuccessfulWeb3Transfer()
                                        }
                                    }
                                )
                            }
                        }
                        transferType == TransferType.USER -> {
                            val amount =
                                if (isReverse) {
                                    binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                                } else {
                                    v
                                }
                            val user = requireNotNull(user)
                            val biometricItem = buildTransferBiometricItem(user, token, amount, null, memo = currentNote, null)
                            prepareCheck(biometricItem)
                        }

                        transferType == TransferType.BIOMETRIC_ITEM -> {
                            val item = assetBiometricItem ?: return@setOnClickListener
                            val amount =
                                if (isReverse) {
                                    binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                                } else {
                                    v
                                }
                            item.amount = amount
                            item.memo = currentNote
                            prepareCheck(item)
                        }

                        else -> {
                            throw IllegalArgumentException("Not supported type")
                        }
                    }
                }
                switchIv.setOnClickListener {
                    pendingWeb3ShortcutPercentage = null
                    isReverse = !isReverse
                    v =
                        if (isReverse) {
                            BigDecimal(v).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).stripTrailingZeros().toString()
                        } else {
                            if (tokenPrice <= BigDecimal.ZERO) {
                                tokenBalance
                            } else {
                                BigDecimal(v).divide(tokenPrice, 8, RoundingMode.DOWN).stripTrailingZeros().toString()
                            }
                        }
                    updateUI()
                }
                updateUI()
            }
            checkSolanaToExists()
            refreshFee()
        }
    }

    private fun applyFeeUi() {
        val binding = bindingOrNull() ?: return
        val hasFeeText: Boolean = binding.contentTextView.text.toString().isNotEmpty()
        val showFee: Boolean = isFeeWaived && hasFeeText
        binding.contentTextView.paintFlags =
            if (showFee) {
                binding.contentTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.contentTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        binding.feeTv.isVisible = showFee
    }

    private var addressLabel:String? = null

    private fun initTitle() {
        binding.apply {
            when (transferType) {
                TransferType.USER -> {
                    titleView.setSubTitle(getString(R.string.Send_To_Title), user)
                }
                TransferType.ADDRESS -> {
                    renderTitle(requireNotNull(toAddress), addressTag)
                }
                TransferType.WEB3 -> {
                    if (user != null) {
                        isFeeWaived = true
                        titleView.setSubTitle(getString(R.string.Send_To_Title), user)
                    } else {
                        renderTitle(requireNotNull(toAddress))
                    }
                }
                TransferType.BIOMETRIC_ITEM -> {
                    assetBiometricItem?.let { item ->
                        when (item) {
                            is WithdrawBiometricItem -> {
                                // isFeeWaived todo check is my wallet
                                titleView.setLabel(getString(R.string.Send_To_Title), addressLabel, "")
                            }

                            is AddressTransferBiometricItem -> {
                                titleView.setLabel(getString(R.string.Send_To_Title), null, (if (toAddress == null) item.address else "$toAddress${addressTag?.let { ":$it" } ?: ""}").formatPublicKey(16))
                                renderTitle(toAddress ?: item.address, addressTag)
                            }

                            is TransferBiometricItem -> {
                                titleView.setSubTitle(getString(R.string.Send_To_Title), item.users) {
                                    showUserList(item.users)
                                }
                            }

                            else -> {
                                titleView.setSubTitle(
                                    getString(R.string.Send_To_Title),
                                    ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupPrimaryPasteOnly() {
        val primaryTextView: TextView = binding.primaryTv
        primaryTextView.setTextIsSelectable(false)
        primaryTextView.setOnLongClickListener {
            showPrimaryPasteActionMode(primaryTextView)
            true
        }
    }

    private fun showPrimaryPasteActionMode(anchorView: View) {
        if (getPrimaryClipboardText().isNullOrBlank()) return
        anchorView.startActionMode(primaryPasteActionModeCallback, ActionMode.TYPE_FLOATING)
    }

    private fun getPrimaryClipboardText(): String? {
        val clipboardManager: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboardManager.primaryClip?.getItemAt(0)?.coerceToText(requireContext())?.toString()
    }

    private val primaryPasteActionModeCallback: ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean {
                val pasteTitle: String = getString(android.R.string.paste)
                menu.add(Menu.NONE, android.R.id.paste, Menu.NONE, pasteTitle).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                return true
            }

            override fun onPrepareActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean = false

            override fun onActionItemClicked(
                mode: ActionMode,
                item: MenuItem,
            ): Boolean {
                if (item.itemId == android.R.id.paste) {
                    val pasteText: String = getPrimaryClipboardText() ?: return false
                    applyPrimaryPastedValue(pasteText)
                    mode.finish()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }

    private fun applyPrimaryPastedValue(rawText: String) {
        val normalizedText: String = rawText.trim().replace(",", "")
        val pastedValue: String = normalizedText.filter { it.isDigit() || it == '.' }
        if (pastedValue.isBlank()) return
        pendingWeb3ShortcutPercentage = null
        v = pastedValue
        updateUI()
    }

    private fun currentInputAmount(): String {
        return if (isReverse) {
            binding.minorTv.text.toString().split(" ").getOrNull(1)?.replace(",", "") ?: "0"
        } else {
            v
        }
    }

    private fun shouldOfferLegacyWeb3FeeOption(): Boolean {
        return BuildConfig.DEBUG
    }

    private fun web3SpendableBalance(): BigDecimal {
        val transferToken = web3Token ?: return BigDecimal.ZERO
        val transferBalance = transferToken.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val selectedGaslessFee = currentGaslessFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return when {
            shouldUseGaslessFlow() && currentGaslessFee?.token?.assetId == transferToken.assetId -> {
                if (transferToken.isNativeSolAsset()) {
                    nativeSolSpendableBalance(transferBalance, selectedGaslessFee)
                } else {
                    transferBalance.subtract(selectedGaslessFee).max(BigDecimal.ZERO)
                }
            }
            shouldUseGaslessFlow() && transferToken.isNativeSolAsset() -> {
                nativeSolSpendableBalance(transferBalance)
            }
            transferToken.assetId == chainToken?.assetId -> {
                if (transferToken.isNativeSolAsset()) {
                    nativeSolSpendableBalance(transferBalance, gas ?: BigDecimal.ZERO)
                } else {
                    transferBalance.subtract(gas ?: BigDecimal.ZERO).max(BigDecimal.ZERO)
                }
            }
            transferToken.isNativeSolAsset() -> {
                nativeSolSpendableBalance(transferBalance)
            }
            else -> transferBalance
        }
    }

    private fun hasNativeGasIssue(amount: String): Boolean {
        val token = web3Token ?: return false
        val chainAsset = chainToken ?: return true
        val gasAmount = gas ?: return true
        val chainBalance = chainAsset.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val inputAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return if (token.isNativeSolAsset()) {
            inputAmount > nativeSolSpendableBalance(chainBalance, gasAmount)
        } else if (chainAsset.isNativeSolAsset()) {
            !hasSolBalanceAfterFeeAndRent(chainBalance, gasAmount)
        } else if (token.assetId == chainAsset.assetId) {
            chainBalance < gasAmount.add(amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        } else {
            chainBalance < gasAmount
        }
    }

    private fun isGaslessFeeEnough(amount: String): Boolean {
        val transferToken = web3Token ?: return false
        val fee = currentGaslessFee ?: return false
        val feeTokenBalance = gaslessFeeToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val transferBalance = transferToken.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val inputAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val feeAmount = fee.fee.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return if (fee.token.assetId == transferToken.assetId) {
            if (transferToken.isNativeSolAsset()) {
                inputAmount <= nativeSolSpendableBalance(transferBalance, feeAmount)
            } else {
                transferBalance >= inputAmount.add(feeAmount)
            }
        } else {
            val transferEnough = if (transferToken.isNativeSolAsset()) {
                inputAmount <= nativeSolSpendableBalance(transferBalance)
            } else {
                transferBalance >= inputAmount
            }
            val feeEnough = if (isNativeSolAsset(fee.token.chainId, fee.token.assetId)) {
                hasSolBalanceAfterFeeAndRent(feeTokenBalance, feeAmount)
            } else {
                feeTokenBalance >= feeAmount
            }
            transferEnough && feeEnough
        }
    }

    private fun hasGaslessFeeSelection(): Boolean {
        return transferType == TransferType.WEB3 &&
            currentGaslessFee != null
    }

    private fun isLegacyWeb3FeeSelection(): Boolean {
        return currentGaslessFee?.source == NetworkFee.Source.LEGACY_WEB3
    }

    private fun nativeWeb3FeeOption(): NetworkFee? {
        val nativeToken = chainToken ?: return null
        val nativeGas = gas ?: return null
        return NetworkFee(
            token = nativeToken.toTokenItem(),
            fee = nativeGas.toPlainString(),
            source = NetworkFee.Source.LEGACY_WEB3,
        )
    }

    private fun web3FeeOptions(): List<NetworkFee> {
        val options = mutableListOf<NetworkFee>()
        if (shouldOfferLegacyWeb3FeeOption()) {
            nativeWeb3FeeOption()?.let(options::add)
        }
        options.addAll(gaslessFees)
        return options
    }

    private fun syncSelectedWeb3Fee() {
        val options = web3FeeOptions()
        if (options.isEmpty()) {
            currentGaslessFee = null
            return
        }
        val matchedOption = currentGaslessFee?.selectionKey?.let { selectionKey ->
            options.firstOrNull { it.selectionKey == selectionKey }
        }
        val nextSelection = when {
            matchedOption != null -> matchedOption
            !hasManuallySelectedWeb3Fee && shouldOfferLegacyWeb3FeeOption() -> nativeWeb3FeeOption() ?: options.first()
            !hasManuallySelectedWeb3Fee -> options.first()
            else -> options.first()
        }
        if (currentGaslessFee != nextSelection) {
            currentGaslessFee = nextSelection
        }
    }

    private fun shouldUseGaslessFlow(): Boolean {
        return hasGaslessFeeSelection() &&
            !isLegacyWeb3FeeSelection()
    }

    private fun updateWeb3AvailableBalance() {
        val binding = bindingOrNull() ?: return
        val transferToken = web3Token ?: return
        val displayBalance = if (transferToken.isNativeSolAsset()) {
            web3SpendableBalance()
        } else {
            when {
                shouldUseGaslessFlow() && currentGaslessFee?.token?.assetId == transferToken.assetId -> {
                    (tokenBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                        .subtract(currentGaslessFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                        .max(BigDecimal.ZERO)
                }
                transferToken.assetId == chainToken?.assetId -> {
                    (tokenBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                        .subtract(gas ?: BigDecimal.ZERO)
                        .max(BigDecimal.ZERO)
                }
                else -> tokenBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO
            }
        }
        val balanceText =
            if (transferToken.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                displayBalance.numberFormat8()
            } else {
                displayBalance.numberFormat12()
            }
        binding.balanceTv.text = getString(R.string.available_balance, "$balanceText $tokenSymbol")
    }

    private fun updateWeb3FeeDisplay() {
        val binding = bindingOrNull() ?: return
        val token = web3Token ?: return
        if (binding.loadingProgressBar.isVisible) return
        val feeOptions = web3FeeOptions()
        if (hasGaslessFeeSelection()) {
            val selectedGaslessFee = currentGaslessFee
            binding.contentTextView.text = selectedGaslessFee?.let {
                "${it.fee.numberFormat8()} ${it.token.symbol}"
            } ?: ""
            binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, selectedGaslessFee?.token?.symbol ?: token.getChainSymbolFromName())
            if (feeOptions.size > 1) {
                binding.iconImageView.isVisible = true
                binding.iconImageView.setImageResource(R.drawable.ic_keyboard_arrow_down)
                binding.infoLinearLayout.setOnClickListener {
                    NetworkFeeBottomSheetDialogFragment.newInstance(
                        ArrayList(feeOptions),
                        currentGaslessFee?.selectionKey,
                    ).apply {
                        callback = { networkFee ->
                            hasManuallySelectedWeb3Fee = true
                            currentGaslessFee = networkFee
                            binding.insufficientFeeBalance.isVisible = false
                            dismiss()
                        }
                    }.show(parentFragmentManager, NetworkFeeBottomSheetDialogFragment.TAG)
                }
            } else {
                binding.iconImageView.isVisible = false
                binding.infoLinearLayout.setOnClickListener(null)
            }
            return
        }
        binding.iconImageView.isVisible = false
        binding.infoLinearLayout.setOnClickListener { }
        binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, chainToken?.symbol ?: token.getChainSymbolFromName())
        binding.contentTextView.text = "${gas?.numberFormat8()} ${chainToken?.symbol ?: token.getChainSymbolFromName()}"
    }

    private fun handleSuccessfulWeb3Transfer() {
        val navController = findNavController()
        val backStackEntryCount = parentFragmentManager.backStackEntryCount
        val currentDestination = navController.currentDestination?.id
        val startDestination = navController.graph.startDestinationId
        val isStartDestination = currentDestination == startDestination || backStackEntryCount <= 1

        if (isStartDestination) {
            requireActivity().finish()
            return
        }
        parentFragmentManager.apply {
            var foundTransferDestFragment = false
            val fragmentCount = backStackEntryCount
            for (i in 0 until fragmentCount) {
                val topFragment = fragments.lastOrNull()
                if (topFragment is TransferDestinationInputFragment) {
                    popBackStackImmediate()
                    foundTransferDestFragment = true
                    break
                } else {
                    popBackStackImmediate()
                }
            }

            if (!foundTransferDestFragment) {
                popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
    }

    private var isFeeWaived = false

    private fun renderTitle(toAddress: String, tag: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val (label, index, _) = web3ViewModel.checkAddressAndGetDisplayName(requireNotNull(toAddress), tag, requireNotNull(token?.chainId ?: web3Token?.chainId)) ?: Triple(null, 0, null)
            isFeeWaived = index == 1 || index == 2 || index == 4  // Privacy(1), Safe(2), Fee-free(4)
            binding.titleView.setLabel(
                getString(R.string.Send_To_Title),
                label,
                content = "$toAddress${tag?.let { ":$it" } ?: ""}".formatPublicKey(16),
                index = index
            )
            label?.let {
                addressLabel = label
            }
        }
    }

    private fun showUserList(
        userList: List<User>,
    ) {
        val t = assetBiometricItem as TransferBiometricItem
        val title = getString(R.string.multisig_receivers_threshold, "${t.threshold}/${t.users.size}")
        UserListBottomSheetDialogFragment.newInstance(ArrayList(userList), title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    private fun noteDialog() {
        editDialog {
            titleText = this@InputFragment.getString(if (currentNote.isNullOrBlank().not()) R.string.Edit_Note else R.string.add_a_note)
            editText = currentNote
            maxTextCount = 140
            allowEmpty = true
            rightAction = { note ->
                if (isAdded) {
                    currentNote = note
                    binding.contentTextView.text =
                        note.ifEmpty { getString(R.string.add_a_note) }
                }
            }
        }
    }

    private val alertDialog by lazy { indeterminateProgressDialog(message = R.string.Please_wait_a_bit) }

    private fun isTwoDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{2}"))
    }

    private fun isEightDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{8}"))
    }

    private var v = "0"
    private var pendingWeb3ShortcutPercentage: BigDecimal? = null

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        if (viewDestroyed()) return
        binding.apply {
            val value =
                if (v.endsWith(".")) {
                    v.substring(0, v.length)
                } else {
                    v
                }
            if (isReverse) {
                if (value == "0") {
                    primaryTv.text = "0 $currencyName"
                    minorTv.text = "0 $tokenSymbol"
                } else {
                    primaryTv.text = "${getNumberFormat(value)} $currencyName"
                    minorTv.text =
                        if (tokenPrice <= BigDecimal.ZERO) {
                            "≈ 0 $tokenSymbol"
                        } else {
                            "≈ ${(value.toBigDecimal().divide(tokenPrice, 8, RoundingMode.UP))} $tokenSymbol"
                        }
                }
            } else {
                val currentValue = tokenPrice.multiply(value.toBigDecimal())
                if (value == "0") {
                    primaryTv.text = "0 $tokenSymbol"
                    minorTv.text = "0 $currencyName"
                } else {
                    primaryTv.text = "$value $tokenSymbol"
                    minorTv.text =
                        "≈ ${getNumberFormat(currentValue.toPlainString())} $currencyName"
                }
            }

            if (value == "0") {
                insufficientBalance.isVisible = false
                insufficientFeeBalance.isVisible = false
                insufficientFunds.isVisible = false
                continueVa.isEnabled = false
                continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                updateAddText()
            } else {
                val v =
                    if (isReverse) {
                        minorTv.text.toString().split(" ")[1].replace(",", "")
                    } else {
                        value
                    }
                scheduleRefreshBtcFeeIfNeeded(v)
                if (isReverse && (v == "0" || BigDecimal(v) == BigDecimal.ZERO)) {
                    insufficientBalance.isVisible = false
                    insufficientFeeBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                    updateAddText()
                } else if (BigDecimal(v) <= BigDecimal.ZERO){
                    insufficientBalance.isVisible = false
                    insufficientFeeBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                    updateAddText()
                } else if (BigDecimal(v) > BigDecimal(tokenBalance) && v != "0") {
                    insufficientBalance.isVisible = true
                    insufficientFeeBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    addTv.text = "${getString(R.string.Add)} ${token?.symbol ?: web3Token?.symbol ?: ""}"
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (transferType != TransferType.WEB3 && (currentFee != null && feeTokensExtra == null ||
                    (currentFee?.token?.assetId == token?.assetId && BigDecimal(v).add(currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > (feeTokensExtra?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)) ||
                    (currentFee?.token?.assetId != token?.assetId && (currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > (feeTokensExtra?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)))
                ) {
                    insufficientFeeBalance.isVisible = true
                    insufficientBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = false
                    addTv.text = "${getString(R.string.Add)} ${currentFee?.token?.symbol ?: ""}"
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (web3Token != null && shouldUseGaslessFlow()) {
                    val gaslessEnough = isGaslessFeeEnough(v)
                    insufficientFeeBalance.isVisible = !gaslessEnough
                    addTv.text = if (gaslessEnough) {
                        ""
                    } else {
                        "${getString(R.string.Add)} ${currentGaslessFee?.token?.symbol.orEmpty()}"
                    }
                    insufficientBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = gaslessEnough
                    continueTv.textColor = requireContext().getColor(if (gaslessEnough) R.color.white else R.color.wallet_text_gray)
                } else if (web3Token != null && hasNativeGasIssue(v)) {
                    insufficientFeeBalance.isVisible = true
                    addTv.text = "${getString(R.string.Add)} ${(chainToken?.symbol ?: web3Token?.getChainSymbolFromName()).orEmpty()}"
                    insufficientBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (!isSolanaToAccountExists && BigDecimal(v) < SOLANA_RENT_EXEMPTION) {
                    insufficientFeeBalance.isVisible = false
                    insufficientBalance.isVisible = false
                    insufficientFunds.isVisible = true
                    continueTv.isEnabled = false
                    addTv.text = ""
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else {
                    insufficientBalance.isVisible = false
                    insufficientFeeBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = true
                    addTv.text = ""
                    continueTv.textColor = requireContext().getColor(R.color.white)
                }
            }
        }

        updatePrimarySize()
        if (transferType == TransferType.WEB3) {
            updateWeb3AvailableBalance()
            updateWeb3FeeDisplay()
        }
        applyFeeUi()
    }

    private fun scheduleRefreshBtcFeeIfNeeded(amount: String) {
        val token: Web3TokenItem = web3Token ?: return
        if (token.chainId != Constants.ChainId.BITCOIN_CHAIN_ID) return
        if (amount.toBigDecimalOrNull() == null) return
        if (amount.toBigDecimalOrNull() == BigDecimal.ZERO) return
        if (isAdjustingBtcAmount) return
        if (lastBtcFeeAmount == amount) return
        lastBtcFeeAmount = amount
        btcFeeRecalculateJob?.cancel()
        btcFeeRecalculateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(300L)
            val currentAmount: String = lastBtcFeeAmount ?: return@launch
            refreshBtcFeeForAmount(currentAmount)
        }
    }

    private suspend fun refreshBtcFeeForAmount(amountBtc: String) {
        val token: Web3TokenItem = web3Token ?: return
        val from: String = fromAddress ?: return
        val to: String = toAddress ?: return
        val localUtxos = web3ViewModel.outputsByAddress(from, token.assetId)
        val currentRate: BigDecimal = rate ?: BigDecimal.ONE
        val currentMiniFee: String? = miniFee
        val tx = try {
            token.buildTransaction(rpc, from, to, amountBtc, localUtxos, currentRate, currentMiniFee)
        } catch (err: InsufficientBtcBalanceException) {
            handleInsufficientBtcBalance(err, amountBtc)
            return
        } catch (err: EmptyUtxoException) {
            return
        } catch (err: Exception) {
            Timber.w(err)
            return
        }
        val newFee: BigDecimal = tx.fee ?: return
        gas = newFee
        binding.contentTextView.text = "${newFee.numberFormat8()} ${chainToken?.symbol ?: token.getChainSymbolFromName()}"
        updateAvailableBalanceForBtcFee()
        updateUI()
    }

    private fun handleInsufficientBtcBalance(error: InsufficientBtcBalanceException, amountBtc: String) {
        val token: Web3TokenItem = web3Token ?: return
        val feeBtc: BigDecimal = error.feeBtc
        val utxoTotalBtc: BigDecimal = error.utxoTotalBtc
        gas = feeBtc
        binding.contentTextView.text = "${feeBtc.numberFormat8()} ${chainToken?.symbol ?: token.getChainSymbolFromName()}"
        updateAvailableBalanceForBtcFee()
        if (isReverse) {
            updateUI()
            return
        }
        val availableBalance: BigDecimal = token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val desiredAmount: BigDecimal = amountBtc.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (desiredAmount > availableBalance) {
            updateUI()
            return
        }
        val maxSendable: BigDecimal = utxoTotalBtc.subtract(feeBtc).max(BigDecimal.ZERO)
        isAdjustingBtcAmount = true
        v = maxSendable.setScale(8, RoundingMode.DOWN).toPlainString()
        isAdjustingBtcAmount = false
        updateUI()
    }

    private fun updateAvailableBalanceForBtcFee() {
        val binding = bindingOrNull() ?: return
        val token: Web3TokenItem = web3Token ?: return
        if (token.chainId != Constants.ChainId.BITCOIN_CHAIN_ID) return
        val reservedFee: BigDecimal = gas ?: return
        val availableBalance: BigDecimal = tokenBalance.toBigDecimalOrNull() ?: return
        val availableAfterFee: BigDecimal = availableBalance.subtract(reservedFee).max(BigDecimal.ZERO)
        binding.balanceTv.text = getString(R.string.available_balance, "${availableAfterFee.numberFormat8()} $tokenSymbol")
    }

    private fun handleBtcBuildTransactionError(amountBtc: String) {
        val token: Web3TokenItem = web3Token ?: return
        val availableBalance: BigDecimal = token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val reservedFee: BigDecimal = gas ?: miniFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val maxSendable: BigDecimal = availableBalance.subtract(reservedFee).max(BigDecimal.ZERO)
        val desiredAmount: BigDecimal = amountBtc.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (desiredAmount <= maxSendable) {
            toast(R.string.insufficient_balance)
            return
        }
        if (isReverse) {
            toast(R.string.insufficient_balance)
            return
        }
        if (maxSendable == BigDecimal.ZERO) {
            toast(R.string.insufficient_balance)
            return
        }
        isAdjustingBtcAmount = true
        v = maxSendable.setScale(8, RoundingMode.DOWN).toPlainString()
        isAdjustingBtcAmount = false
        updateUI()
    }

    private fun updateAddText() {
        val binding = bindingOrNull() ?: return
        if (transferType == TransferType.WEB3 && shouldUseGaslessFlow()) {
            if (!isGaslessFeeEnough(currentInputAmount())) {
                binding.addTv.text = "${getString(R.string.Add)} ${currentGaslessFee?.token?.symbol ?: ""}"
            } else {
                binding.addTv.text = ""
            }
        } else if (gas != null && chainToken != null) {
            val chainBalance = chainToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val insufficientGas = if (chainToken?.isNativeSolAsset() == true) {
                !hasSolBalanceAfterFeeAndRent(chainBalance, gas ?: BigDecimal.ZERO)
            } else {
                chainBalance < (gas ?: BigDecimal.ZERO)
            }
            if (insufficientGas) {
                binding.addTv.text = "${getString(R.string.Add)} ${chainToken?.symbol ?: ""}"
            } else {
                binding.addTv.text = ""
            }
        } else if (currentFee != null) {
            if ((feeTokensExtra?.balance?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO) < (currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            ) {
                binding.addTv.text = "${getString(R.string.Add)} ${currentFee?.token?.symbol ?: ""}"
            } else {
                binding.addTv.text = ""
            }
        }
    }

    override
    fun onAddressClick() {
        currentFee?.let {
            when  {
                transferType == TransferType.ADDRESS -> {
                    view?.navigate(
                        R.id.action_input_fragment_to_deposit_fragment,
                        Bundle().apply {
                            putParcelable("args_asset", token)
                            putBoolean("args_hide_network_switch", true)
                        }
                    )
                }

                transferType ==  TransferType.USER -> {
                    view?.navigate(
                        R.id.action_input_fragment_to_deposit_fragment,
                        Bundle().apply {
                            putParcelable("args_asset", token)
                            putBoolean("args_hide_network_switch", true)
                        }
                    )
                }

                transferType == TransferType.WEB3 -> {
                    val address =
                        when (web3Token?.chainId) {
                            Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
                            Constants.ChainId.BITCOIN_CHAIN_ID -> Web3Signer.btcAddress
                            else -> Web3Signer.evmAddress
                        }
                    view?.navigate(
                        R.id.action_input_fragment_to_web3_address_fragment,
                        Bundle().apply {
                            putString("address", address)
                            putParcelable("web3_token", web3Token)
                            putBoolean("args_hide_network_switch", true)
                        }
                    )
                }

                transferType ==  TransferType.BIOMETRIC_ITEM && assetBiometricItem is WithdrawBiometricItem -> {
                    view?.navigate(
                        R.id.action_input_fragment_to_deposit_fragment,
                        Bundle().apply {
                            putParcelable("args_asset", token)
                            putBoolean("args_hide_network_switch", true)
                        }
                    )
                }

                else -> throw IllegalArgumentException("Not supported type")
            }
        }
    }

    override
    fun onWalletClick() {
        ReceiveQrActivity.show(requireContext(), Session.getAccountId()!!)
    }

    private fun getNumberFormat(value: String): String {
        return value.numberFormat2().let {
            if (v.endsWith(".")) {
                "$it."
            } else if (v.endsWith(".00")) {
                "$it.00"
            } else if (v.endsWith(".0")) {
                "$it.0"
            } else if (Regex(".*\\.\\d0$").matches(v)) {
                "${it}0"
            } else {
                it
            }
        }
    }

    private fun updatePrimarySize() {
        if (viewDestroyed()) return
        binding.apply {
            val length = primaryTv.text.length
            val size =
                if (length <= 12) {
                    40f
                } else {
                    max(40f - (length - 8), 16f)
                }
            primaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }
    }

    private fun valueClick(percentageOfBalance: BigDecimal) {
        val isWeb3Max = transferType == TransferType.WEB3 && percentageOfBalance.compareTo(BigDecimal.ONE) == 0
        pendingWeb3ShortcutPercentage =
            if (isWeb3Max && binding.loadingProgressBar.isVisible) {
                percentageOfBalance
            } else {
                null
            }
        val baseValue = when {
            transferType == TransferType.WEB3 -> {
                if (!shouldUseGaslessFlow() && web3Token?.assetId == chainToken?.assetId && gas == null) {
                    if (!dialog.isShowing) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            dialog.show()
                            refreshFee()
                        }
                    }
                    return
                }
                web3SpendableBalance()
            }
            shouldUseGaslessFlow() && web3Token?.assetId == currentGaslessFee?.token?.assetId -> {
                BigDecimal(tokenBalance).subtract(currentGaslessFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
            token != null && token?.assetId == currentFee?.token?.assetId -> {
                BigDecimal(tokenBalance).subtract(BigDecimal(currentFee!!.fee))
            }
            else -> BigDecimal(tokenBalance)
        }

        v = if (isReverse) {
            baseValue.multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
        } else {
            baseValue.toPlainString()
        }
        val targetValue =
            BigDecimal(v)
                .multiply(percentageOfBalance)
                .max(BigDecimal.ZERO)
        v =
            if (!isReverse && isWeb3Max) {
                targetValue.stripTrailingZeros().toPlainString()
            } else {
                targetValue
                    .setScale(8, RoundingMode.DOWN)
                    .stripTrailingZeros()
                    .toPlainString()
            }
        updateUI()
    }

    private suspend fun checkSolanaToExists() {
        val token = web3Token ?: return
        val to = toAddress ?: return
        if (token.chainId != Constants.ChainId.SOLANA_CHAIN_ID || !token.isNativeSolAsset()) return

        val toAccount = withContext(Dispatchers.IO) {
            rpc.getAccountInfo(PublicKey(to))
        }
        isSolanaToAccountExists = toAccount != null
    }

    private suspend fun refreshFee() {
        when (transferType) {
            TransferType.ADDRESS -> {
                refreshFee(token!!)
            }
            TransferType.WEB3 -> {
                refreshWeb3Fees(web3Token!!)
            }
            TransferType.BIOMETRIC_ITEM if assetBiometricItem is WithdrawBiometricItem -> {
                refreshFee(token!!)
            }
            else -> {
                // User free
            }
        }
    }

    private val fees: ArrayList<NetworkFee> = arrayListOf()
    private var currentFee: NetworkFee? = null
        get() = field
        set(value) {
            field = value
            val binding = bindingOrNull()
            if (value != null) {
                if (binding != null && (value.token.assetId == token?.assetId || value.token.assetId == web3Token?.assetId)) {

                    val balance = runCatching {
                        tokenBalance.toBigDecimalOrNull()?.subtract(value.fee.toBigDecimalOrNull() ?: BigDecimal.ZERO)?.max(BigDecimal.ZERO)?.let {
                            if (web3Token == null) { it.numberFormat8() } else { it.numberFormat12() } }
                    }.getOrDefault("0")
                    binding.balanceTv.text = getString(R.string.available_balance, "$balance $tokenSymbol")
                } else if (binding != null) {
                    binding.balanceTv.text = getString(R.string.available_balance, "${tokenBalance.let {
                        if (web3Token == null) { it.numberFormat8() } else { it.numberFormat12() } }
                    } $tokenSymbol")
                }
                binding?.insufficientFeeBalance?.text = getString(R.string.insufficient_gas, value.token.symbol)
            }
            if (binding != null) {
                refreshFeeTokenExtra(value?.token?.assetId)
            }
        }
    private var feeTokensExtra: TokensExtra? = null

    private val gaslessFees: ArrayList<NetworkFee> = arrayListOf()
    private var currentGaslessFee: NetworkFee? = null
        set(value) {
            field = value
            if (view != null) {
                refreshGaslessFeeToken(value?.token?.assetId)
            }
        }
    private var gaslessFeeToken: Web3TokenItem? = null
    private var hasManuallySelectedWeb3Fee = false

    private fun refreshFeeTokenExtra(tokenId: String?) = viewLifecycleOwner.lifecycleScope.launch {
        feeTokensExtra = if (tokenId == null) null
        else web3ViewModel.findTokensExtra(tokenId)
        updateUI()
    }

    private fun refreshGaslessFeeToken(tokenId: String?) = viewLifecycleOwner.lifecycleScope.launch {
        gaslessFeeToken = if (tokenId == null || web3Token == null) {
            null
        } else {
            web3ViewModel.findWeb3TokenItems(requireNotNull(web3Token).walletId).firstOrNull { it.assetId == tokenId }
        }
        updateWeb3FeeDisplay()
        updateUI()
    }

    private var gas: BigDecimal? = null
    private var rate: BigDecimal? = null
    private var miniFee: String? = null

    private var btcFeeRecalculateJob: Job? = null
    private var lastBtcFeeAmount: String? = null
    private var isAdjustingBtcAmount: Boolean = false

    private fun setFeeLoading(isLoading: Boolean) {
        val binding = bindingOrNull() ?: return
        binding.loadingProgressBar.isVisible = isLoading
        binding.contentTextView.isVisible = !isLoading
    }

    private suspend fun refreshFee(t: TokenItem) {
        val binding = bindingOrNull() ?: return
        val toAddress = toAddress?: return
        setFeeLoading(true)
        val feeResponse = runCatching { web3ViewModel.getFees(t.assetId, toAddress) }.getOrNull()
        if (feeResponse == null) {
            delay(3000)
            refreshFee(t)
        } else if (feeResponse.isSuccess) {
            val ids = feeResponse.data?.mapNotNull { it.assetId }
            val tokens = web3ViewModel.findTokenItems(ids ?: emptyList())
            fees.clear()
            fees.addAll(
                feeResponse.data!!.mapNotNull { d ->
                    tokens.find { t -> t.assetId == d.assetId }?.let {
                        NetworkFee(it, d.amount!!)
                    }
                },
            )
            if (fees.size > 1) {
                binding.iconImageView.isVisible = true
                binding.iconImageView.setImageResource(R.drawable.ic_keyboard_arrow_down)
                binding.infoLinearLayout.setOnClickListener {
                    NetworkFeeBottomSheetDialogFragment.newInstance(
                        fees,
                        currentFee?.token?.assetId
                    ).apply {
                        callback = { networkFee ->
                            currentFee = networkFee
                            binding.contentTextView.text = "${BigDecimal(networkFee.fee).numberFormat8()} ${networkFee.token.symbol}"
                            binding.insufficientFeeBalance.isVisible = false
                            updateUI()
                            dismiss()
                        }
                    }.show(parentFragmentManager, NetworkFeeBottomSheetDialogFragment.TAG)
                }
            } else {
                binding.iconImageView.isVisible = false
                binding.infoLinearLayout.setOnClickListener {
                    // do nothing
                }
            }
            fees.firstOrNull()?.let {
                currentFee = it
                binding.contentTextView.text = "${it.fee.numberFormat8()} ${it.token.symbol}"
                updateUI()
            }
        } else {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
        setFeeLoading(false)
    }

    private fun prepareCheck(item: BiometricItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val amount = item.amount
            val rawTransaction = web3ViewModel.firstUnspentTransaction()
            if (rawTransaction != null) {
                WaitingBottomSheetDialogFragment.newInstance()
                    .showNow(parentFragmentManager, WaitingBottomSheetDialogFragment.TAG)
            } else {
                checkUtxo(amount) {
                    prepareTransferBottom(amount, item)
                }
            }
        }
    }

    private fun checkUtxo(amount: String, callback: () -> Unit) {
        val token = token ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val consolidationAmount = web3ViewModel.checkUtxoSufficiency(token.assetId, amount)
            if (consolidationAmount != null) {
                UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), token, consolidationAmount, UUID.randomUUID().toString(), null, null))
                    .show(parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
            } else {
                callback.invoke()
            }
        }
    }

    private fun prepareTransferBottom(amount: String, item: BiometricItem) =
        viewLifecycleOwner.lifecycleScope.launch {
            val t = item
            if (t !is TransferBiometricItem && t !is AddressTransferBiometricItem && t !is WithdrawBiometricItem) {
                return@launch
            }
            val asset = t.asset ?: return@launch

            if (item is WithdrawBiometricItem) {
                val address = t.address
                val dust = address.dust?.toBigDecimalOrNull()
                val amountDouble = amount.toBigDecimalOrNull()
                if (dust != null && amountDouble != null && amountDouble < dust) {
                    toast(getString(R.string.withdrawal_minimum_amount, address.dust, asset.symbol))
                    return@launch
                }
            }

            val memo = currentNote ?: ""
            if (memo.toByteArray().size > 140) {
                toast("$currentNote ${getString(R.string.Content_too_long)}")
                return@launch
            }

            val pair =
                if (t is TransferBiometricItem && t.users.size == 1) {
                    web3ViewModel.findLatestTrace(t.users.first().userId, null, null, amount, asset.assetId)
                } else if (t is WithdrawBiometricItem) {
                    web3ViewModel.findLatestTrace(null, t.address.destination, t.address.tag, amount, asset.assetId)
                } else {
                    Pair(null, false)
                }
            if (pair.second) {
                return@launch
            }
            if (t is TransferBiometricItem && t.users.size == 1) {
                t.trace = pair.first
            } else if (t is WithdrawBiometricItem) {
                t.trace = pair.first
            }

            if (t is WithdrawBiometricItem) {
                val fee = requireNotNull(currentFee) { "withdrawal currentFee can not be null" }
                t.fee = fee
            }
            TransferBottomSheetDialogFragment.newInstance(t).apply {
                setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                    override fun onDismiss(success: Boolean) {
                        if (success) {
                            val navController = findNavController()
                            val backStackEntryCount = parentFragmentManager.backStackEntryCount

                            val currentDestination = navController.currentDestination?.id
                            val startDestination = navController.graph.startDestinationId
                            val isStartDestination = currentDestination == startDestination || backStackEntryCount <= 1

                            if (isStartDestination) {
                                requireActivity().finish()
                            } else {
                                parentFragmentManager.apply {
                                    var foundTransferDestFragment = false
                                    val fragmentCount = backStackEntryCount
                                    for (i in 0 until fragmentCount) {
                                        val topFragment = fragments.lastOrNull()
                                        if (topFragment is TransferDestinationInputFragment) {
                                            // Found TransferDestinationInputFragment, pop it too
                                            popBackStackImmediate()
                                            foundTransferDestFragment = true
                                            break
                                        } else {
                                            popBackStackImmediate()
                                        }
                                    }

                                    if (!foundTransferDestFragment) {
                                        popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                                    }
                                }
                            }
                        }
                    }
                })
            }.show(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        }

    private suspend fun refreshWeb3Fees(t: Web3TokenItem) {
        setFeeLoading(true)
        try {
            refreshGas(t)
            refreshGaslessFees(t)
            syncSelectedWeb3Fee()
        } finally {
            setFeeLoading(false)
            pendingWeb3ShortcutPercentage?.let { pendingPercentage ->
                pendingWeb3ShortcutPercentage = null
                valueClick(pendingPercentage)
            }
            updateWeb3FeeDisplay()
            applyFeeUi()
            updateUI()
        }
    }

    private suspend fun refreshGas(t: Web3TokenItem) {
        val binding = bindingOrNull() ?: return
        val toAddress = toAddress?: return
        val fromAddress = fromAddress ?: return
        if (t.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
            jobManager.addJobInBackground(RefreshWeb3BitCoinJob(t.walletId))
        }
        val transaction =
            try {
                if (t.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                    t.buildTransaction(rpc, fromAddress, toAddress, (tokenBalance.toBigDecimalOrNull()?: BigDecimal.ZERO).divide(BigDecimal.valueOf(2L)).setScale(8, RoundingMode.CEILING).toPlainString(), web3ViewModel.outputsByAddress(fromAddress, t.assetId), rate, miniFee)
                } else {
                    t.buildTransaction(rpc, fromAddress, toAddress, tokenBalance)
                }
            } catch (e: Exception) {
                Timber.w(e)
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                null
            }
        if (transaction == null) {
            val handledByBtcFallback = t.chainId == Constants.ChainId.BITCOIN_CHAIN_ID &&
                isAdded &&
                applyFallbackBtcFeeWithoutRawTransaction(t)
            if (handledByBtcFallback) {
                binding.iconImageView.isVisible = false
                binding.contentTextView.isVisible = true
                binding.loadingProgressBar.isVisible = false
                applyFeeUi()
                return
            }
            delay(3000)
            refreshGas(t)
            return
        } else if (isAdded) {
            val estimate= web3ViewModel.calcFee(t, transaction, fromAddress)
            gas = estimate.fee
            rate = estimate.rate
            miniFee = estimate.minFee
            if (gas == null) {
                val handledByBtcFallback = t.chainId == Constants.ChainId.BITCOIN_CHAIN_ID &&
                    applyFallbackBtcFeeWithoutRawTransaction(t)
                if (handledByBtcFallback) {
                    binding.iconImageView.isVisible = false
                    binding.contentTextView.isVisible = true
                    binding.loadingProgressBar.isVisible = false
                    applyFeeUi()
                    return
                }
                delay(3000)
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                refreshGas(t)
                return
            }
            if (chainToken?.assetId == t.assetId) {
                val balance = runCatching {
                    tokenBalance.toBigDecimalOrNull()?.subtract(gas ?: BigDecimal.ZERO)
                        ?.max(BigDecimal.ZERO)?.let {
                        if (web3Token == null) {
                            it.numberFormat8()
                        } else {
                            it.numberFormat12()
                        }
                    }

                }.getOrDefault("0")
                binding.balanceTv.text = getString(R.string.available_balance, "$balance $tokenSymbol")
            } else {
                binding.balanceTv.text = getString(
                    R.string.available_balance,
                    "${
                        tokenBalance.let {
                            if (web3Token == null) {
                                it.numberFormat8()
                            } else {
                                it.numberFormat12()
                            }
                        }
                    } $tokenSymbol"
                )
            }
            updateUI()
            if (dialog.isShowing) {
                dialog.dismiss()
                v = if (isReverse) {
                        BigDecimal(tokenBalance).subtract(gas).multiply(tokenPrice)
                            .setScale(2, RoundingMode.DOWN).toPlainString()
                    } else {
                        BigDecimal(tokenBalance).subtract(gas).toPlainString()
                    }
                updateUI()
            }
        }
    }

    private fun bindingOrNull(): FragmentInputBinding? {
        return if (view == null) {
            null
        } else {
            binding
        }
    }

    private suspend fun refreshGaslessFees(t: Web3TokenItem) {
        val fromAddress = fromAddress ?: return
        val toAddress = toAddress ?: return
        val response = runCatching {
            web3ViewModel.gaslessFee(
                GaslessFeeRequest(
                    from = fromAddress,
                    to = toAddress,
                    assetId = t.assetId,
                    chainId = t.chainId,
                ),
            )
        }.getOrNull() ?: return
        if (!response.isSuccess || response.data == null) return

        val feeItems = response.data!!.fees.mapNotNull { estimate ->
            val asset = web3ViewModel.findOrSyncAsset(estimate.assetId) ?: return@mapNotNull null
            NetworkFee(
                token = asset,
                fee = estimate.amount,
                source = NetworkFee.Source.GASLESS,
            )
        }
        gaslessFees.clear()
        gaslessFees.addAll(feeItems)
    }

    private suspend fun submitGaslessTransfer(pin: String) {
        val token = requireNotNull(web3Token)
        val fromAddress = requireNotNull(fromAddress)
        val toAddress = requireNotNull(toAddress)
        val fee = requireNotNull(currentGaslessFee) { "gasless fee asset is required" }
        val amount = currentInputAmount()
        val response = web3ViewModel.gaslessPrepare(
            GaslessTxRequest(
                from = fromAddress,
                to = toAddress,
                assetId = token.assetId,
                amount = amount,
                feeAssetId = fee.token.assetId,
                feeAmount = fee.fee.stripAmountZero(),
                chainId = token.chainId,
            ),
        )
        if (!response.isSuccess || response.data == null) {
            throw IllegalStateException(response.errorDescription)
        }
        val payload = response.data!!
        val privateKey = web3ViewModel.getWeb3Priv(requireContext(), pin, token.chainId)
        when (token.chainId) {
            Constants.ChainId.SOLANA_CHAIN_ID -> submitSolanaGaslessTransfer(
                token = token,
                amount = amount,
                fromAddress = fromAddress,
                toAddress = toAddress,
                payload = payload.payload,
                privateKey = privateKey,
            )
            in Constants.Web3EvmChainIds -> submitEvmGaslessTransfer(
                token = token,
                fromAddress = fromAddress,
                toAddress = toAddress,
                amount = amount,
                chainId = payload.chainId,
                payload = payload.payload,
                privateKey = privateKey,
            )
            else -> throw IllegalArgumentException("Gasless is not supported for ${token.chainId}")
        }
        requireContext().defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
        requireContext().updatePinCheck()
    }

    private suspend fun submitSolanaGaslessTransfer(
        token: Web3TokenItem,
        amount: String,
        fromAddress: String,
        toAddress: String,
        payload: JsonElement,
        privateKey: ByteArray,
    ) {
        val rawPayload = payload.takeIf { it.isJsonPrimitive }?.asString
            ?: throw IllegalStateException("Gasless payload is not a Solana base64 transaction")
        val tx = VersionedTransactionCompat.from(rawPayload)
        val signedTx = Web3Signer.signSolanaTransaction(privateKey, tx) {
            rpc.getLatestBlockhash() ?: throw IllegalArgumentException("failed to get blockhash")
        }
        if (!signedTx.allSignerSigned()) {
            throw IllegalStateException("Gasless Solana transaction is not fully signed")
        }
        val rawTx = signedTx.serialize().base64Encode()
        val txHash = signedTx.signatures.firstOrNull { signature ->
            signature != Base58.encode(ByteArray(SIGNATURE_LENGTH))
        } ?: throw IllegalStateException("Gasless Solana transaction signature is missing")
        val now = nowInUtc()
        web3ViewModel.insertSignedPendingTransaction(
            hash = txHash,
            chainId = Constants.ChainId.Solana,
            account = fromAddress,
            assetId = token.assetId,
            amount = amount,
            fee = requireNotNull(currentGaslessFee).fee.stripAmountZero(),
            to = toAddress,
            raw = rawTx,
            createdAt = now,
            updatedAt = now,
        )
        val response = web3ViewModel.postRawTx(
            rawTx = rawTx,
            web3ChainId = Constants.ChainId.Solana,
            account = fromAddress,
            to = toAddress,
            assetId = token.assetId,
            feeType = WEB3_FEE_TYPE_FREE,
        )
        if (!response.isSuccess) {
            throw IllegalStateException(response.errorDescription)
        }
    }

    private suspend fun submitEvmGaslessTransfer(
        token: Web3TokenItem,
        fromAddress: String,
        toAddress: String,
        amount: String,
        chainId: String,
        payload: JsonElement,
        privateKey: ByteArray,
    ) {
        if (!payload.isJsonObject) {
            throw IllegalStateException("Gasless payload is not an EVM object payload")
        }
        val ethPayload = GsonHelper.customGson.fromJson(payload, EthGaslessTxPayload::class.java)
            ?: throw IllegalStateException("Failed to parse gasless EVM payload")
        val userOpSignature = Web3Signer.signEthMessage(
            priv = privateKey,
            message = ethPayload.signing.userOperation.message,
            type = JsSignMessage.TYPE_GASLESS_TRANSFER,
        )
        val eip7702AuthSignature = ethPayload.signing.eip7702Auth
            ?.takeIf { it.required }
            ?.let { auth ->
                if (!auth.address.equals(GASLESS_EIP7702_AUTHORIZED_ADDRESS, ignoreCase = true)) {
                    throw IllegalArgumentException("Unsupported EIP-7702 auth target")
                }
                Web3Signer.signEthMessage(
                    priv = privateKey,
                    message = auth.message,
                    type = JsSignMessage.TYPE_GASLESS_TRANSFER,
                )
            }
        val response = web3ViewModel.submitGaslessTx(
            SubmitGaslessTxRequest(
                chainId = chainId,
                payload = payload,
                userOpSignature = userOpSignature,
                eip7702AuthSignature = eip7702AuthSignature,
            ),
        )
        if (!response.isSuccess) {
            throw IllegalStateException(response.errorDescription)
        }
        val sponsorTxId = response.data?.sponsorTxId ?: throw IllegalStateException("Missing sponsor tx id")
        val now = nowInUtc()
        web3ViewModel.insertGaslessPendingTransaction(
            sponsorTxId = sponsorTxId,
            chainId = chainId,
            account = fromAddress,
            assetId = token.assetId,
            amount = amount,
            fee = requireNotNull(currentGaslessFee).fee.stripAmountZero(),
            to = toAddress,
            nonce = ethPayload.userOperation.nonce,
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun applyFallbackBtcFeeWithoutRawTransaction(t: Web3TokenItem): Boolean {
        val estimate = web3ViewModel.estimateBtcFeeRate(currentRate = rate?.toPlainString()) ?: return false
        rate = estimate.feeRate?.toBigDecimalOrNull() ?: rate
        miniFee = estimate.minFee ?: miniFee
        val fallbackFee: BigDecimal = estimate.minFee?.toBigDecimalOrNull()?.movePointLeft(1) ?: return false
        gas = fallbackFee
        binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, chainToken?.symbol)
        binding.contentTextView.text = "${fallbackFee.numberFormat8()} ${chainToken?.symbol ?: t.getChainSymbolFromName()}"
        updateAvailableBalanceForBtcFee()
        updateUI()
        return true
    }

    private val dialog by lazy {
        indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
            dismiss()
        }
    }
}
