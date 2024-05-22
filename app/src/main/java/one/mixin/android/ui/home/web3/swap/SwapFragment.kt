package one.mixin.android.ui.home.web3.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment
import timber.log.Timber
import java.math.BigDecimal

@AndroidEntryPoint
class SwapFragment : BaseFragment() {
    companion object {
        const val TAG = "SwapFragment"

        fun newInstance(): SwapFragment = SwapFragment()
    }

    enum class SwapDestination {
        Swap,
    }

    private var list: List<SwapToken> by mutableStateOf(emptyList())
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)
    private var outputText: String by mutableStateOf("0")

    private var quoteResp: QuoteResponse? = null

    private val swapViewModel by viewModels<SwapViewModel>()
    private val textInputFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            textInputFlow
                .debounce(500L)
                .distinctUntilChanged()
                .collectLatest { text ->
                    onTextChanged(text)
                }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    swapViewModel.web3Tokens()
                },
                successBlock = { resp ->
                    resp.data
                },
            )?.let {
                list = it
                fromToken = list[0]
                toToken = list[1]

                refreshTokensPrice(listOf(fromToken!!, toToken!!))
            }
        }
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = SwapDestination.Swap.name,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300),
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300),
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300),
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300),
                            )
                        },
                    ) {
                        composable(SwapDestination.Swap.name) {
                            SwapPage(fromToken, toToken, list, outputText, {
                                val token = fromToken
                                fromToken = toToken
                                toToken = token
                            }, { index ->
                                if (list.isNotEmpty()) {
                                    selectCallback(list, index)
                                }
                            }, { input ->
                                if (input.isBlank()) {
                                    return@SwapPage
                                }
                                if (BigDecimal(input) == BigDecimal.ZERO) {
                                    return@SwapPage
                                }
                                Timber.e("input $input")
                                textInputFlow.value = input
                            }, {
                                lifecycleScope.launch { swap() }
                            }) {
                                navigateUp(navController)
                            }
                        }
                    }
                }
            }
        }
    }

    private val selectCallback = fun(list: List<SwapToken>, index: Int) {
        SwapTokenListBottomSheetDialogFragment.newInstance(ArrayList(list)).apply {
            setOnClickListener { token ->
                if (index == 0) {
                    this@SwapFragment.fromToken = token
                } else {
                    this@SwapFragment.toToken = token
                }
                lifecycleScope.launch {
                    refreshTokensPrice(listOf(token))
                    fromToken?.let { quote(it.address) }
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private suspend fun refreshTokensPrice(tokens: List<SwapToken>): List<SwapToken> {
        val web3Tokens = swapViewModel.web3Tokens(tokens.map { it.address })
        if (web3Tokens.isEmpty()) {
            return tokens
        }
        tokens.forEachIndexed { _, token ->
            web3Tokens.forEach { t ->
                if (t.assetKey.equals(token.address, true)) {
                    token.price = t.price
                }
            }
        }
        return tokens
    }

    private var quoteJob: Job? = null
    private fun onTextChanged(text: String) {
        quoteJob?.cancel()
        if (text.isEmpty() || text == "0") {
            quoteJob?.cancel()
            outputText = "0"
        } else {
            quoteJob = lifecycleScope.launch {
                quote(text)
            }
        }
    }

    private suspend fun quote(input: String) {
        val inputMint = fromToken?.address ?: return
        val outputMint = toToken?.address ?: return
        val amount = fromToken?.toLongAmount(input) ?: return
        quoteResp = handleMixinResponse(
            invokeNetwork = { swapViewModel.web3Quote(inputMint, outputMint, amount.toString()) },
            successBlock = {
                return@handleMixinResponse it.data
            }
        ) ?: return
        outputText = toToken?.toStringAmount(quoteResp?.outAmount?.toIntOrNull() ?: 0) ?: "0"
    }

    private suspend fun swap() {
        val qr = quoteResp ?: return
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
        )
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
