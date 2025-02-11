package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.buildTransaction
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.ReceiveSelectionBottom
import one.mixin.android.ui.address.ReceiveSelectionBottom.OnReceiveSelectionClicker
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment.Companion.TYPE_RECEIVE_QR
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.web3.receive.Web3ReceiveSelectionFragment
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.max

@AndroidEntryPoint
class InputFragment : BaseFragment(R.layout.fragment_input), OnReceiveSelectionClicker {
    companion object {
        const val TAG = "InputFragment"
        const val ARGS_TO_ADDRESS = "args_to_address"
        const val ARGS_FROM_ADDRESS = "args_from_address"

        const val ARGS_TO_ADDRESS_TAG = "args_to_address_tag"
        const val ARGS_TO_ADDRESS_ID = "args_to_address_id"
        const val ARGS_TO_ADDRESS_LABEL = "args_to_address_label"

        const val ARGS_TO_USER = "args_to_user"

        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_WEB3_CHAIN_TOKEN = "args_web3_chain_token"
        const val ARGS_TOKEN = "args_token"

        const val ARGS_RECEIVE = "args_receive"

        enum class TransferType {
            USER,
            ADDRESS,
            WEB3
        }
        fun newInstance(
            fromAddress: String,
            toAddress: String,
            web3Token: Web3Token,
            chainToken: Web3Token?,
        ) =
            InputFragment().apply {
                withArgs {
                    putString(ARGS_FROM_ADDRESS, fromAddress)
                    putString(ARGS_TO_ADDRESS, toAddress)
                    putParcelable(ARGS_WEB3_TOKEN, web3Token)
                    putParcelable(ARGS_WEB3_CHAIN_TOKEN, chainToken)
                }
            }

        fun newInstance(
            tokenItem: TokenItem,
            toAddress: String,
            tag: String? = null,
            isReceive: Boolean = false
        ) =
            InputFragment().apply {
                withArgs {
                    putParcelable(ARGS_TOKEN, tokenItem)
                    putString(ARGS_TO_ADDRESS, toAddress)
                    putString(ARGS_TO_ADDRESS_TAG, tag)
                    putBoolean(ARGS_RECEIVE, isReceive)
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
    }

    private val transferType: TransferType by lazy {
        when {
            arguments?.containsKey(ARGS_TO_USER) == true -> TransferType.USER
            arguments?.containsKey(ARGS_WEB3_TOKEN) == true -> TransferType.WEB3
            else -> TransferType.ADDRESS
        }
    }

    private val binding by viewBinding(FragmentInputBinding::bind)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var isReverse: Boolean = false

    private val toAddress by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS)
    }

    private val fromAddress by lazy {
        requireArguments().getString(ARGS_FROM_ADDRESS)
    }
    private val user: User? by lazy {
        arguments?.getParcelableCompat(ARGS_TO_USER, User::class.java)
    }
    private val web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3Token::class.java)
    }
    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_CHAIN_TOKEN, Web3Token::class.java)
    }

    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
    }

    private val addressTag by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_TAG)
    }

    private val addressLabel by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_LABEL)
    }

    private val addressId by lazy {
        requireArguments().getString(ARGS_TO_ADDRESS_ID)
    }

    private val isReceive by lazy {
        requireArguments().getBoolean(ARGS_RECEIVE, false)
    }

    private val currencyName by lazy {
        Fiats.getAccountCurrencyAppearance()
    }

    private val tokenPrice: BigDecimal by lazy {
        ((token?.priceUsd ?: web3Token?.price)?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(Fiats.getRate().toBigDecimal())
    }
    private val tokenSymbol by lazy {
        token?.symbol ?: web3Token!!.symbol
    }
    private val tokenIconUrl by lazy {
        token?.iconUrl ?: web3Token!!.iconUrl
    }
    private val tokenChainIconUrl by lazy {
        token?.chainIconUrl ?: web3Token!!.chainIconUrl
    }
    private val tokenBalance by lazy {
        token?.balance ?: web3Token!!.balance
    }
    private val tokenName by lazy {
        token?.name ?: web3Token!!.name
    }

    private var currentNote: String? = null

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
        lifecycleScope.launch {
            binding.apply {
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.rightIb.setOnClickListener {
                    requireContext().openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }
                titleView.setSubTitle(
                    getString(if (isReceive) R.string.Receive else R.string.Send_transfer),
                    when(transferType) {
                        TransferType.WEB3 -> "2/2"
                        else -> {
                            if (addressId.isNullOrBlank().not()) "2/2"
                            else if (addressTag.isNullOrBlank()) "2/2"
                            else "3/3"
                        }
                    }
                )
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
                                } else if (!isReverse && isEightDecimal(v)) {
                                    // do noting
                                    return
                                } else if (isReverse && isTwoDecimal(v)) {
                                    // do noting
                                    return
                                } else if (value == "." && v.contains(".")) {
                                    // do noting
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
                balance.text = getString(R.string.available_balance, "${tokenBalance.numberFormat8()} $tokenSymbol")
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
                when(transferType) {
                    TransferType.USER -> {
                        binding.infoLinearLayout.setOnClickListener {
                            noteDialog()
                        }
                        binding.titleTextView.setText(R.string.Note_Optional)
                        binding.contentTextView.setText(R.string.add_a_note)
                        binding.iconImageView.isVisible = true
                        binding.iconImageView.setImageResource(R.drawable.ic_arrow_right)
                    }
                    else -> {
                        binding.titleTextView.setText(R.string.Network_Fee)
                    }
                }
                continueVa.setOnClickListener {
                    when (transferType) {
                        TransferType.ADDRESS -> {
                            val toAddress = requireNotNull(toAddress)
                            val assetId = requireNotNull(token?.assetId)
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
                                alertDialog.show()
                                if (currentFee == null) {
                                    refreshFee(token!!)
                                    alertDialog.dismiss()
                                    return@launch
                                }
                                feeTokensExtra = web3ViewModel.findTokensExtra(currentFee!!.token.assetId)
                                val feeItem = web3ViewModel.syncAsset(assetId)
                                if (feeItem == null) {
                                    toast(R.string.insufficient_balance)
                                    alertDialog.dismiss()
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
                                    binding.addTv.text = "${getString(R.string.Add)} ${currentFee?.token?.symbol ?: ""}"
                                    binding.addTv.setOnClickListener {
                                        binding.addTv.setOnClickListener {
                                            ReceiveSelectionBottom(
                                                this@InputFragment,
                                            ).apply {
                                                setOnReceiveSelectionClicker(this@InputFragment)
                                            }.show(currentFee!!.token)
                                        }
                                    }
                                    alertDialog.dismiss()
                                    return@launch
                                } else {
                                    binding.insufficientFeeBalance.isVisible = false
                                }

                                alertDialog.dismiss()
                                val address = Address(
                                    addressId ?: "",
                                    "address",
                                    assetId,
                                    toAddress,
                                    addressLabel ?: "Web3 Address",
                                    nowInUtc(),
                                    addressTag,
                                    null
                                )
                                val networkFee = NetworkFee(feeItem, currentFee?.fee ?: "0")
                                val withdrawBiometricItem = WithdrawBiometricItem(
                                    address,
                                    networkFee,
                                    null,
                                    UUID.randomUUID().toString(),
                                    token,
                                    amount,
                                    null,
                                    PaymentStatus.pending.name,
                                    null
                                )
                                TransferBottomSheetDialogFragment.Companion.newInstance(withdrawBiometricItem).apply {
                                    setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                                        override fun onDismiss(success: Boolean) {
                                            if (success) {
                                                parentFragmentManager.apply {
                                                    findFragmentByTag(Web3ReceiveSelectionFragment.Companion.TAG)?.let {
                                                        beginTransaction().remove(it).commit()
                                                    }
                                                    findFragmentByTag(TAG)?.let {
                                                        beginTransaction().remove(it).commit()
                                                    }
                                                }
                                            }
                                        }
                                    })

                                }.show(parentFragmentManager, TransferBottomSheetDialogFragment.Companion.TAG)
                            }
                        }
                        TransferType.WEB3 -> {
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
                                    token.buildTransaction(fromAddress, toAddress, amount)
                                showBrowserBottomSheetDialogFragment(
                                    requireActivity(),
                                    transaction,
                                    token = token,
                                    amount = amount,
                                    toAddress = toAddress,
                                    chainToken = chainToken,
                                    onTxhash = { _, serializedTx ->
                                        val txStateFragment =
                                            TransactionStateFragment.Companion.newInstance(
                                                serializedTx,
                                                null
                                            )
                                        navTo(txStateFragment, TransactionStateFragment.Companion.TAG)
                                    },
                                )
                            }
                        }
                        TransferType.USER -> {
                            val amount =
                                if (isReverse) {
                                    binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                                } else {
                                    v
                                }
                            val user = requireNotNull(user)
                            val biometricItem = buildTransferBiometricItem(user, token, amount, null, memo = currentNote, null)
                            TransferBottomSheetDialogFragment.Companion.newInstance(biometricItem).apply {
                                setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                                    override fun onDismiss(success: Boolean) {
                                        if (success) {
                                            parentFragmentManager.apply {
                                                findFragmentByTag(Web3ReceiveSelectionFragment.Companion.TAG)?.let {
                                                    beginTransaction().remove(it).commit()
                                                }
                                                findFragmentByTag(TAG)?.let {
                                                    beginTransaction().remove(it).commit()
                                                }
                                            }
                                        }
                                    }
                                })

                            }.show(parentFragmentManager, TransferBottomSheetDialogFragment.Companion.TAG)
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
            refreshFee()
        }
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
                continueVa.isEnabled = false
                continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
            } else {
                val v =
                    if (isReverse) {
                        minorTv.text.toString().split(" ")[1].replace(",", "")
                    } else {
                        value
                    }
                if (isReverse && v == "0") {
                    insufficientBalance.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (BigDecimal(v) > BigDecimal(tokenBalance) && v != "0") {
                    insufficientBalance.isVisible = true
                    insufficientFeeBalance.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else if (currentFee != null && feeTokensExtra == null ||
                    (currentFee?.token?.assetId == token?.assetId && BigDecimal(v).add(currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > (feeTokensExtra?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)) ||
                    (currentFee?.token?.assetId != token?.assetId && (currentFee?.fee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > (feeTokensExtra?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO))
                ) {
                    insufficientFeeBalance.isVisible = true
                    insufficientBalance.isVisible = false
                    continueVa.isEnabled = false
                    continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else {
                    insufficientBalance.isVisible = false
                    continueVa.isEnabled = true
                    continueTv.textColor = requireContext().getColor(R.color.white)
                }
            }
        }

        updatePrimarySize()
    }

    override
    fun onAddressClick() {
        currentFee?.let {
            when(transferType){
                TransferType.ADDRESS -> {
                    navTo(DepositFragment.newInstance(token!!), DepositFragment.TAG)
                }
                TransferType.USER -> {
                    navTo(DepositFragment.newInstance(token!!), DepositFragment.TAG)
                }
                TransferType.WEB3 -> {
                    navTo(Web3AddressFragment(), Web3AddressFragment.TAG)
                }
                else -> throw IllegalArgumentException("Not supported type")
            }
        }
    }

    override
    fun onWalletClick() {
        QrBottomSheetDialogFragment.newInstance(
            one.mixin.android.session.Session.getAccountId()!!,
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
            web3Token != null && web3Token?.fungibleId == chainToken?.fungibleId -> {
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

        v = BigDecimal(v).multiply(percentageOfBalance).max(BigDecimal.ZERO).toPlainString()
        updateUI()
    }

    private suspend fun refreshFee() {
        when (transferType) {
            TransferType.ADDRESS -> {
                refreshFee(token!!)
            }
            TransferType.WEB3 -> {
                refreshGas(web3Token!!)
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
                        tokenBalance.toBigDecimalOrNull()?.subtract(value.fee.toBigDecimalOrNull()?: BigDecimal.ZERO)?.max(BigDecimal.ZERO) ?.numberFormat8()
                    }.getOrDefault("0")

                    binding.balance.text = getString(R.string.available_balance, "$balance $tokenSymbol")
                } else {
                    binding.balance.text = getString(R.string.available_balance, "${tokenBalance.numberFormat8()} $tokenSymbol")
                }
                binding.insufficientFeeBalance.text = getString(R.string.insufficient_gas, value.token.symbol)
            }
            refreshFeeTokenExtra(value?.token?.assetId)
        }
    private var feeTokensExtra: TokensExtra? = null

    private fun refreshFeeTokenExtra(tokenId: String?) = lifecycleScope.launch {
        feeTokensExtra = if (tokenId == null) null
        else web3ViewModel.findTokensExtra(tokenId)
    }

    private var gas: BigDecimal? = null

    private suspend fun refreshFee(t: TokenItem) {
        val toAddress = toAddress?: return
        binding.loadingProgressBar.isVisible = true
        binding.contentTextView.isVisible = false
        val feeResponse = web3ViewModel.getFees(t.assetId, toAddress)
        if (feeResponse.isSuccess) {
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
            }
        }
        binding.contentTextView.isVisible = true
        binding.loadingProgressBar.isVisible = false
    }

    private suspend fun refreshGas(t: Web3Token) {
        val toAddress = toAddress?: return
        binding.loadingProgressBar.isVisible = true
        binding.contentTextView.isVisible = false
        if (t.fungibleId == chainToken?.fungibleId) {
            val fromAddress = fromAddress ?: return
            val transaction =
                try {
                    t.buildTransaction(fromAddress, toAddress, tokenBalance)
                } catch (e: Exception) {
                    Timber.Forest.w(e)
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    return
                }
            if (isAdded) {
                gas = web3ViewModel.calcFee(t, transaction, fromAddress)
                binding.contentTextView.text = "${gas?.numberFormat8()} ${chainToken?.symbol}"
                if (dialog.isShowing) {
                    dialog.dismiss()
                    v =
                        if (isReverse) {
                            BigDecimal(tokenBalance).subtract(gas).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
                        } else {
                            BigDecimal(tokenBalance).subtract(gas).toPlainString()
                        }
                    updateUI()
                }
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