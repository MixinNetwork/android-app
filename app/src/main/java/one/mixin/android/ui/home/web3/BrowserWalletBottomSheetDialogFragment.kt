package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.home.web3.error.JupiterErrorHandler
import one.mixin.android.ui.home.web3.error.ProgramErrorHandler
import one.mixin.android.ui.home.web3.error.RaydiumErrorHandler
import one.mixin.android.ui.home.web3.error.SolanaErrorHandler
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment.Step
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.reportException
import one.mixin.android.util.tickerFlow
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.throwIfAnyMaliciousInstruction
import org.sol4k.Base58
import org.sol4k.Constants.SIGNATURE_LENGTH
import org.sol4k.exception.RpcException
import org.sol4kt.SignInInput
import org.sol4kt.VersionedTransactionCompat
import org.web3j.crypto.Hash
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class BrowserWalletBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "BrowserWalletBottomSheetDialogFragment"

        const val ARGS_MESSAGE = "args_message"
        const val ARGS_URL = "args_url"
        const val ARGS_TITLE = "args_title"
        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"
        const val ARGS_TO_ADDRESS = "args_to_address"

        fun newInstance(
            jsSignMessage: JsSignMessage,
            url: String?,
            title: String?,
            amount: String? = null,
            token: Web3TokenItem? = null,
            chainToken: Web3TokenItem? = null,
            toAddress: String? = null,
        ) = BrowserWalletBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_MESSAGE, jsSignMessage)
            putString(
                ARGS_URL,
                url?.run {
                    Uri.parse(this).host
                },
            )
            putString(ARGS_TITLE, title)
            amount?.let { putString(ARGS_AMOUNT, it) }
            token?.let { putParcelable(ARGS_TOKEN, it) }
            chainToken?.let { putParcelable(ARGS_CHAIN_TOKEN, it) }
            toAddress?.let { putString(ARGS_TO_ADDRESS, it) }
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<BrowserWalletBottomSheetViewModel>()

    private val solanaErrorHandler = SolanaErrorHandler()

    private val signMessage: JsSignMessage by lazy { requireArguments().getParcelableCompat(ARGS_MESSAGE, JsSignMessage::class.java)!! }
    private val url: String? by lazy { requireArguments().getString(ARGS_URL) }
    private val title: String? by lazy { requireArguments().getString(ARGS_TITLE) }
    private val toAddress: String? by lazy { requireArguments().getString(ARGS_TO_ADDRESS) }
    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_CHAIN_TOKEN, Web3TokenItem::class.java)
    }
    private val currentChain by lazy {
        token?.getChainFromName() ?: JsSigner.currentChain
    }

    var step by mutableStateOf(Step.Input)
        private set
    private var amount: String? by mutableStateOf(null)
    private var token: Web3TokenItem? by mutableStateOf(null)
    private var errorInfo: String? by mutableStateOf(null)
    private var tipGas: TipGas? by mutableStateOf(null)
    private var asset: Token? by mutableStateOf(null)
    private var insufficientGas by mutableStateOf(false)
    private var solanaTx: VersionedTransactionCompat? by mutableStateOf(null)
    private var parsedTx: ParsedTx? by mutableStateOf(null)
    private var solanaSignInInput: SignInInput? by mutableStateOf(null)

    @Inject
    lateinit var rpc: Rpc

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            token = requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java)
            amount = requireArguments().getString(ARGS_AMOUNT)
            setContent {
                BrowserPage(
                    JsSigner.address,
                    currentChain,
                    amount,
                    token,
                    toAddress,
                    signMessage.type,
                    step,
                    signMessage.isCancelTx,
                    signMessage.isSpeedUp,
                    tipGas,
                    solanaTx?.calcFee(JsSigner.address),
                    parsedTx,
                    signMessage.solanaTxSource,
                    asset,
                    signMessage.wcEthereumTransaction,
                    solanaSignInInput?.toMessage() ?: signMessage.reviewData,
                    url,
                    title,
                    errorInfo,
                    insufficientGas,
                    onPreviewMessage = { TextPreviewActivity.show(requireContext(), it) },
                    showPin = { showPin() },
                    onDismissRequest = { dismiss() },
                    onRejectAction = {
                        onRejectAction?.invoke()
                        dismiss()
                    },
                )
            }

            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                val ctx = requireContext()
                behavior?.peekHeight = ctx.realSize().y - ctx.statusBarHeight() - ctx.navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }

            refreshEstimatedGasAndAsset(currentChain)
        }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is WalletConnectActivity || activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private fun refreshEstimatedGasAndAsset(chain: Chain) {
        if (chain == Chain.Solana) {
            refreshSolana()
            return
        }
        val assetId = chain.getWeb3ChainId()
        val transaction = signMessage.wcEthereumTransaction ?: return
        tickerFlow(15.seconds)
            .onEach {
                asset = viewModel.refreshAsset(assetId)
                try {
                    tipGas = withContext(Dispatchers.IO) {
                        val r = viewModel.estimateFee(
                            EstimateFeeRequest(
                                assetId,
                                transaction.data,
                                transaction.from,
                                transaction.to,
                            )
                        )
                        if (r.isSuccess.not()) {
                            step = Step.Error
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            return@withContext null
                        }
                        buildTipGas(chain.chainId, r.data!!)
                    } ?: return@onEach
                    insufficientGas = checkGas(token, chainToken, tipGas, transaction.value, transaction.maxFeePerGas)
                    if (insufficientGas) {
                        handleException(IllegalArgumentException(requireContext().getString(R.string.insufficient_gas, chainToken?.symbol ?: currentChain.symbol)))
                    }
                    val hex = JsSigner.ethPreviewTransaction(
                        JsSigner.evmAddress,
                        transaction,
                        tipGas!!,
                        chain = token?.getChainFromName()
                    ) { _ ->
                        val nonce = rpc.nonceAt(currentChain.assetId, JsSigner.evmAddress) ?: throw IllegalArgumentException("failed to get nonce")
                        return@ethPreviewTransaction nonce
                    }
                    if (parsedTx == null) {
                        parsedTx = viewModel.simulateWeb3Tx(hex, assetId, from = JsSigner.evmAddress, toAddress)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun refreshSolana() {
        tickerFlow(15.seconds)
            .onEach {
                try {
                    if (signMessage.type == JsSignMessage.TYPE_RAW_TRANSACTION) {
                        val tx =
                            solanaTx ?: VersionedTransactionCompat.from(signMessage.data ?: "").apply {
                                val tx = if (this.onlyOneSigner()) {
                                    updateTxPriorityFee(this, signMessage.solanaTxSource)
                                } else {
                                    this
                                }
                                solanaTx = tx
                            }
                        if (parsedTx == null) {
                            parsedTx = viewModel.simulateWeb3Tx(tx.serialize().base64Encode(), Constants.ChainId.Solana, JsSigner.solanaAddress, toAddress)
                        }
                        tx.throwIfAnyMaliciousInstruction()
                    } else if (signMessage.type == JsSignMessage.TYPE_SIGN_IN) {
                        solanaSignInInput = SignInInput.from(signMessage.data ?: "", JsSigner.address)
                    }
                } catch (e: Exception) {
                    handleException(e)
                }
                asset = viewModel.refreshAsset(Chain.Solana.assetId)
            }.launchIn(lifecycleScope)
    }

    private fun doAfterPinComplete(pin: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                step = Step.Loading
                errorInfo = null
                if (signMessage.type == JsSignMessage.TYPE_TRANSACTION) {
                    val transaction = requireNotNull(signMessage.wcEthereumTransaction)
                    val priv = viewModel.getWeb3Priv(requireContext(), pin, JsSigner.currentChain.assetId)
                    val pair = JsSigner.ethSignTransaction(priv, transaction, tipGas!!, chain = token?.getChainFromName()) { address ->
                        val nonce = rpc.nonceAt(currentChain.assetId, address) ?: throw IllegalArgumentException("failed to get nonce")
                        return@ethSignTransaction nonce
                    }
                    step = Step.Sending
                    val hex = pair.first
                    val hash = Hash.sha3(hex)
                    viewModel.postRawTx(hex, currentChain.getWeb3ChainId(), pair.second, toAddress, token?.assetId)
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$hash\");")
                } else if (signMessage.type == JsSignMessage.TYPE_RAW_TRANSACTION) {
                    val priv = viewModel.getWeb3Priv(requireContext(), pin, JsSigner.currentChain.assetId)
                    val tx = JsSigner.signSolanaTransaction(priv, requireNotNull(solanaTx) { "required solana tx can not be null" }) {
                        val blockhash = rpc.getLatestBlockhash() ?: throw IllegalArgumentException("failed to get blockhash")
                        return@signSolanaTransaction blockhash
                    }
                    step = Step.Sending
                    val sig = tx.signatures.first { s -> s != Base58.encode(ByteArray(SIGNATURE_LENGTH)) }
                    val rawTx = tx.serialize().base64Encode()
                    if (tx.onlyOneSigner()) {
                        viewModel.postRawTx(rawTx, Constants.ChainId.Solana, JsSigner.solanaAddress,  toAddress,token?.assetId)
                        onTxhash?.invoke(sig, rawTx)
                    }
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$sig\");")
                } else if (signMessage.type == JsSignMessage.TYPE_TYPED_MESSAGE || signMessage.type == JsSignMessage.TYPE_MESSAGE || signMessage.type == JsSignMessage.TYPE_PERSONAL_MESSAGE) {
                    val priv = viewModel.getWeb3Priv(requireContext(), pin, JsSigner.currentChain.assetId)
                    val hex = JsSigner.signMessage(priv, requireNotNull(signMessage.data), signMessage.type)
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$hex\");")
                } else if (signMessage.type == JsSignMessage.TYPE_SIGN_IN) {
                    val priv = viewModel.getWeb3Priv(requireContext(), pin, JsSigner.currentChain.assetId)
                    val output = JsSigner.solanaSignIn(priv, solanaSignInInput!!)
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$output\");")
                } else {
                    throw IllegalArgumentException("invalid signMessage type ${signMessage.type}")
                }
                step = Step.Done
            } catch (e: Exception) {
                onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, null);")
                handleException(e)
            }
        }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissAction?.invoke(step == Step.Done)
    }
    private suspend fun updateTxPriorityFee(tx: VersionedTransactionCompat, solanaTxSource: SolanaTxSource): VersionedTransactionCompat {
        val priorityFeeResp = viewModel.getPriorityFee(tx.serialize().base64Encode())
        if (priorityFeeResp != null && priorityFeeResp.unitPrice != null && priorityFeeResp.unitLimit != null) {
            tx.setPriorityFee(priorityFeeResp.unitPrice, priorityFeeResp.unitLimit)
        }
        return tx
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
                    Convert.fromWei(Numeric.decodeQuantity(value ?: "0x0").toBigDecimal(), Convert.Unit.ETHER) + maxGas > BigDecimal(chainToken.balance)
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

    private fun handleException(e: Throwable) {
        Timber.e(e)
        val msg =
            if (e is RpcException) {
                solanaErrorHandler.reset()
                    .addHandler(JupiterErrorHandler(e.rawResponse))
                    .addHandler(RaydiumErrorHandler(e.rawResponse))
                    .addHandler(ProgramErrorHandler(e.rawResponse))
                    .start(requireContext())
            } else {
                null
            }
        errorInfo = msg ?: e.message
        reportException("$TAG handleException", e)
        step = Step.Error
    }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismiss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }

    fun setOnDone(callback: (String?) -> Unit): BrowserWalletBottomSheetDialogFragment {
        onDone = callback
        return this
    }

    fun setOnReject(callback: () -> Unit): BrowserWalletBottomSheetDialogFragment {
        onRejectAction = callback
        return this
    }

    fun setOnDismiss(callback: (Boolean) -> Unit): BrowserWalletBottomSheetDialogFragment {
        onDismissAction = callback
        return this
    }

    fun setOnTxhash(callback: (String, String) -> Unit): BrowserWalletBottomSheetDialogFragment {
        onTxhash = callback
        return this
    }

    private var onDone: ((String?) -> Unit)? = null
    private var onRejectAction: (() -> Unit)? = null
    private var onDismissAction: ((Boolean) -> Unit)? = null
    private var onTxhash: ((String, String) -> Unit)? = null

    fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1).setOnPinComplete { pin ->
            lifecycleScope.launch(
                CoroutineExceptionHandler { _, error ->
                    handleException(error)
                },
            ) {
                doAfterPinComplete(pin)
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun deposit() {
        dismiss()
    }
}

fun showBrowserBottomSheetDialogFragment(
    fragmentActivity: FragmentActivity,
    signMessage: JsSignMessage,
    amount: String? = null,
    token: Web3TokenItem? = null,
    chainToken: Web3TokenItem? = null,
    toAddress: String? = null,
    currentUrl: String? = null,
    currentTitle: String? = null,
    onReject: (() -> Unit)? = null,
    onDone: ((String?) -> Unit)? = null,
    onDismiss: ((Boolean) -> Unit)? = null,
    onTxhash: ((String, String) -> Unit)? = null,
) {
    val wcBottomSheet = BrowserWalletBottomSheetDialogFragment.newInstance(signMessage, currentUrl, currentTitle, amount, token, chainToken, toAddress)
    onDismiss?.let {
        wcBottomSheet.setOnDismiss(onDismiss)
    }
    onDone?.let {
        wcBottomSheet.setOnDone(onDone)
    }
    onReject?.let {
        wcBottomSheet.setOnReject(onReject)
    }
    onTxhash?.let {
        wcBottomSheet.setOnTxhash(onTxhash)
    }
    wcBottomSheet.showNow(
        fragmentActivity.supportFragmentManager,
        BrowserWalletBottomSheetDialogFragment.TAG,
    )
}
