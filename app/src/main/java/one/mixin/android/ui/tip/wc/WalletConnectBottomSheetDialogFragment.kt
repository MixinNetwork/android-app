package one.mixin.android.ui.tip.wc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.GsonBuilder
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.wc.WCChangeEvent
import one.mixin.android.tip.wc.WCError
import one.mixin.android.tip.wc.WCErrorEvent
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnect.RequestType
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.WalletConnectV2.getProposalChainIds
import one.mixin.android.tip.wc.WalletConnectV2.getNamespaceProposal
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.Method
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WcBitcoinSendTransfer
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.tip.wc.internal.WalletConnectException
import one.mixin.android.tip.wc.internal.WcSolanaTransaction
import one.mixin.android.tip.wc.internal.formatProposalAccountText
import one.mixin.android.tip.wc.internal.getChain
import one.mixin.android.tip.wc.internal.getChainByChainId
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.home.web3.error.JupiterErrorHandler
import one.mixin.android.ui.home.web3.error.ProgramErrorHandler
import one.mixin.android.ui.home.web3.error.RaydiumErrorHandler
import one.mixin.android.ui.home.web3.error.SolanaErrorHandler
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.tip.wc.sessionproposal.SessionProposalPage
import one.mixin.android.ui.tip.wc.sessionrequest.SessionRequestPage
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.CrossWalletFeeFreeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferWeb3BalanceErrorBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.reportException
import one.mixin.android.util.tickerFlow
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.Web3Signer
import org.sol4k.exception.RpcException
import org.sol4kt.VersionedTransactionCompat
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class WalletConnectBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletConnectBottomSheetDialogFragment"

        const val ARGS_REQUEST_TYPE = "args_request_type"
        const val ARGS_VERSION = "args_version"
        const val ARGS_TOPIC = "args_topic"

        fun newInstance(
            requestType: RequestType,
            version: WalletConnect.Version,
            topic: String? = null,
        ) = WalletConnectBottomSheetDialogFragment().withArgs {
            putInt(ARGS_REQUEST_TYPE, requestType.ordinal)
            putInt(ARGS_VERSION, version.ordinal)
            topic?.let { putString(ARGS_TOPIC, it) }
        }
    }

    enum class Step {
        Connecting,
        Sign,
        Input,
        Loading,
        Sending,
        Done,
        Error,
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<WalletConnectBottomSheetViewModel>()

    private val solanaErrorHandler = SolanaErrorHandler()

    private var processCompleted = false

    private val requestType by lazy { RequestType.entries[requireArguments().getInt(ARGS_REQUEST_TYPE)] }
    private val version by lazy { WalletConnect.Version.entries[requireArguments().getInt(ARGS_VERSION)] }
    private val topic: String by lazy { requireArguments().getString(ARGS_TOPIC) ?: "" }

    var step by mutableStateOf(Step.Input)
        private set
    private var chain: Chain by mutableStateOf(Chain.Ethereum)
    private var errorInfo: String? by mutableStateOf(null)
    private var tipGas: TipGas? by mutableStateOf(null)
    private var asset: Token? by mutableStateOf(null)
    private var signData: WalletConnect.WCSignData.V2SignData<*>? by mutableStateOf(null)
    private var sessionProposal: Wallet.Model.SessionProposal? by mutableStateOf(null)
    private var sessionRequest: Wallet.Model.SessionRequest? by mutableStateOf(null)
    private var account: String by mutableStateOf("")
    private var signedTransactionData: Any? = null
    private var estimateGasJob: Job? = null
    private var preflightMessage: JsSignMessage? = null

    @Inject
    lateinit var rpc: Rpc

    @Composable
    override fun ComposeContent() {
        when (requestType) {
            RequestType.Connect -> {
                Loading()
            }

            RequestType.SessionProposal -> {
                SessionProposalPage(
                    version,
                    account,
                    step,
                    chain,
                    topic,
                    sessionProposal,
                    errorInfo,
                    onDismissRequest = { dismiss() },
                    showPin = { showPin() },
                )
            }

            RequestType.SessionRequest -> {
                val gson =
                    GsonBuilder()
                        .serializeNulls()
                        .setPrettyPrinting()
                        .create()
                SessionRequestPage(
                    gson,
                    version,
                    account,
                    step,
                    chain,
                    topic,
                    sessionRequest,
                    signData,
                    asset,
                    tipGas,
                    errorInfo,
                    onFreeClick = {
                        CrossWalletFeeFreeBottomSheetDialogFragment
                            .newInstance()
                            .show(parentFragmentManager, CrossWalletFeeFreeBottomSheetDialogFragment.TAG)
                    },
                    onPreviewMessage = { TextPreviewActivity.show(requireContext(), it) },
                    onDismissRequest = { dismiss() },
                    showPin = { showPin() },
                )
            }
            RequestType.Pay -> {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        step =
            when (requestType) {
                RequestType.Connect -> Step.Connecting
                RequestType.SessionProposal -> Step.Input
                RequestType.SessionRequest -> Step.Sign
                RequestType.Pay -> Step.Done
            }
        checkV2ChainAndParseSignData()
        if (requestType == RequestType.Connect) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val error = WalletConnectV2.pairingErrors.await()
                    if (step == Step.Connecting && dialog?.isShowing == true) {
                        RxBus.publish(
                            WCErrorEvent(
                                WCError(error ?: IllegalStateException(getString(R.string.error_connection_timeout))),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        if (requestType == RequestType.Connect) {
            return 200.dp
        }
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
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

    override fun onDismiss(dialog: DialogInterface) {
        stopEstimatedGasRefresh("dismiss")
        if (!processCompleted) {
            Timber.d("$TAG dismiss onReject")
            if (onRejectAction != null) {
                onRejectAction?.invoke()
            } else {
                reject()
            }
        }
        super.onDismiss(dialog)
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

    private fun checkV2ChainAndParseSignData() =
        viewLifecycleOwner.lifecycleScope.launch {
            val topic = this@WalletConnectBottomSheetDialogFragment.topic

            when (requestType) {
                RequestType.SessionProposal -> {
                    sessionProposal =
                        viewModel.getV2SessionProposal(topic)?.apply {
                            this.getNamespaceProposal()?.chains?.firstOrNull { c ->
                                c.getChain() != null
                            }?.getChain()?.let { chain = it }
                        }
                }
                RequestType.SessionRequest -> {
                    sessionRequest =
                        viewModel.getV2SessionRequest(topic)?.apply {
                            getChainByChainId(chainId)?.let { chain = it }
                        }
                }
                else -> {}
            }

            account =
                if (requestType == RequestType.SessionProposal) {
                    sessionProposal?.let { proposalAccountText(it) } ?: accountFor(chain)
                } else {
                    accountFor(chain)
                }

            if (requestType != RequestType.SessionRequest) return@launch
            val sessionRequest = this@WalletConnectBottomSheetDialogFragment.sessionRequest ?: return@launch

            var signData = this@WalletConnectBottomSheetDialogFragment.signData
            if (signData == null) {
                signData =
                    try {
                        viewModel.parseV2SignData(account, sessionRequest)
                    } catch (e: Exception) {
                        toast(e.message ?: "Unknown error")
                        null
                    }
            }

            // not supported sessionRequest, like eth_call
            if (signData == null) {
                dismiss()
                return@launch
            }

            this@WalletConnectBottomSheetDialogFragment.signData = signData

            val message = signData.signMessage
            preflightMessage = when (message) {
                is WCEthereumTransaction -> JsSignMessage(signData.requestId, JsSignMessage.TYPE_TRANSACTION, wcEthereumTransaction = message)
                is VersionedTransactionCompat -> JsSignMessage(
                    signData.requestId,
                    JsSignMessage.TYPE_RAW_TRANSACTION,
                    data = GsonHelper.customGson.fromJson(sessionRequest.request.params, WcSolanaTransaction::class.java).transaction,
                    solanaTxSource = SolanaTxSource.WalletConnect,
                )
                else -> null
            }
            if (preflightMessage != null) {
                step = Step.Loading
                try {
                    if (!preflightTransaction()) return@launch
                    asset = viewModel.refreshAsset(chain.getWeb3ChainId())
                    step = Step.Sign
                    if (message is WCEthereumTransaction) refreshEstimatedGasAndAsset(chain)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleException(e)
                }
            }
        }

    private suspend fun preflightTransaction(cachedTipGas: TipGas? = null): Boolean {
        val message = preflightMessage ?: return true
        val result = viewModel.preflightTransaction(message, chain, account, cachedTipGas) ?: return true
        tipGas = result.tipGas
        signData?.tipGas = result.tipGas
        if (result.insufficientBalance) {
            stopEstimatedGasRefresh("insufficient_balance")
            TransferWeb3BalanceErrorBottomSheetDialogFragment.newInstance(result.balance)
                .showNow(parentFragmentManager, TransferWeb3BalanceErrorBottomSheetDialogFragment.TAG)
            dismiss()
            return false
        }
        return true
    }

    private fun stopEstimatedGasRefresh(reason: String) {
        estimateGasJob?.let {
            Timber.d("$TAG estimateGas stop topic=$topic requestId=${sessionRequest?.request?.id} step=$step reason=$reason")
            it.cancel()
            estimateGasJob = null
        }
    }

    private fun refreshEstimatedGasAndAsset(chain: Chain) {
        stopEstimatedGasRefresh("restart")
        var cachedTipGas = tipGas
        estimateGasJob = tickerFlow(15.seconds)
            .onEach {
                if (processCompleted || step == Step.Done || step == Step.Sending) {
                    stopEstimatedGasRefresh("step_$step")
                    return@onEach
                }
                try {
                    val initialTipGas = cachedTipGas
                    cachedTipGas = null
                    if (!preflightTransaction(initialTipGas)) return@onEach
                    asset = viewModel.refreshAsset(chain.getWeb3ChainId())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    tipGas = null
                    signData?.tipGas = null
                    handleException(e)
                    stopEstimatedGasRefresh("error")
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun doAfterPinComplete(pin: String) =
        lifecycleScope.launch {
            stopEstimatedGasRefresh("confirm")
            step = Step.Loading
            try {
                if (!preflightTransaction(tipGas)) return@launch
                val error =
                    withContext(Dispatchers.IO) {
                        if (onPinCompleteAction != null) {
                            onPinCompleteAction?.invoke(pin)
                        } else {
                            if (version == WalletConnect.Version.V2 && requestType == RequestType.SessionProposal) {
                                viewModel.verifyPin(requireContext(), pin)
                                approveWithPriv(ByteArray(0))
                            } else {
                                val privateKey = viewModel.getWeb3Priv(requireContext(), pin, chain.assetId)
                                approveWithPriv(privateKey)
                            }
                        }
                    }
                if (error == null) {
                    step =
                        if (isSendEvmTransaction() || isSignSolanaTransaction() || isSendBitcoinTransfer()) {
                            try {
                                step = Step.Sending
                                val sendError =
                                    withContext(Dispatchers.IO) {
                                        val sessionRequest = this@WalletConnectBottomSheetDialogFragment.sessionRequest ?: return@withContext "sessionRequest is null"
                                        val signedTransactionData = this@WalletConnectBottomSheetDialogFragment.signedTransactionData ?: return@withContext "signedTransactionData is null"
                                        Timber.d("$TAG sendTransaction start topic=$topic requestId=${sessionRequest.request.id} step=${Step.Sending} chain=${chain.chainId}")
                                        viewModel.sendTransaction(signedTransactionData, chain, sessionRequest, account, null)
                                    }
                                if (sendError == null) {
                                    Timber.d("$TAG sendTransaction success topic=$topic requestId=${sessionRequest?.request?.id} step=$step chain=${chain.chainId}")
                                    processCompleted = true
                                    RxBus.publish(WCChangeEvent())
                                    Step.Done
                                } else {
                                    Timber.d("$TAG sendTransaction error topic=$topic requestId=${sessionRequest?.request?.id} step=$step chain=${chain.chainId} error=$sendError")
                                    errorInfo = sendError
                                    Step.Error
                                }
                            } catch (e: Exception) {
                                handleException(e)
                                Step.Error
                            }
                        } else {
                            processCompleted = true
                            RxBus.publish(WCChangeEvent())
                            defaultSharedPreferences.putLong(
                                Constants.BIOMETRIC_PIN_CHECK,
                                System.currentTimeMillis(),
                            )
                            Step.Done
                        }
                } else {
                    errorInfo = error
                    step = Step.Error
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(e)
            }
        }

    private suspend fun approveWithPriv(priv: ByteArray): String? {
        when (version) {
            WalletConnect.Version.V2 -> {
                when (requestType) {
                    RequestType.Connect -> {}
                    RequestType.SessionProposal -> {
                        WalletConnectV2.approveSession(topic)
                    }
                    RequestType.SessionRequest -> {
                        val signData = this.signData ?: return "SignData is null"
                        signedTransactionData =
                            WalletConnectV2.approveRequest(
                                priv,
                                chain,
                                topic,
                                signData,
                                {
                                    val latestBlockhash = rpc.getLatestBlockhash() ?: throw IllegalArgumentException("failed to get blockhash")
                                    return@approveRequest latestBlockhash
                                },
                                { address ->
                                    val nonce = rpc.nonceAt(chain.assetId, address) ?: throw IllegalArgumentException("failed to get nonce")
                                    return@approveRequest nonce
                                },
                                { address ->
                                    viewModel.outputsByAddress(address, Chain.Bitcoin.assetId)
                                },
                                { rawTx ->
                                    viewModel.estimateBitcoinFee(rawTx)
                                },
                            )
                    }
                    RequestType.Pay -> {}
                }
            }
            WalletConnect.Version.TIP -> {
                Timber.e("$TAG wcActionWithPriv")
            }
        }
        return null
    }

    private fun reject() {
        when (version) {
            WalletConnect.Version.V2 -> {
                when (requestType) {
                    RequestType.Connect -> {}
                    RequestType.SessionProposal -> {
                        WalletConnectV2.rejectSession(topic)
                    }
                    RequestType.SessionRequest -> {
                        WalletConnectV2.rejectRequest(topic = topic)
                    }
                    RequestType.Pay -> {}
                }
            }
            WalletConnect.Version.TIP -> {
                Timber.e("$TAG wcActionWithPriv")
            }
        }
    }

    private fun handleException(e: Exception) {
        errorInfo =
            when (e) {
                is TipNetworkException -> {
                    "code: ${e.error.code}, message: ${e.error.description}"
                }
                is WalletConnectException -> {
                    "code: ${e.code}, message: ${e.message}"
                }
                is RpcException -> {
                    solanaErrorHandler.reset()
                        .addHandler(JupiterErrorHandler(e.rawResponse))
                        .addHandler(RaydiumErrorHandler(e.rawResponse))
                        .addHandler(ProgramErrorHandler(e.rawResponse))
                        .start(requireContext())
                }
                else -> {
                    ErrorHandler.getErrorMessage(e)
                }
            }
        reportException("$TAG handleException", e)
        Timber.e(e)
        step = Step.Error
    }

    private fun handleException(e: Throwable) {
        errorInfo = ErrorHandler.getErrorMessage(e)
        reportException("$TAG handleException", e)
        step = Step.Error
    }

    private fun accountFor(chain: Chain): String =
        when (chain) {
            Chain.Solana -> Web3Signer.solanaAddress
            Chain.Bitcoin -> Web3Signer.btcAddress
            else -> Web3Signer.evmAddress
        }

    private fun proposalAccountText(sessionProposal: Wallet.Model.SessionProposal): String {
        return formatProposalAccountText(
            sessionProposal.getProposalChainIds(),
            WalletConnectAddresses(
                evm = Web3Signer.evmAddress,
                solana = Web3Signer.solanaAddress,
                bitcoin = Web3Signer.btcAddress,
            ),
        )
    }

    private fun isSendEvmTransaction() = signData?.sessionRequest?.request?.method == Method.ETHSendTransaction.name

    private fun isSignSolanaTransaction() = signData != null && signData?.signMessage is VersionedTransactionCompat

    private fun isSendBitcoinTransfer() = signData != null && signData?.signMessage is WcBitcoinSendTransfer

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

    fun setOnPinComplete(callback: suspend (String) -> String?): WalletConnectBottomSheetDialogFragment {
        onPinCompleteAction = callback
        return this
    }

    fun setOnReject(callback: () -> Unit): WalletConnectBottomSheetDialogFragment {
        onRejectAction = callback
        return this
    }

    private var onPinCompleteAction: (suspend (String) -> String?)? = null
    private var onRejectAction: (() -> Unit)? = null

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
}

fun showWalletConnectBottomSheetDialogFragment(
    tip: Tip,
    fragmentActivity: FragmentActivity,
    requestType: RequestType,
    version: WalletConnect.Version,
    topic: String?,
    onReject: (() -> Unit)? = null,
    callback: (suspend (ByteArray) -> Unit)? = null,
) {
    val wcBottomSheet = WalletConnectBottomSheetDialogFragment.newInstance(requestType, version, topic)
    callback?.let {
        wcBottomSheet.setOnPinComplete { pin ->
            val result = tip.getOrRecoverTipPriv(fragmentActivity, pin)
            if (result.isSuccess) {
                callback.invoke(result.getOrThrow())
                return@setOnPinComplete null
            } else {
                val e = result.exceptionOrNull()
                val errorInfo = e?.stackTraceToString()
                Timber.d(
                    "${
                        when (version) {
                            WalletConnect.Version.V2 -> WalletConnectV2.TAG
                            else -> WalletConnectTIP.TAG
                        }
                    } $errorInfo",
                )
                return@setOnPinComplete if (e is TipNetworkException) {
                    "code: ${e.error.code}, message: ${e.error.description}"
                } else {
                    e?.let { ErrorHandler.getErrorMessage(it) }
                }
            }
        }
    }
    onReject?.let {
        wcBottomSheet.setOnReject(it)
    }
    wcBottomSheet.showNow(
        fragmentActivity.supportFragmentManager,
        WalletConnectBottomSheetDialogFragment.TAG,
    )
}
