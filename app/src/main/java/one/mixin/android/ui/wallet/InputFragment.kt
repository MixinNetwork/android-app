package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainSymbolFromName
import one.mixin.android.db.web3.vo.isSolToken
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.address.ReceiveSelectionBottom.OnReceiveSelectionClicker
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment.Companion.TYPE_RECEIVE_QR
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.WaitingBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapActivity
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.toUser
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.widget.Keyboard
import org.sol4k.PublicKey
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
        const val ARGS_TO_ADDRESS = "args_to_address"
        const val ARGS_FROM_ADDRESS = "args_from_address"

        const val ARGS_TO_WALLET = "args_to_wallet"

        const val ARGS_TO_MY_WALLET = "args_to_my_wallet"
        const val ARGS_TO_ACCOUNT = "args_to_account"

        const val ARGS_TO_ADDRESS_TAG = "args_to_address_tag"
        const val ARGS_TO_ADDRESS_ID = "args_to_address_id"
        const val ARGS_TO_ADDRESS_LABEL = "args_to_address_label"

        const val ARGS_TO_USER = "args_to_user"

        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_WEB3_CHAIN_TOKEN = "args_web3_chain_token"
        const val ARGS_TOKEN = "args_token"

        const val ARGS_RECEIVE = "args_receive"

        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"

        enum class TransferType {
            USER,
            ADDRESS,
            WEB3,
            BIOMETRIC_ITEM
        }

        fun newInstance(
            fromAddress: String,
            toAddress: String,
            web3Token: Web3TokenItem,
            chainToken: Web3TokenItem,
            label: String? = null,
            toWallet: Boolean = false
        ) =
            InputFragment().apply {
                withArgs {
                    Timber.e("chain ${chainToken.name} ${web3Token.chainId} ${chainToken.chainId} $fromAddress $toAddress")
                    putString(ARGS_FROM_ADDRESS, fromAddress)
                    putString(ARGS_TO_ADDRESS, toAddress)
                    putParcelable(ARGS_WEB3_TOKEN, web3Token)
                    putParcelable(ARGS_WEB3_CHAIN_TOKEN, chainToken)
                    putString(ARGS_TO_ADDRESS_LABEL, label)
                    putBoolean(ARGS_TO_WALLET, toWallet)
                }
            }

        fun newInstance(
            tokenItem: TokenItem,
            toAddress: String,
            tag: String? = null,
            toAccount: Boolean? = null,
            isReceive: Boolean = false,
            label: String? = null
        ) =
            InputFragment().apply {
                withArgs {
                    if (toAccount != null) {
                        putBoolean(ARGS_TO_ACCOUNT, toAccount)
                    }
                    putParcelable(ARGS_TOKEN, tokenItem)
                    putString(ARGS_TO_ADDRESS, toAddress)
                    putString(ARGS_TO_ADDRESS_TAG, tag)
                    putBoolean(ARGS_RECEIVE, isReceive)
                    if (label != null) {
                        putString(ARGS_TO_ADDRESS_LABEL, label)
                    }
                }
            }

        fun newInstance(
            tokenItem: TokenItem,
            address: Address,
        ) =
            InputFragment().apply {
                withArgs {
                    putParcelable(ARGS_TOKEN, tokenItem)
                    putString(ARGS_TO_ADDRESS, address.destination)
                    putString(ARGS_TO_ADDRESS_TAG, address.tag)
                    putString(ARGS_TO_ADDRESS_ID, address.addressId)
                    putString(ARGS_TO_ADDRESS_LABEL, address.label)
                }
            }

        fun newInstance(
            tokenItem: TokenItem,
            user: User
        ) =
            InputFragment().apply {
                withArgs {
                    putParcelable(ARGS_TOKEN, tokenItem)
                    putParcelable(ARGS_TO_USER, user)
                }
            }

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            InputFragment().apply {
                withArgs {
                    putParcelable(ARGS_BIOMETRIC_ITEM, t)
                }
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

    private val addressLabel by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_LABEL) ?: (assetBiometricItem as? WithdrawBiometricItem)?.address?.label
    }

    private val addressId by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_ID) ?: (assetBiometricItem as? WithdrawBiometricItem)?.address?.addressId
    }

    private val isReceive by lazy {
        requireArguments().getBoolean(ARGS_RECEIVE, false)
    }

    private val toWallet by lazy {
        requireArguments().getBoolean(ARGS_TO_WALLET, false)
    }

    private val toMyWallet by lazy {
        requireArguments().getBoolean(ARGS_TO_MY_WALLET, false)
    }

    private val toAccount by lazy {
        requireArguments().getBoolean(ARGS_TO_ACCOUNT, false)
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
        binding.root.hideKeyboard()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(SyncOutputJob())
        lifecycleScope.launch {
            binding.apply {
                if (requireActivity() !is WalletActivity){
                    root.fitsSystemWindows = false
                }
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.rightIb.setOnClickListener {
                    requireContext().openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }
                binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, getString(R.string.Token))
                binding.insufficientFunds.text = getString(R.string.send_sol_for_rent, "0.00203928")
                when (transferType) {
                    TransferType.USER -> {
                        titleView.setSubTitle(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), user)
                    }
                    TransferType.ADDRESS -> {
                        if (addressLabel.isNullOrBlank()) {
                            titleView.setLabel(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), if (toAccount) getString(R.string.Common_Wallet) else null, "$toAddress${addressTag?.let { ":$it" } ?: ""}".formatPublicKey(16), toMyWallet)
                        } else {
                            titleView.setLabel(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), addressLabel, "" , toMyWallet)
                        }
                    }
                    TransferType.WEB3 -> {
                        titleView.setLabel(
                            getString(if (isReceive) R.string.Receive else R.string.Send_To_Title),
                            addressLabel ?: if (toWallet) getString(R.string.Privacy_Wallet) else null,
                            toAddress ?: "",
                            toMyWallet
                        )
                    }
                    TransferType.BIOMETRIC_ITEM -> {
                        assetBiometricItem?.let { item ->
                            when {
                                item is WithdrawBiometricItem -> {
                                    titleView.setLabel(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), addressLabel, "")
                                }
                                item is AddressTransferBiometricItem -> {
                                    titleView.setLabel(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), null, (if (toAddress == null) item.address else "$toAddress${addressTag?.let { ":$it" } ?: ""}").formatPublicKey(16))
                                }
                                item is TransferBiometricItem -> {
                                    titleView.setSubTitle(getString(if (isReceive) R.string.Receive else R.string.Send_To_Title), item.users) {
                                        showUserList(item.users)
                                    }
                                }

                                else -> {
                                    titleView.setSubTitle(
                                        getString(if (isReceive) R.string.Receive else R.string.Send_To_Title),
                                        ""
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
                keyboard.tipTitleEnabled = false
                keyboard.disableNestedScrolling()
                keyboard.setOnClickKeyboardListener(
                    object : Keyboard.OnClickKeyboardListener {
                        override fun onKeyClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.tickVibrate()
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
                balance.text = getString(R.string.available_balance, "${tokenBalance.let {
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
                        if (web3Token != null) {
                            AddFeeBottomSheetDialogFragment.newInstance(web3Token!!)
                                .apply {
                                    onWeb3Action = { type, t ->
                                        if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                            SwapActivity.show(
                                                requireActivity(),
                                                input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                                output = t.assetId,
                                                null,
                                                null,
                                                walletId = JsSigner.currentWalletId,
                                                inMixin = false
                                            )
                                        } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                            val address =
                                                if (web3Token?.chainId == Constants.ChainId.SOLANA_CHAIN_ID) JsSigner.solanaAddress else JsSigner.evmAddress
                                            this@InputFragment.view?.navigate(
                                                R.id.action_input_fragment_to_web3_address_fragment,
                                                Bundle().apply {
                                                    putString("address", address)
                                                }
                                            )
                                        }
                                    }
                                }.showNow(
                                    parentFragmentManager,
                                    AddFeeBottomSheetDialogFragment.TAG
                                )
                        } else if (token != null) {
                            AddFeeBottomSheetDialogFragment.newInstance(token!!)
                                .apply {
                                    onAction = { type, t ->
                                        if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
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
                                                }
                                            )
                                        }
                                    }
                                }.showNow(
                                    parentFragmentManager,
                                    AddFeeBottomSheetDialogFragment.TAG
                                )
                        }
                    } else if (gas != null && chainToken != null) {
                        AddFeeBottomSheetDialogFragment.newInstance(chainToken!!)
                            .apply {
                                onWeb3Action = { type, t ->
                                    if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                        SwapActivity.show(
                                            requireActivity(),
                                            input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                            output = t.assetId,
                                            null,
                                            null,
                                            inMixin = false
                                        )
                                    } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                        val address =
                                            if (web3Token?.chainId == Constants.ChainId.SOLANA_CHAIN_ID) JsSigner.solanaAddress else JsSigner.evmAddress
                                        this@InputFragment.view?.navigate(
                                            R.id.action_input_fragment_to_web3_address_fragment,
                                            Bundle().apply {
                                                putString("address", address)
                                            }
                                        )
                                    }
                                }
                            }.showNow(
                                parentFragmentManager,
                                AddFeeBottomSheetDialogFragment.TAG
                            )
                    } else if (currentFee != null) {
                        AddFeeBottomSheetDialogFragment.newInstance(currentFee!!.token)
                            .apply {
                                onAction = { type, t ->
                                    if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
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
                            lifecycleScope.launch(
                                CoroutineExceptionHandler { _, error ->
                                    ErrorHandler.Companion.handleError(error)
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
                                    ?: Address(addressId ?: "", "address", assetId, chainId, toAddress, addressLabel ?: "", nowInUtc(), addressTag, null)
                                val trace = (assetBiometricItem as? WithdrawBiometricItem)?.traceId ?: UUID.randomUUID().toString()
                                val networkFee = NetworkFee(feeItem, currentFee?.fee ?: "0")
                                val toWallet = web3ViewModel.anyAddressExists(listOf(address.destination))
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
                                    toWallet
                                )

                                prepareCheck(withdrawBiometricItem)
                            }
                        }
                        transferType == TransferType.WEB3 -> {
                            val token = requireNotNull(web3Token)
                            val fromAddress = requireNotNull(fromAddress)
                            val toAddress = requireNotNull(toAddress)
                            val amount =
                                if (isReverse) {
                                    binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                                } else {
                                    v
                                }
                            lifecycleScope.launch(
                                CoroutineExceptionHandler { _, error ->
                                    ErrorHandler.Companion.handleError(error)
                                    alertDialog.dismiss()
                                },
                            ) {
                                val transaction =
                                    token.buildTransaction(rpc, fromAddress, toAddress, amount)
                                showBrowserBottomSheetDialogFragment(
                                    requireActivity(),
                                    transaction,
                                    token = token,
                                    amount = amount,
                                    toAddress = toAddress,
                                    chainToken = chainToken,
                                    onTxhash = { _, serializedTx ->
                                        val txStateFragment =
                                            TransactionStateFragment.newInstance(
                                                serializedTx,
                                                null
                                            )
                                        navTo(txStateFragment, TransactionStateFragment.TAG)
                                    },
                                    onDismiss = { isDone->
                                        if (isDone) {
                                            this@InputFragment.parentFragmentManager.apply {
                                                if (backStackEntryCount > 0) {
                                                    popBackStack(
                                                        null,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                                                    )
                                                }
                                            }
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
                        if (note.isNotEmpty()) note else getString(R.string.add_a_note)
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
                } else if (
                    web3Token != null && (chainToken == null || gas == null || chainToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO < gas ||
                        (web3Token?.assetId == chainToken?.assetId && (gas ?: BigDecimal.ZERO).add(BigDecimal(v)) > (web3Token?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)))
                ) {
                    insufficientFeeBalance.isVisible = gas != null
                    addTv.text = "${getString(R.string.Add)} ${chainToken?.symbol ?: ""}"
                    insufficientBalance.isVisible = false
                    insufficientFunds.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (!isSolanaToAccountExists && BigDecimal(v) < BigDecimal("0.00203928")) { // rent
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
    }

    private fun updateAddText() {
        if (gas != null && chainToken != null) {
            if ((chainToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < gas) {
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
                        }
                    )
                }

                transferType ==  TransferType.USER -> {
                    view?.navigate(
                        R.id.action_input_fragment_to_deposit_fragment,
                        Bundle().apply {
                            putParcelable("args_asset", token)
                        }
                    )
                }

                transferType == TransferType.WEB3 -> {
                    val address = if (token?.chainId == Constants.ChainId.SOLANA_CHAIN_ID) JsSigner.solanaAddress else JsSigner.evmAddress
                    view?.navigate(
                        R.id.action_input_fragment_to_web3_address_fragment,
                        Bundle().apply {
                            putString("address", address)
                        }
                    )
                }

                transferType ==  TransferType.BIOMETRIC_ITEM && assetBiometricItem is WithdrawBiometricItem -> {
                    view?.navigate(
                        R.id.action_input_fragment_to_deposit_fragment,
                        Bundle().apply {
                            putParcelable("args_asset", token)
                        }
                    )
                }

                else -> throw IllegalArgumentException("Not supported type")
            }
        }
    }

    override
    fun onWalletClick() {
        QrBottomSheetDialogFragment.newInstance(
            Session.getAccountId()!!,
            TYPE_RECEIVE_QR
        ).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
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
                    max(40f - 1 * (length - 8), 16f)
                }
            primaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }
    }

    private fun valueClick(percentageOfBalance: BigDecimal) {
        val baseValue = when {
            web3Token != null && web3Token?.assetId == chainToken?.assetId -> {
                if (gas == null) {
                    if (!dialog.isShowing) {
                        lifecycleScope.launch {
                            dialog.show()
                            refreshFee()
                        }
                    }
                    return
                }
                BigDecimal(tokenBalance).subtract(gas)
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

        v = BigDecimal(v).multiply(percentageOfBalance).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
        updateUI()
    }

    private suspend fun checkSolanaToExists() {
        val token = web3Token ?: return
        val to = toAddress ?: return
        if (token.chainId != Constants.ChainId.SOLANA_CHAIN_ID || !token.isSolToken()) return

        val toAccount = withContext(Dispatchers.IO) {
            rpc.getAccountInfo(PublicKey(to))
        }
        isSolanaToAccountExists = toAccount != null
    }

    private suspend fun refreshFee() {
        when  {
            transferType == TransferType.ADDRESS -> {
                refreshFee(token!!)
            }
            transferType == TransferType.WEB3 -> {
                refreshGas(web3Token!!)
            }
            transferType == TransferType.BIOMETRIC_ITEM && assetBiometricItem is WithdrawBiometricItem -> {
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
            if (value != null) {
                if (value.token.assetId == token?.assetId || value.token.assetId == web3Token?.assetId) {

                    val balance = runCatching {
                        tokenBalance.toBigDecimalOrNull()?.subtract(value.fee.toBigDecimalOrNull() ?: BigDecimal.ZERO)?.max(BigDecimal.ZERO)?.let {
                            if (web3Token == null) {
                                it.numberFormat8()
                            } else {
                                it.numberFormat12()
                            }
                        }
                    }.getOrDefault("0")

                    binding.balance.text = getString(R.string.available_balance, "$balance $tokenSymbol")
                } else {
                    binding.balance.text = getString(R.string.available_balance, "${tokenBalance.let {
                        if (web3Token == null) { it.numberFormat8() } else { it.numberFormat12() } }
                    } $tokenSymbol")
                }
                binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, value.token.symbol)
            }
            refreshFeeTokenExtra(value?.token?.assetId)
        }
    private var feeTokensExtra: TokensExtra? = null

    private fun refreshFeeTokenExtra(tokenId: String?) = lifecycleScope.launch {
        feeTokensExtra = if (tokenId == null) null
        else web3ViewModel.findTokensExtra(tokenId)
        updateUI()
    }

    private var gas: BigDecimal? = null

    private suspend fun refreshFee(t: TokenItem) {
        val toAddress = toAddress?: return
        binding.loadingProgressBar.isVisible = true
        binding.contentTextView.isVisible = false
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
                updateUI()
                binding.contentTextView.text = "${it.fee.numberFormat8()} ${it.token.symbol}"
            }
        }
        binding.contentTextView.isVisible = true
        binding.loadingProgressBar.isVisible = false
    }

    private fun prepareCheck(item: BiometricItem) {
        lifecycleScope.launch {
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
        lifecycleScope.launch {
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
        lifecycleScope.launch {
            val t = item
            if (t !is TransferBiometricItem && t !is AddressTransferBiometricItem && t !is WithdrawBiometricItem) {
                return@launch
            }
            val asset = t.asset ?: return@launch

            if (item is WithdrawBiometricItem) {
                t as WithdrawBiometricItem
                val address = t.address
                val dust = address.dust?.toBigDecimalOrNull()
                val amountDouble = amount.toBigDecimalOrNull()
                if (dust != null && amountDouble != null && amountDouble < dust) {
                    toast(getString(R.string.withdrawal_minimum_amount, address.dust, asset.symbol))
                    return@launch
                }
            }

            val memo = currentNote?.toString() ?: ""
            if (memo.toByteArray().size > 140) {
                toast("$currentNote ${getString(R.string.Content_too_long)}")
                return@launch
            }

            val traceId = t.traceId
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
                            parentFragmentManager.apply {
                                if (backStackEntryCount > 0) {
                                    popBackStack(
                                        null,
                                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                                    )
                                }
                                val activity = requireActivity()
                                if (backStackEntryCount == 0 && activity is WalletActivity) { // Only pop if no other fragments in back stack
                                    activity.onBackPressedDispatcher.onBackPressed()
                                }
                            }
                        }
                    }
                })
            }.show(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        }

    private suspend fun refreshGas(t: Web3TokenItem) {
        val toAddress = toAddress?: return
        binding.loadingProgressBar.isVisible = true
        binding.contentTextView.isVisible = false
        val fromAddress = fromAddress ?: return
        val transaction =
            try {
                t.buildTransaction(rpc, fromAddress, toAddress, tokenBalance)
            } catch (e: Exception) {
                Timber.Forest.w(e)
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                null
            }
        if (transaction == null) {
          delay(3000)
          refreshGas(t)
        } else if (isAdded) {
            gas = web3ViewModel.calcFee(t, transaction, fromAddress)
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
                binding.balance.text = getString(
                    R.string.available_balance,
                    "$balance $tokenSymbol"
                )
            } else {
                binding.balance.text = getString(
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
            binding.insufficientFeeBalance.text =
                getString(R.string.insufficient_gas, chainToken?.symbol)

            binding.contentTextView.text = "${gas?.numberFormat8()} ${chainToken?.symbol ?: t.getChainSymbolFromName()}"
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
        binding.iconImageView.isVisible = false
        binding.contentTextView.isVisible = true
        binding.loadingProgressBar.isVisible = false
    }

    private val dialog by lazy {
        indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
            dismiss()
        }
    }
}

