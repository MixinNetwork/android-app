package one.mixin.android.ui.tip.wc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
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
import com.google.gson.GsonBuilder
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.R
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnect.RequestType
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectException
import one.mixin.android.tip.wc.internal.getChain
import one.mixin.android.tip.wc.internal.getChainByChainId
import one.mixin.android.tip.wc.internal.toTransaction
import one.mixin.android.tip.wc.internal.walletConnectChainIdMap
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
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.reportException
import one.mixin.android.util.tickerFlow
import one.mixin.android.vo.safe.Token
import org.sol4k.VersionedTransaction
import org.sol4k.exception.RpcException
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class WalletConnectBottomSheetDialogFragment : BottomSheetDialogFragment() {
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

    private var behavior: BottomSheetBehavior<*>? = null

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            step =
                when (requestType) {
                    RequestType.Connect -> Step.Connecting
                    RequestType.SessionProposal -> Step.Input
                    RequestType.SessionRequest -> Step.Sign
                }
            setContent {
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
                            onPreviewMessage = { TextPreviewActivity.show(requireContext(), it) },
                            onDismissRequest = { dismiss() },
                            showPin = { showPin() },
                        )
                    }
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                val ctx = requireContext()
                behavior?.peekHeight = ctx.realSize().y - ctx.statusBarHeight() - ctx.navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }

            checkV2ChainAndParseSignData()
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
        lifecycleScope.launch {
            val topic = this@WalletConnectBottomSheetDialogFragment.topic

            when (requestType) {
                RequestType.SessionProposal -> {
                    sessionProposal =
                        viewModel.getV2SessionProposal(topic)?.apply {
                            requiredNamespaces.values.firstOrNull()?.chains?.firstOrNull()?.getChain()?.let { chain = it }
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
                if (chain != Chain.Solana) {
                    PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                } else {
                    PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
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

            if (signData.signMessage is WCEthereumTransaction) {
                refreshEstimatedGasAndAsset(chain)
            } else if (signData.signMessage is VersionedTransaction) {
                asset = viewModel.refreshAsset(Chain.Solana.assetId)
            }
        }

    private fun refreshEstimatedGasAndAsset(chain: Chain) {
        val signData = this.signData ?: return

        val tx = signData.signMessage
        if (tx !is WCEthereumTransaction) return
        val assetId = walletConnectChainIdMap[chain.symbol]
        if (assetId == null) {
            Timber.d("$TAG refreshEstimatedGasAndAsset assetId not support")
            return
        }

        tickerFlow(15.seconds)
            .onEach {
                asset = viewModel.refreshAsset(assetId)
                if (version == WalletConnect.Version.V2) {
                    try {
                        val gasPrice = viewModel.ethGasPrice(chain) ?: return@onEach
                        val gasLimit = viewModel.ethGasLimit(chain, tx.toTransaction()) ?: return@onEach
                        val maxPriorityFeePerGas = viewModel.ethMaxPriorityFeePerGas(chain) ?: return@onEach
                        tipGas = TipGas(chain.chainId, gasPrice, gasLimit, maxPriorityFeePerGas, tx)
                        (signData as? WalletConnect.WCSignData.V2SignData)?.tipGas = tipGas
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun doAfterPinComplete(pin: String) =
        lifecycleScope.launch {
            step = Step.Loading
            try {
                val error =
                    withContext(Dispatchers.IO) {
                        if (onPinCompleteAction != null) {
                            onPinCompleteAction?.invoke(pin)
                        } else {
                            val privateKey = viewModel.getWeb3Priv(requireContext(), pin, chain.assetId)
                            approveWithPriv(privateKey)
                        }
                    }
                if (error == null) {
                    step =
                        if (isSignEvmTransaction() || isSignSolanaTransaction()) {
                            try {
                                step = Step.Sending
                                val sendError =
                                    withContext(Dispatchers.IO) {
                                        val sessionRequest = this@WalletConnectBottomSheetDialogFragment.sessionRequest ?: return@withContext "sessionRequest is null"
                                        val signedTransactionData = this@WalletConnectBottomSheetDialogFragment.signedTransactionData ?: return@withContext "signedTransactionData is null"
                                        if (signedTransactionData is VersionedTransaction) {
                                            viewModel.sendTransaction(signedTransactionData, sessionRequest)
                                        } else {
                                            viewModel.sendTransaction(version, chain, sessionRequest, signedTransactionData as String)
                                        }
                                    }
                                if (sendError == null) {
                                    processCompleted = true
                                    Step.Done
                                } else {
                                    errorInfo = sendError
                                    Step.Error
                                }
                            } catch (e: Exception) {
                                handleException(e)
                                Step.Error
                            }
                        } else {
                            processCompleted = true
                            Step.Done
                        }
                } else {
                    errorInfo = error
                    step = Step.Error
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }

    private fun approveWithPriv(priv: ByteArray): String? {
        when (version) {
            WalletConnect.Version.V2 -> {
                when (requestType) {
                    RequestType.Connect -> {}
                    RequestType.SessionProposal -> {
                        WalletConnectV2.approveSession(priv, topic)
                    }
                    RequestType.SessionRequest -> {
                        val signData = this.signData ?: return "SignData is null"
                        signedTransactionData = WalletConnectV2.approveRequest(priv, chain, topic, signData)
                    }
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
                    e.stackTraceToString()
                }
            }
        reportException("$TAG handleException", e)
        Timber.e(e)
        step = Step.Error
    }

    private fun handleException(e: Throwable) {
        errorInfo = e.message
        reportException("$TAG handleException", e)
        step = Step.Error
    }

    private fun isSignEvmTransaction() = signData != null && signData?.signMessage is WCEthereumTransaction

    private fun isSignSolanaTransaction() = signData != null && signData?.signMessage is VersionedTransaction

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
                    errorInfo
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
