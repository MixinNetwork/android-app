package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.app.Dialog
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
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.toTransaction
import one.mixin.android.tip.wc.internal.walletConnectChainIdMap
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment.Step
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetViewModel
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.reportException
import one.mixin.android.util.tickerFlow
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.JsSignMessage
import one.mixin.android.web3.JsSigner
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class BrowserWalletBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "BrowserWalletBottomSheetDialogFragment"

        const val ARGS_MESSAGE = "args_message"
        const val ARGS_URL = "args_url"
        const val ARGS_TITLE = "args_title"

        fun newInstance(
            jsSignMessage: JsSignMessage,
            url:String?,
            title:String?
        ) = BrowserWalletBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_MESSAGE, jsSignMessage)
            putString(ARGS_URL, url?.run {
                Uri.parse(this).host
            })
            putString(ARGS_TITLE, title)
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<WalletConnectBottomSheetViewModel>()

    private val signMessage: JsSignMessage by lazy { requireArguments().getParcelableCompat(ARGS_MESSAGE, JsSignMessage::class.java)!! }
    private val url: String? by lazy { requireArguments().getString(ARGS_URL) }
    private val title: String? by lazy { requireArguments().getString(ARGS_TITLE) }

    var step by mutableStateOf(Step.Input)
        private set
    private var errorInfo: String? by mutableStateOf(null)
    private var tipGas: TipGas? by mutableStateOf(null)
    private var asset: Token? by mutableStateOf(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                BrowserPage(JsSigner.address, JsSigner.currentChain, signMessage.type, step, tipGas, asset, signMessage.wcEthereumTransaction, signMessage.reviewData,
                    url,
                    title,
                    errorInfo,
                    onPreviewMessage = { TextPreviewActivity.show(requireContext(), it) },
                    showPin = { showPin() }, onDismissRequest = { dismiss() })
            }

            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                val ctx = requireContext()
                behavior?.peekHeight = ctx.realSize().y - ctx.statusBarHeight() - ctx.navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }


            refreshEstimatedGasAndAsset(JsSigner.currentChain)
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

    private fun refreshEstimatedGasAndAsset(chain: Chain) {
        val assetId = walletConnectChainIdMap[chain.symbol]
        val transaction = signMessage.wcEthereumTransaction ?: return
        if (assetId == null) {
            Timber.d("$TAG refreshEstimatedGasAndAsset assetId not support")
            return
        }
        tickerFlow(15.seconds)
            .onEach {
                asset = viewModel.refreshAsset(assetId)
                try {
                    val gasPrice = viewModel.ethGasPrice(chain) ?: return@onEach
                    val gasLimit = viewModel.ethGasLimit(chain, transaction.toTransaction()) ?: return@onEach
                    val maxPriorityFeePerGas = viewModel.ethMaxPriorityFeePerGas(chain) ?: return@onEach
                    tipGas = TipGas(chain.chainId, gasPrice, gasLimit, maxPriorityFeePerGas, transaction)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun doAfterPinComplete(pin: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                step = Step.Loading
                if (signMessage.type == JsSignMessage.TYPE_TRANSACTION) {
                    val transaction = requireNotNull(signMessage.wcEthereumTransaction)
                    val priv = viewModel.getTipPriv(requireContext(), pin)
                    val hex = JsSigner.ethSignTransaction(priv, transaction, tipGas!!)
                    step = Step.Sending
                    val hash = JsSigner.sendTransaction(hex)
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$hash\");")
                } else if (signMessage.type == JsSignMessage.TYPE_TYPED_MESSAGE || signMessage.type == JsSignMessage.TYPE_MESSAGE) {
                    val priv = viewModel.getTipPriv(requireContext(), pin)
                    val hex = JsSigner.signMessage(priv, requireNotNull(signMessage.data), signMessage.type)
                    onDone?.invoke("window.${JsSigner.currentNetwork}.sendResponse(${signMessage.callbackId}, \"$hex\");")
                }
                step = Step.Done
            } catch (e: Exception) {
                handleException(e)
            }
        }

    private fun handleException(e: Throwable) {
        Timber.e(e)
        errorInfo = e.message
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

    private var onDone: ((String?) -> Unit)? = null
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

fun showBrowserBottomSheetDialogFragment(
    tip: Tip,
    signMessage: JsSignMessage,
    currentUrl: String?,
    currentTitle: String?,
    fragmentActivity: FragmentActivity,
    onReject: (() -> Unit)? = null,
    onDone: ((String?) -> Unit)? = null,
) {
    val wcBottomSheet = BrowserWalletBottomSheetDialogFragment.newInstance(signMessage, currentUrl, currentTitle)
    onDone?.let {
        wcBottomSheet.setOnDone(onDone)
    }
    onReject?.let {
        wcBottomSheet.setOnReject(onReject)
    }
    wcBottomSheet.showNow(
        fragmentActivity.supportFragmentManager,
        BrowserWalletBottomSheetDialogFragment.TAG,
    )
}