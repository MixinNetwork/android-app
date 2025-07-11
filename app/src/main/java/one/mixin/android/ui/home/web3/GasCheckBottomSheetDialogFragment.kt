package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.FragmentBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenFeeItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.wallet.SwapTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferWeb3BalanceErrorBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class GasCheckBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "GasCheckBottomSheetDialogFragment"
        private const val ARGS_MESSAGE = "args_message"
        private const val ARGS_URL = "args_url"
        private const val ARGS_TITLE = "args_title"
        private const val ARGS_AMOUNT = "args_amount"
        private const val ARGS_TOKEN = "args_token"
        private const val ARGS_CHAIN_TOKEN = "args_chain_token"
        private const val ARGS_TO_ADDRESS = "args_to_address"

        fun newInstance(
            jsSignMessage: JsSignMessage,
            url: String?,
            title: String?,
            amount: String? = null,
            token: Web3TokenItem? = null,
            chainToken: Web3TokenItem? = null,
            toAddress: String? = null,
        ) = GasCheckBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_MESSAGE, jsSignMessage)
                putString(ARGS_URL, url)
                putString(ARGS_TITLE, title)
                amount?.let { putString(ARGS_AMOUNT, it) }
                token?.let { putParcelable(ARGS_TOKEN, it) }
                chainToken?.let { putParcelable(ARGS_CHAIN_TOKEN, it) }
                toAddress?.let { putString(ARGS_TO_ADDRESS, it) }
            }
        }

        private const val ARGS_SWAP_RESULT = "args_result"
        private const val ARGS_FROM = "args_from"
        private const val ARGS_TO = "args_to"

        fun newInstance(swapResult: SwapResponse, inAsset: SwapToken, outAssetItem: SwapToken) =
            GasCheckBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_SWAP_RESULT, swapResult)
                    putParcelable(ARGS_FROM, inAsset)
                    putParcelable(ARGS_TO, outAssetItem)
                }
            }
    }

    private val binding by viewBinding(FragmentBottomSheetBinding::inflate)

    private lateinit var contentView: View

    @Inject
    lateinit var rpc: Rpc

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        dialog.setContentView(contentView)
        val behavior =
            ((contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                requireContext().dpToPx(300f)
            )
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
        binding.linkLoadingInfo.text = ""
        lifecycleScope.launch {
            if (swapResult != null) {
                // Todo: Handle swap result
                val web3TokenItem = viewModel.web3TokenItemById("", fromToken.assetId)
                val chainTokenItem = viewModel.web3TokenItemById("", fromToken.chain.chainId)
                if (web3TokenItem != null) {
                    val jsSignMessage = web3TokenItem.buildTransaction(
                        rpc, JsSigner.evmAddress,
                        swapResult!!.depositDestination!!,
                        swapResult!!.quote.inAmount
                    )
                    val transaction = jsSignMessage.wcEthereumTransaction
                    val chain = web3TokenItem.getChainFromName()
                    refreshEstimatedGasAndAsset(transaction, chain, web3TokenItem, chainTokenItem)
                } else {
                    showError("Token not found")
                }
            } else {
                refreshEstimatedGasAndAsset(signMessage.wcEthereumTransaction, currentChain, token, chainToken)
            }
        }
    }

    private val swapResult: SwapResponse? by lazy {
        requireArguments().getParcelableCompat(
            ARGS_SWAP_RESULT,
            SwapResponse::class.java
        )
    }

    private val fromToken: SwapToken by lazy {
        requireArguments().getParcelableCompat(
            ARGS_FROM,
            SwapToken::class.java
        )!!
    }

    private val toToken: SwapToken by lazy {
        requireArguments().getParcelableCompat(
            ARGS_TO,
            SwapToken::class.java
        )!!
    }

    private val signMessage: JsSignMessage by lazy {
        requireArguments().getParcelableCompat(
            ARGS_MESSAGE,
            JsSignMessage::class.java
        )!!
    }
    private val url: String? by lazy { arguments?.getString(ARGS_URL) }
    private val title: String? by lazy { arguments?.getString(ARGS_TITLE) }
    private val amount: String? by lazy { arguments?.getString(ARGS_AMOUNT) }
    private val token: Web3TokenItem? by lazy {
        requireArguments().getParcelableCompat(
            ARGS_TOKEN,
            Web3TokenItem::class.java
        )
    }
    private val chainToken: Web3TokenItem? by lazy {
        requireArguments().getParcelableCompat(
            ARGS_CHAIN_TOKEN,
            Web3TokenItem::class.java
        )
    }
    private val toAddress: String? by lazy { requireArguments().getString(ARGS_TO_ADDRESS) }
    private val currentChain by lazy {
        token?.getChainFromName() ?: JsSigner.currentChain
    }

    private val viewModel by viewModels<BrowserWalletBottomSheetViewModel>()

    private fun showBrowserWalletBottomSheet() {
        if (!isAdded) return
        if (swapResult != null) {
            SwapTransferBottomSheetDialogFragment.newInstance(swapResult!!, fromToken, toToken)
                .apply {
                    onDone?.let {
                        setOnDone(it)
                    }
                    onDestroy?.let {
                        setOnDestroy(it)
                    }
                }
                .show(requireActivity().supportFragmentManager, SwapTransferBottomSheetDialogFragment.TAG)
        } else {
            val fragment = BrowserWalletBottomSheetDialogFragment.newInstance(
                signMessage,
                url,
                title,
                amount,
                token,
                chainToken,
                toAddress
            )
            onDismiss?.let { it ->
                fragment.setOnDismiss(it)
            }
            onDone?.let { it ->
                fragment.setOnDone(it)
            }
            onReject?.let { it ->
                fragment.setOnReject(it)
            }
            onTxhash?.let { it ->
                fragment.setOnTxhash(it)
            }
            fragment.show(requireActivity().supportFragmentManager, BrowserWalletBottomSheetDialogFragment.TAG)
        }
        dismissAllowingStateLoss()
    }

    private suspend fun refreshEstimatedGasAndAsset(
        transaction: WCEthereumTransaction?,
        chain: Chain,
        token: Web3TokenItem?,
        chainToken: Web3TokenItem?,
    ) {
        if (chain == Chain.Solana) {
            showBrowserWalletBottomSheet()
            return
        }
        val chainId = chain.getWeb3ChainId()
        if (transaction == null) {
            Timber.e("Transaction is null")
            showBrowserWalletBottomSheet()
            return
        }
        if (token == null) {
            Timber.e("token is null")
            showBrowserWalletBottomSheet()
            return
        }
        viewModel.refreshAsset(chainId)
        try {
            val tipGas = withContext(Dispatchers.IO) {
                val r = viewModel.estimateFee(
                    EstimateFeeRequest(
                        chainId,
                        transaction.data,
                        transaction.from,
                        transaction.to,
                    )
                )
                if (r.isSuccess.not()) {
                    ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                    return@withContext null
                }
                buildTipGas(chain.chainId, r.data!!)
            }
            if (tipGas == null) {
                Timber.e("Failed to estimate gas for chain: ${chain.chainId}")
                showBrowserWalletBottomSheet()
                return
            }
            val insufficientGas =
                checkGas(token, chainId = chainId, tipGas, transaction.value, transaction.maxFeePerGas)
            if (insufficientGas) {
                val c = chainToken ?: viewModel.web3TokenItemById(token.walletId, chainId)
                if (c == null) {
                    Timber.e("Insufficient gas for chain: ${chain.chainId}")
                    showBrowserWalletBottomSheet()
                    return
                } else {
                    val fee = tipGas.displayValue(transaction.maxFeePerGas) ?: BigDecimal.ZERO
                    val amount = transaction.getMainTokenAmount()
                    TransferWeb3BalanceErrorBottomSheetDialogFragment.newInstance(
                        Web3TokenFeeItem(
                            c,
                            amount,
                            fee
                        )
                    ).showNow(
                        parentFragmentManager,
                        TransferWeb3BalanceErrorBottomSheetDialogFragment.TAG
                    )
                    dismiss()
                }
            } else {
                showBrowserWalletBottomSheet()
            }
        } catch (e: Exception) {
            showError(ErrorHandler.getErrorMessage(e))
        }
    }

    @SuppressLint("SetTextI18n")
    fun showError(
        @StringRes errorRes: Int = R.string.Invalid_Link,
    ) {
        if (!isAdded) return

        binding.apply {
            if (errorRes == R.string.Invalid_Link) {
                linkErrorInfo.text = "${getString(R.string.Invalid_Link)}\n\n$url"
            } else {
                linkErrorInfo.setText(errorRes)
            }
            linkLoading.visibility = GONE
            linkErrorInfo.visibility = VISIBLE
            linkErrorInfo.setTextIsSelectable(true)
        }
    }

    fun showError(error: String) {
        if (!isAdded) return

        binding.apply {
            linkErrorInfo.text = error
            linkLoading.visibility = GONE
            linkErrorInfo.visibility = VISIBLE
        }
    }

    private suspend fun checkGas(
        web3Token: Web3TokenItem?,
        chainId: String,
        tipGas: TipGas?,
        value: String?,
        maxFeePerGas: String?
    ): Boolean {
        val assetId = web3Token?.assetId
        val walletId = web3Token?.walletId ?: return true
        val c = viewModel.web3TokenItemById(walletId, chainId) ?: return true
        return if (tipGas != null) {
            val maxGas = tipGas.displayValue(maxFeePerGas) ?: BigDecimal.ZERO
            if (assetId == c.assetId && assetId == c.chainId) {
                Convert.fromWei(
                    Numeric.decodeQuantity(value ?: "0x0").toBigDecimal(),
                    Convert.Unit.ETHER
                ) + maxGas > BigDecimal(c.balance)
            } else {
                maxGas > BigDecimal(c.balance)
            }
        } else {
            false
        }
    }

    var onReject: (() -> Unit)? = null
    var onDestroy: (() -> Unit)? = null
    var onDone: ((String?) -> Unit)? = null
    var onDismiss: ((Boolean) -> Unit)? = null
    var onTxhash: ((String, String) -> Unit)? = null

    fun setOnDone(callback: (String?) -> Unit): GasCheckBottomSheetDialogFragment {
        onDone = callback
        return this
    }

    fun setOnDestroy(callback: () -> Unit): GasCheckBottomSheetDialogFragment {
        onDestroy = callback
        return this
    }

    fun setOnReject(callback: () -> Unit): GasCheckBottomSheetDialogFragment {
        onReject = callback
        return this
    }

    fun setOnDismiss(callback: (Boolean) -> Unit): GasCheckBottomSheetDialogFragment {
        onDismiss = callback
        return this
    }

    fun setOnTxhash(callback: (String, String) -> Unit): GasCheckBottomSheetDialogFragment {
        onTxhash = callback
        return this
    }

    fun close() {
        dismissAllowingStateLoss()
    }

    private val mBottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    try {
                        dismissAllowingStateLoss()
                    } catch (e: IllegalStateException) {
                        Timber.w(e)
                    }
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }
}