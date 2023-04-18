package one.mixin.android.ui.tip.wc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.GasPriceType
import one.mixin.android.api.response.TipGas
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnect.RequestType
import one.mixin.android.tip.wc.WalletConnectException
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.walletConnectChainIdMap
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.tip.wc.sessionproposal.SessionProposalPage
import one.mixin.android.ui.tip.wc.sessionrequest.SessionRequestPage
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Asset
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class WalletConnectBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletConnectBottomSheetDialogFragment"

        const val ARGS_REQUEST_TYPE = "args_request_type"
        const val ARGS_VERSION = "args_version"

        fun newInstance(requestType: RequestType, version: WalletConnect.Version) = WalletConnectBottomSheetDialogFragment().withArgs {
            putInt(ARGS_REQUEST_TYPE, requestType.ordinal)
            putInt(ARGS_VERSION, version.ordinal)
        }
    }

    enum class Step {
        Sign, Send, Input, Loading, Done, Error,
    }

    private var behavior: BottomSheetBehavior<*>? = null
    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<WalletConnectBottomSheetViewModel>()

    private var pinCompleted = false

    private val requestType by lazy { RequestType.values()[requireArguments().getInt(ARGS_REQUEST_TYPE)] }
    private val version by lazy { WalletConnect.Version.values()[requireArguments().getInt(ARGS_VERSION)] }
    private val wc by lazy {
        when (version) {
            WalletConnect.Version.V1 -> WalletConnectV1
            WalletConnect.Version.V2 -> WalletConnectV2
            else -> WalletConnectTIP
        }
    }

    private var step by mutableStateOf(Step.Input)
    private var errorInfo: String? by mutableStateOf(null)
    private var tipGas: TipGas? by mutableStateOf(null)
    private var asset: Asset? by mutableStateOf(null)
    private var gasPriceType: GasPriceType by mutableStateOf(GasPriceType.Propose)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                snapshotFlow { step }.collect { value ->
                    if (value == Step.Input) {
                        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        step = when (requestType) {
            RequestType.SessionProposal -> Step.Input
            RequestType.SessionRequest -> Step.Sign
            RequestType.SwitchNetwork -> Step.Input
        }
        setContent {
            when (requestType) {
                RequestType.SessionProposal, RequestType.SwitchNetwork -> {
                    SessionProposalPage(
                        version,
                        step,
                        requestType,
                        errorInfo,
                        onDismissRequest = { dismiss() },
                        onBiometricClick = { showBiometricPrompt() },
                        onPinComplete = { pin -> doAfterPinComplete(pin) },
                    )
                }
                RequestType.SessionRequest -> {
                    val gson = GsonBuilder()
                        .serializeNulls()
                        .setPrettyPrinting()
                        .create()
                    SessionRequestPage(
                        gson,
                        version,
                        step,
                        asset,
                        tipGas,
                        gasPriceType,
                        errorInfo,
                        onPreviewMessage = { TextPreviewActivity.show(requireContext(), it) },
                        onDismissRequest = { dismiss() },
                        onPositiveClick = { onPositiveClick(it) },
                        onBiometricClick = { showBiometricPrompt() },
                        onPinComplete = { pin -> doAfterPinComplete(pin) },
                        onGasItemClick = { type ->
                            gasPriceType = type
                            if (version == WalletConnect.Version.V1) {
                                (wc.currentSignData as? WalletConnect.WCSignData.V1SignData)?.gasPriceType = type
                            } else if (version == WalletConnect.Version.V2) {
                                (wc.currentSignData as? WalletConnect.WCSignData.V2SignData)?.gasPriceType = type
                            }
                        },
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

        refreshEstimatedGasAndAsset()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
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
        if (!pinCompleted) {
            Timber.d("$TAG dismiss onReject")
            onReject?.invoke()
        }
        super.onDismiss(dialog)
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is WalletConnectActivity || activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private fun refreshEstimatedGasAndAsset() {
        val signData = wc.currentSignData ?: return
        val tx = signData.signMessage
        if (tx !is WCEthereumTransaction) return
        val assetId = walletConnectChainIdMap[
            when (version) {
                WalletConnect.Version.V1 -> {
                    WalletConnectV1.chain.symbol
                }
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.chain.symbol
                }
                else -> return
            },
        ]
        if (assetId == null) {
            Timber.d("$TAG refreshEstimatedGasAndAsset assetId not support")
            return
        }

        tickerFlow(15.seconds)
            .onEach {
                asset = viewModel.refreshAsset(assetId)
                tipGas = handleMixinResponse(
                    invokeNetwork = { viewModel.getTipGas(assetId) },
                    successBlock = {
                        it.data
                    },
                )
                if (version == WalletConnect.Version.V1) {
                    (wc.currentSignData as? WalletConnect.WCSignData.V1SignData)?.tipGas = tipGas
                } else if (version == WalletConnect.Version.V2) {
                    (wc.currentSignData as? WalletConnect.WCSignData.V2SignData)?.tipGas = tipGas
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    private fun doAfterPinComplete(pin: String) = lifecycleScope.launch {
        step = Step.Loading
        try {
            val error = withContext(Dispatchers.IO) {
                onPinComplete?.invoke(pin)
            }
            if (error == null) {
                pinCompleted = true
                step = if (viewModel.isTransaction(version)) {
                    Step.Send
                } else {
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

    private fun onPositiveClick(requestId: Long?) {
        if (step == Step.Sign) {
            step = Step.Input
        } else if (step == Step.Send) {
            if (requestId != null) {
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            viewModel.sendTransaction(version, requestId)
                        }
                        step = Step.Done
                    } catch (e: Exception) {
                        handleException(e)
                    }
                }
            } else {
                step = Step.Done
            }
        }
    }

    private fun handleException(e: Exception) {
        errorInfo = if (e is WalletConnectException) {
            "code: ${e.code}, message: ${e.message}"
        } else {
            e.stackTraceToString()
        }
        step = Step.Error
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> dismiss()
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    fun setOnPinComplete(callback: suspend (String) -> String?): WalletConnectBottomSheetDialogFragment {
        onPinComplete = callback
        return this
    }

    fun setOnReject(callback: () -> Unit): WalletConnectBottomSheetDialogFragment {
        onReject = callback
        return this
    }

    private var onPinComplete: (suspend (String) -> String?)? = null
    private var onReject: (() -> Unit)? = null

    private var biometricDialog: BiometricDialog? = null
    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireActivity(),
            getBiometricInfo(),
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
    )

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onPinComplete(pin: String) {
            doAfterPinComplete(pin)
        }

        override fun showPin() {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@WalletConnectBottomSheetDialogFragment.requireActivity())
        }

        override fun onCancel() {}
    }
}

fun showWalletConnectBottomSheetDialogFragment(
    tip: Tip,
    fragmentActivity: FragmentActivity,
    requestType: RequestType,
    version: WalletConnect.Version,
    onReject: () -> Unit,
    callback: suspend (ByteArray) -> Unit,
) {
    val wcBottomSheet = WalletConnectBottomSheetDialogFragment.newInstance(requestType, version)
    wcBottomSheet.setOnPinComplete { pin ->
        val result = tip.getOrRecoverTipPriv(fragmentActivity, pin)
        if (result.isSuccess) {
            callback(result.getOrThrow())
            return@setOnPinComplete null
        } else {
            val e = result.exceptionOrNull()
            val errorInfo = e?.stackTraceToString()
            Timber.d(
                "${
                    when (version) {
                        WalletConnect.Version.V2 -> WalletConnectV2.TAG
                        WalletConnect.Version.V1 -> WalletConnectV1.TAG
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
    }.setOnReject { onReject() }
        .showNow(
            fragmentActivity.supportFragmentManager,
            WalletConnectBottomSheetDialogFragment.TAG,
        )
}
