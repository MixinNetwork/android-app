package one.mixin.android.web3

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
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.buildTransaction
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.receive.Web3ReceiveSelectionFragment
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.max

@AndroidEntryPoint
class InputFragment : BaseFragment(R.layout.fragment_input) {
    companion object {
        const val TAG = "InputFragment"
        const val ARGS_TO_ADDRESS = "args_to_address"
        const val ARGS_FROM_ADDRESS = "args_from_address"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"
        const val ARGS_ASSET = "args_asset"

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
                    putParcelable(ARGS_TOKEN, web3Token)
                    putParcelable(ARGS_CHAIN_TOKEN, chainToken)
                }
            }

        fun newInstance(
            tokenItem: TokenItem,
            toAddress: String,
        ) =
            InputFragment().apply {
                withArgs {
                    putParcelable(ARGS_ASSET, tokenItem)
                    putString(ARGS_TO_ADDRESS, toAddress)
                }
            }
    }

    private val binding by viewBinding(FragmentInputBinding::bind)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var isReverse: Boolean = false

    private val toAddress by lazy {
        requireNotNull(requireArguments().getString(ARGS_TO_ADDRESS))
    }
    private val fromAddress by lazy {
        requireArguments().getString(ARGS_FROM_ADDRESS)
    }
    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java)
    }
    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_CHAIN_TOKEN, Web3Token::class.java)
    }

    private val asset by lazy {
        requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)
    }

    private val currencyName by lazy {
        Fiats.getAccountCurrencyAppearance()
    }

    private val tokenPrice: BigDecimal by lazy {
        ((asset?.priceUsd ?: token?.price)?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(Fiats.getRate().toBigDecimal())
    }
    private val tokenSymbol by lazy {
        asset?.symbol ?: token!!.symbol
    }
    private val tokenIconUrl by lazy {
        asset?.iconUrl ?: token!!.iconUrl
    }
    private val tokenChainIconUrl by lazy {
        asset?.chainIconUrl ?: token!!.chainIconUrl
    }
    private val tokenBalance by lazy {
        asset?.balance ?: token!!.balance
    }
    private val tokenName by lazy {
        asset?.name ?: token!!.name
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
                titleView.setSubTitle(getString(if (asset != null) R.string.Receive else R.string.Send_transfer), toAddress.formatPublicKey())
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
                            context?.clickVibrate()
                        }
                    },
                )
                avatar.bg.loadImage(tokenIconUrl, R.drawable.ic_avatar_place_holder)
                avatar.badge.loadImage(tokenChainIconUrl, R.drawable.ic_avatar_place_holder)
                name.text = tokenName
                balance.text = "$tokenBalance $tokenSymbol"
                max.setOnClickListener {
                    maxClick()
                }
                keyboard.initPinKeys(
                    requireContext(),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<<"),
                    force = true,
                    white = true,
                )
                continueVa.setOnClickListener {
                    if (asset != null) { // to web3
                        val assetId = requireNotNull(asset?.assetId)
                        val amount =
                            if (isReverse) {
                                binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                            } else {
                                v
                            }
                        lifecycleScope.launch(
                            CoroutineExceptionHandler { _, error ->
                                ErrorHandler.handleError(error)
                                alertDialog.dismiss()
                            },
                        ) {
                            alertDialog.show()
                            val feeResponse = web3ViewModel.getFees(assetId, toAddress)
                            if (!feeResponse.isSuccess) {
                                toast(requireContext().getMixinErrorStringByCode(feeResponse.errorCode, feeResponse.errorDescription))
                                alertDialog.dismiss()
                                return@launch
                            }
                            val fee = feeResponse.data!!.first()
                            val feeTokensExtra = web3ViewModel.findTokensExtra(fee.assetId!!)
                            val feeItem = web3ViewModel.syncAsset(fee.assetId)
                            if (feeItem == null) {
                                toast(R.string.insufficient_balance)
                                alertDialog.dismiss()
                                return@launch
                            }
                            val totalAmount =
                                if (fee.assetId == assetId) {
                                    (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) + (fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                } else {
                                    fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                }
                            if (feeTokensExtra == null || (feeTokensExtra.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < totalAmount) {
                                toast(requireContext().getString(R.string.insufficient_gas, feeItem.symbol))
                                alertDialog.dismiss()
                                return@launch
                            }

                            alertDialog.dismiss()
                            val address = Address("", "address", assetId, toAddress, "Web3 Address", nowInUtc(), "0", fee.amount!!, null, null, fee.assetId)
                            val networkFee = NetworkFee(feeItem, fee.amount)
                            val withdrawBiometricItem = WithdrawBiometricItem(address, networkFee, null, UUID.randomUUID().toString(), asset, amount, null, PaymentStatus.pending.name, null)
                            TransferBottomSheetDialogFragment.newInstance(withdrawBiometricItem).apply {
                                setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                                    override fun onDismiss(success: Boolean) {
                                        if (success) {
                                            parentFragmentManager.apply {
                                                findFragmentByTag(Web3ReceiveSelectionFragment.TAG)?.let {
                                                    beginTransaction().remove(it).commit()
                                                }
                                                findFragmentByTag(TAG)?.let {
                                                    beginTransaction().remove(it).commit()
                                                }
                                            }
                                        }
                                    }
                                })

                            }.show(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                        }
                    } else { // from web3
                        val token = requireNotNull(token)
                        val fromAddress = requireNotNull(fromAddress)
                        val amount =
                            if (isReverse) {
                                binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                            } else {
                                v
                            }
                        lifecycleScope.launch(
                            CoroutineExceptionHandler { _, error ->
                                ErrorHandler.handleError(error)
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
                                    val txStateFragment = TransactionStateFragment.newInstance(serializedTx, null)
                                    navTo(txStateFragment, TransactionStateFragment.TAG)
                                },
                            )
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
            refreshGas()
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

    private var fee: BigDecimal? = null

    private fun maxClick() {
        if (token != null && token?.fungibleId == chainToken?.fungibleId) {
            if (fee == null) {
                if (!dialog.isShowing) {
                    lifecycleScope.launch {
                        dialog.show()
                        refreshGas()
                    }
                }
            } else {
                v =
                    if (isReverse) {
                        BigDecimal(tokenBalance).subtract(fee).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
                    } else {
                        BigDecimal(tokenBalance).subtract(fee).toPlainString()
                    }
            }
        } else if (asset != null && (asset?.assetId == ETHEREUM_CHAIN_ID || asset?.assetId == SOLANA_CHAIN_ID)) {
            if (fee == null) {
                if (!dialog.isShowing) {
                    lifecycleScope.launch {
                        dialog.show()
                        refreshGas()
                    }
                }
            } else {
                v =
                    if (isReverse) {
                        BigDecimal(tokenBalance).subtract(fee).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
                    } else {
                        BigDecimal(tokenBalance).subtract(fee).toPlainString()
                    }
            }
        } else {
            v =
                if (isReverse) {
                    BigDecimal(tokenBalance).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
                } else {
                    tokenBalance
                }
        }
        updateUI()
    }

    private suspend fun refreshGas() {
        if (asset != null) {
            refreshGas(asset!!)
        } else if (token != null) {
            refreshGas(token!!)
        }
    }

    private suspend fun refreshGas(t: TokenItem) {
        val feeResponse = web3ViewModel.getFees(t.assetId, toAddress)
        feeResponse.data?.firstOrNull()
        if (feeResponse.isSuccess) {
            feeResponse.data?.firstOrNull()?.let {
                fee = BigDecimal(it.amount)
            }
        }
    }

    private suspend fun refreshGas(t: Web3Token) {
        if (t.fungibleId == chainToken?.fungibleId) {
            val fromAddress = fromAddress ?: return
            val transaction =
                try {
                    t.buildTransaction(fromAddress, toAddress, tokenBalance)
                } catch (e: Exception) {
                    Timber.w(e)
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    return
                }
            if (isAdded) {
                fee = web3ViewModel.calcFee(t, transaction, fromAddress)
                if (dialog.isShowing) {
                    dialog.dismiss()
                    v =
                        if (isReverse) {
                            BigDecimal(tokenBalance).subtract(fee).multiply(tokenPrice).setScale(2, RoundingMode.DOWN).toPlainString()
                        } else {
                            BigDecimal(tokenBalance).subtract(fee).toPlainString()
                        }
                    updateUI()
                }
            }
        }
    }

    private val dialog by lazy {
        indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
            dismiss()
        }
    }
}
