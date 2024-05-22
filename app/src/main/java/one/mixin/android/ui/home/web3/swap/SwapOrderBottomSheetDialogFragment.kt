package one.mixin.android.ui.home.web3.swap

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.home.web3.BrowserWalletBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner

@AndroidEntryPoint
class SwapOrderBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "SwapOrderBottomSheetDialogFragment"

        fun newInstance(fromToken: SwapToken, toToken: SwapToken, qr: QuoteResponse) = SwapOrderBottomSheetDialogFragment().withArgs {
            putParcelable("QUOTE", qr)
            putParcelable("FROM", fromToken)
            putParcelable("TO", toToken)
        }
    }

    private val quoteResp: QuoteResponse by lazy {
        requireArguments().getParcelableCompat("QUOTE", QuoteResponse::class.java)!!
    }

    private val fromToken: SwapToken by lazy {
        requireArguments().getParcelableCompat("FROM", SwapToken::class.java)!!
    }

    private val toToken: SwapToken by lazy {
        requireArguments().getParcelableCompat("TO", SwapToken::class.java)!!
    }


    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    private val swapViewModel by viewModels<SwapViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                SwapOrderPage(quoteResp, fromToken, toToken, {dismiss()}, {
                    lifecycleScope.launch {
                        swap()
                    }
                })
            }

            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                val ctx = requireContext()
                behavior?.peekHeight = ctx.realSize().y - ctx.statusBarHeight() - ctx.navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }

        }

    private suspend fun swap() {
        val qr = quoteResp
        val swapResult = handleMixinResponse(
            invokeNetwork = { swapViewModel.web3Swap(SwapRequest(JsSigner.solanaAddress, qr)) },
            successBlock = {
                return@handleMixinResponse it.data
            }
        ) ?: return
        val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = swapResult.swapTransaction)
        JsSigner.useSolana()
        showBrowserBottomSheetDialogFragment(
            requireActivity(),
            signMessage,
            amount = qr.inAmount,
            onTxhash = { onTxhash?.invoke(it) }
        )
        dismiss()
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

    private var onTxhash: ((String) -> Unit)? = null

    fun setOnTxhash(callback: (String) -> Unit): SwapOrderBottomSheetDialogFragment {
        onTxhash = callback
        return this
    }
}
