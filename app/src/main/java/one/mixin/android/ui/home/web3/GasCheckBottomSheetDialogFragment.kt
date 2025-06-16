package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
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
import one.mixin.android.databinding.FragmentBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenFeeItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferBalanceErrorBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferWeb3BalanceErrorBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import kotlin.getValue

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
    }

    private val binding by viewBinding(FragmentBottomSheetBinding::inflate)

    private lateinit var contentView: View

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        dialog.setContentView(contentView)
        val behavior = ((contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, requireContext().dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
        binding.linkLoadingInfo.text = ""
        lifecycleScope.launch {
            refreshEstimatedGasAndAsset(currentChain)
        }
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
        fragment.showNow(parentFragmentManager, BrowserWalletBottomSheetDialogFragment.TAG)
        dismissAllowingStateLoss()
    }

    private suspend fun refreshEstimatedGasAndAsset(chain: Chain) {
        if (chain == Chain.Solana) {
            // Todo
            dismiss()
            showBrowserWalletBottomSheet()
            return
        }
        val assetId = chain.getWeb3ChainId()
        val transaction = signMessage.wcEthereumTransaction
        if (transaction == null) {
            Timber.e("Transaction is null")
            dismiss()
            showBrowserWalletBottomSheet()
            return
        }
        viewModel.refreshAsset(assetId)
        try {
            val tipGas = withContext(Dispatchers.IO) {
                val r = viewModel.estimateFee(
                    EstimateFeeRequest(
                        assetId,
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
                dismiss()
                showBrowserWalletBottomSheet()
                return
            }
            val insufficientGas = checkGas(token, chainToken, tipGas, transaction.value, transaction.maxFeePerGas)
            if (insufficientGas) {
                if (chainToken == null) {
                    Timber.e("Insufficient gas for chain: ${chain.chainId}")
                    dismiss()
                    showBrowserWalletBottomSheet()
                    return
                } else {
                    val fee = tipGas.displayValue(transaction.maxFeePerGas) ?: BigDecimal.ZERO
                    val amount = transaction.getMainTokenAmount()
                    TransferWeb3BalanceErrorBottomSheetDialogFragment.newInstance(Web3TokenFeeItem(chainToken!!, amount, fee)).showNow(parentFragmentManager, TransferWeb3BalanceErrorBottomSheetDialogFragment.TAG)
                    dismiss()
                }
            } else {
                dismiss()
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

    private fun checkGas(
        web3Token: Web3TokenItem?,
        chainToken: Web3TokenItem?,
        tipGas: TipGas?,
        value: String?,
        maxFeePerGas: String?
    ): Boolean {
        return if (web3Token != null) {
            if (chainToken == null) {
                true
            } else if (tipGas != null) {
                val maxGas = tipGas.displayValue(maxFeePerGas) ?: BigDecimal.ZERO
                if (web3Token.assetId == chainToken.assetId && web3Token.chainId == chainToken.chainId) {
                    Convert.fromWei(
                        Numeric.decodeQuantity(value ?: "0x0").toBigDecimal(),
                        Convert.Unit.ETHER
                    ) + maxGas > BigDecimal(chainToken.balance)
                } else {
                    maxGas > BigDecimal(chainToken.balance)
                }
            } else {
                false
            }
        } else {
            false
        }
    }

    var onReject: (() -> Unit)? = null
    var onDone: ((String?) -> Unit)? = null
    var onDismiss: ((Boolean) -> Unit)? = null
    var onTxhash: ((String, String) -> Unit)? = null

    fun setOnDone(callback: (String?) -> Unit): GasCheckBottomSheetDialogFragment {
        onDone = callback
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
            ) {}
        }

}



