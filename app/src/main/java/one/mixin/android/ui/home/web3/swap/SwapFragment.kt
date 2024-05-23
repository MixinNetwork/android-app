package one.mixin.android.ui.home.web3.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.TxState
import one.mixin.android.api.response.web3.isFinalTxState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.tickerFlow
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class SwapFragment : BaseFragment() {
    companion object {
        const val TAG = "SwapFragment"

        fun newInstance(): SwapFragment = SwapFragment()
    }

    enum class SwapDestination {
        Swap, SwapState,
    }

    private var list: List<SwapToken> by mutableStateOf(emptyList())
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)
    private var outputText: String by mutableStateOf("")
    private var exchangeRate: Float by mutableFloatStateOf(0f)
    private var isLoading by mutableStateOf(false)

    private var tx: Tx? by mutableStateOf(null)

    private var quoteResp: QuoteResponse? = null
    private var txhash: String? = null

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
            refreshTokens()
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
                            SwapPage(isLoading, fromToken, toToken, list, outputText, exchangeRate, {
                                val token = fromToken
                                fromToken = toToken
                                toToken = token
                                onTextChanged(currentText)
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
                                val from  = fromToken?:return@SwapPage
                                val to = toToken ?: return@SwapPage
                                quoteResp?.let {
                                    SwapOrderBottomSheetDialogFragment.newInstance(from, to, it).apply {
                                        setOnTxhash { hash ->
                                            Timber.d("hash $hash")
                                            this@SwapFragment.txhash = hash
                                            navController.navigate("${SwapDestination.SwapState.name}/$hash") {
                                                popUpTo(SwapDestination.Swap.name)
                                            }
                                            refreshTx(hash)
                                        }
                                    }.showNow(parentFragmentManager, SwapOrderBottomSheetDialogFragment.TAG)
                                }
                            }) {
                                navigateUp(navController)
                            }
                        }
                        composable("${SwapDestination.SwapState.name}/{txhash}") { navBackStackEntry ->
                            navBackStackEntry.arguments?.getString("txhash")?.let { txhash ->
                                SwapStatePage(
                                    tx = tx ?: Tx(TxState.NotFound.name),
                                    fromToken = fromToken!!,
                                    toToken = toToken!!,
                                    quoteResp!!,
                                    viewTx = {
                                        WebActivity.show(context, "https://solscan.io/tx/${txhash}", null)
                                    }) {
                                        navigateUp(navController)
                                    }
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
                    onTextChanged(currentText)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private suspend fun refreshTokens() {
        handleMixinResponse(
            invokeNetwork = {
                swapViewModel.web3Tokens()
            },
            successBlock = { resp ->
                resp.data
            },
            failureBlock = {
                Timber.d("@@@ ${it.errorCode}")
                if (it.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID)
                    refreshTokens()
                }
                return@handleMixinResponse true
            }
        )?.let {
            list = it
            fromToken = list[0]
            toToken = list[1]

            refreshTokensPrice(listOf(fromToken!!, toToken!!))
        }
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
    private var currentText: String = "0"
    private fun onTextChanged(text: String) {
        quoteJob?.cancel()
        currentText = text
        if (text.isEmpty() || text == "0" || text.isBlank()) {
            quoteJob?.cancel()
            outputText = "0"
        } else {
            quoteJob = lifecycleScope.launch {
                quote(text)
            }
        }
    }

    private var refreshTxJob: Job? = null
    private fun refreshTx(txhash: String) {
        refreshTxJob?.cancel()
        refreshTxJob = tickerFlow(2.seconds)
            .onEach {
                handleMixinResponse(
                    invokeNetwork = { swapViewModel.getWeb3Tx(txhash) },
                    successBlock = {
                        tx = it.data
                    }
                )
                if (tx?.state?.isFinalTxState() == true) {
                    refreshTxJob?.cancel()
                }
            }.launchIn(lifecycleScope)
    }

    private suspend fun quote(input: String) {
        val inputMint = fromToken?.address ?: return
        val outputMint = toToken?.address ?: return
        val amount = fromToken?.toLongAmount(input) ?: return
        isLoading = true
        quoteResp = handleMixinResponse(
            invokeNetwork = { swapViewModel.web3Quote(inputMint, outputMint, amount.toString()) },
            successBlock = {
                return@handleMixinResponse it.data
            }, endBlock = {
                isLoading = false
            }
        ) ?: return

        val inValue = fromToken?.realAmount(quoteResp?.inAmount?.toLongOrNull() ?: 0L)
        val outValue = toToken?.realAmount(quoteResp?.outAmount?.toLongOrNull() ?: 0L)
        exchangeRate = if (inValue == null || outValue == null || inValue== BigDecimal.ZERO || outValue==BigDecimal.ZERO) {
            0f
        } else {
            outValue.divide(inValue, RoundingMode.CEILING).toFloat()
        }
        outputText = toToken?.toStringAmount(quoteResp?.outAmount?.toLongOrNull() ?: 0L) ?: "0"
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
