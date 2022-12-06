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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectException
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.SystemUIManager
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class WalletConnectBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletConnectBottomSheetDialogFragment"

        const val ARGS_IS_ACCOUNT = "args_is_account"
        const val ARGS_ACTION = "args_action"
        const val ARGS_DESC = "args_desc"

        fun newInstance(isAccount: Boolean, action: String, desc: String? = null) = WalletConnectBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_ACCOUNT, isAccount)
            putString(ARGS_ACTION, action)
            desc?.let { putString(ARGS_DESC, it) }
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null
    override fun getTheme() = R.style.AppTheme_Dialog

    private var pinCompleted = false

    private var step by mutableStateOf(WCStep.Account)
    private var errorInfo: String? by mutableStateOf(null)
    private var desc: String? by mutableStateOf(null)

    init {
        lifecycleScope.launchWhenCreated {
            snapshotFlow { step }.collect { value ->
                if (value == WCStep.Input) {
                    dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        step = if (requireArguments().getBoolean(ARGS_IS_ACCOUNT)) WCStep.Account else WCStep.Choice
        desc = requireArguments().getString(ARGS_DESC)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WalletConnectCompose(
                step = step,
                networkName = WalletConnect.get().getNetworkName(),
                peerMeta = WalletConnect.get().remotePeerMeta,
                action = requireNotNull(requireArguments().getString(ARGS_ACTION)) { "action can not be null" },
                desc = desc,
                balance = WalletConnect.get().getBalanceString(),
                errorInfo = errorInfo,
                onDisconnectClick = {
                    WalletConnect.release()
                    dismiss()
                },
                onDismissClick = {
                    onReject?.invoke()
                    dismiss()
                },
                onCancelClick = {
                    onReject?.invoke()
                    dismiss()
                },
                onApproveClick = {
                    step = WCStep.Input
                },
                onBiometricClick = {
                    showBiometricPrompt()
                },
                onPinComplete = { pin ->
                    doAfterPinComplete(pin)
                }
            )
        }
        doOnPreDraw {
            val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
            behavior = params?.behavior as? BottomSheetBehavior<*>
            behavior?.peekHeight = 690.dp
            behavior?.isDraggable = false
            behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
        }

        refreshEstimatedGas()
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
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night)
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!pinCompleted && step != WCStep.Account) {
            Timber.d("${WalletConnect.TAG} dismiss onReject")
            onReject?.invoke()
        }
        super.onDismiss(dialog)
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private fun refreshEstimatedGas() {
        val tx = WalletConnect.get().currentWCEthereumTransaction ?: return

        tickerFlow(15.seconds)
            .onEach { desc = WalletConnect.get().getHumanReadableTransactionInfo(tx) }
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
        step = WCStep.Loading
        try {
            onPinComplete?.invoke(pin)
            pinCompleted = true
            step = WCStep.Done
            delay(1000)
            dismiss()
        } catch (e: Exception) {
            errorInfo = if (e is WalletConnectException) {
                "code: ${e.code}, message: ${e.message}"
            } else {
                e.stackTraceToString()
            }
            step = WCStep.Error
        }
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    fun setOnPinComplete(callback: suspend (String) -> Unit): WalletConnectBottomSheetDialogFragment {
        onPinComplete = callback
        return this
    }

    fun setOnReject(callback: () -> Unit): WalletConnectBottomSheetDialogFragment {
        onReject = callback
        return this
    }

    private var onPinComplete: (suspend (String) -> Unit)? = null
    private var onReject: (() -> Unit)? = null

    private var biometricDialog: BiometricDialog? = null
    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireActivity(),
            getBiometricInfo()
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        ""
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
