package one.mixin.android.ui.home.web3.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_SWAP_SLIPPAGE
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.solanaNativeTokenAssetKey
import one.mixin.android.api.response.toSwapToken
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.wrappedSolTokenAssetKey
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putInt
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
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

        const val MaxSlippage = 5000
        const val DangerousSlippage = 500
        const val MinSlippage = 10
        const val DefaultSlippage = 100

        const val maxLeftAmount = 0.01

        fun newInstance(tokens: List<Web3Token>): SwapFragment =
            SwapFragment().withArgs {
                putParcelableArrayList("TOKENS", arrayListOf<Web3Token>().apply { addAll(tokens) })
            }
    }

    enum class SwapDestination {
        Swap,
    }

    private var swapTokens: List<SwapToken> by mutableStateOf(emptyList())
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)
    private var inputText = mutableStateOf("")
    private var outputText: String by mutableStateOf("")
    private var exchangeRate: Float by mutableFloatStateOf(0f)
    private var slippageBps: Int by mutableIntStateOf(0)
    private var isLoading by mutableStateOf(false)
    private val web3tokens by lazy {
        requireArguments().getParcelableArrayListCompat("TOKENS", Web3Token::class.java)!!
    }

    private var quoteResp: QuoteResponse? = null
    private var txhash: String? = null

    private val swapViewModel by viewModels<SwapViewModel>()
    private val textInputFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slippageBps = defaultSharedPreferences.getInt(PREF_SWAP_SLIPPAGE, DefaultSlippage)
        if (slippageBps > DefaultSlippage) {
            slippageBps = DefaultSlippage
            defaultSharedPreferences.putInt(PREF_SWAP_SLIPPAGE, DefaultSlippage)
        }

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
            if (web3tokens.size > 0) {
                fromToken = web3tokens[0].toSwapToken()
            }
            if (web3tokens.size > 1) {
                toToken = web3tokens[1].toSwapToken()
            }
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
                            SwapPage(isLoading, fromToken, toToken, inputText, outputText, exchangeRate, slippageBps, {
                                val token = fromToken
                                fromToken = toToken
                                toToken = token
                                onTextChanged(currentText)
                            }, { index ->
                                if (swapTokens.isNotEmpty()) {
                                    selectCallback(swapTokens, index)
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
                                val qr = quoteResp ?: return@SwapPage
                                SwapSlippageBottomSheetDialogFragment.newInstance(qr.slippageBps)
                                    .setOnSlippage { bps ->
                                        val needQuote = bps != slippageBps
                                        slippageBps = bps
                                        defaultSharedPreferences.putInt(PREF_SWAP_SLIPPAGE, bps)
                                        if (needQuote) {
                                            lifecycleScope.launch {
                                                quote(currentText)
                                            }
                                        }
                                    }
                                    .showNow(parentFragmentManager, SwapSlippageBottomSheetDialogFragment.TAG)
                            }, {
                                val a = calcInput(true)
                                inputText.value = a
                                refreshQuote(a)
                            }, {
                                val a = calcInput(false)
                                inputText.value = a
                                refreshQuote(a)
                            }, {
                                lifecycleScope.launch {
                                    val qr = quoteResp ?: return@launch
                                    quoteJob?.cancel()
                                    isLoading = true
                                    val swapResult =
                                        handleMixinResponse(
                                            invokeNetwork = { swapViewModel.web3Swap(SwapRequest(JsSigner.solanaAddress, qr)) },
                                            successBlock = {
                                                return@handleMixinResponse it.data
                                            },
                                            exceptionBlock = { _ ->
                                                isLoading = false
                                                return@handleMixinResponse false
                                            },
                                            failureBlock = {
                                                isLoading = false
                                                return@handleMixinResponse false
                                            },
                                        ) ?: return@launch
                                    val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = swapResult.swapTransaction)
                                    JsSigner.useSolana()
                                    isLoading = false
                                    showBrowserBottomSheetDialogFragment(
                                        requireActivity(),
                                        signMessage,
                                        amount = qr.inAmount,
                                        onTxhash = { hash, serializedTx ->
                                            lifecycleScope.launch {
                                                txhash = hash
                                                val txStateFragment =
                                                    TransactionStateFragment.newInstance(serializedTx, toToken!!.symbol).apply {
                                                        setCloseAction {
                                                            navigateUp(navController)
                                                            parentFragmentManager.popBackStackImmediate()
                                                        }
                                                    }
                                                navTo(txStateFragment, TransactionStateFragment.TAG)
                                            }
                                        },
                                    )
                                }
                            }) {
                                navigateUp(navController)
                            }
                        }
                    }
                }
            }
        }
    }

    private val selectCallback = fun(
        list: List<SwapToken>,
        index: Int,
    ) {
        if (index == 0) {
            Web3TokenListBottomSheetDialogFragment.newInstance(
                ArrayList(web3tokens),
            ).apply {
                setOnClickListener { t ->
                    val token = t.toSwapToken()
                    if (token == this@SwapFragment.toToken) {
                        this@SwapFragment.toToken = fromToken
                    }
                    this@SwapFragment.fromToken = token
                    lifecycleScope.launch {
                        refreshTokensPrice(listOf(token))
                        onTextChanged(currentText)
                    }
                    dismissNow()
                }
            }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
        } else {
            SwapTokenListBottomSheetDialogFragment.newInstance(
                ArrayList(
                    list.run {
                        this
                    },
                ),
            ).apply {
                setOnClickListener { token, alert ->
                    if (alert) {
                        SwapTokenBottomSheetDialogFragment.newInstance(token).showNow(parentFragmentManager, SwapTokenBottomSheetDialogFragment.TAG)
                        return@setOnClickListener
                    }
                    if (token == this@SwapFragment.fromToken) {
                        this@SwapFragment.fromToken = toToken
                    }
                    this@SwapFragment.toToken = token
                    lifecycleScope.launch {
                        refreshTokensPrice(listOf(token))
                        onTextChanged(currentText)
                    }
                    dismissNow()
                }
            }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
        }
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
                if (it.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID)
                    refreshTokens()
                }
                return@handleMixinResponse true
            },
        )?.let {
            swapTokens =
                it.map { token ->
                    val t =
                        web3tokens.firstOrNull { web3Token ->
                            web3Token.assetKey == token.address || (token.address == wrappedSolTokenAssetKey && web3Token.assetKey == solanaNativeTokenAssetKey)
                        } ?: return@map token
                    token.balance = t.balance
                    token
                }
            if (fromToken == null) {
                fromToken = swapTokens.firstOrNull { t -> fromToken?.address == t.address } ?: swapTokens[0]
            }
            toToken = swapTokens.firstOrNull { t -> toToken?.address == t.address } ?: swapTokens[1]

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
        currentText = text
        refreshQuote(text)
    }

    private fun refreshQuote(text: String) {
        quoteJob?.cancel()
        if (text.isBlank()) {
            outputText = "0"
        } else {
            val inputValue =
                try {
                    BigDecimal(text)
                } catch (e: Exception) {
                    BigDecimal.ZERO
                }
            if (inputValue <= BigDecimal.ZERO) {
                outputText = "0"
            } else {
                quoteJob =
                    lifecycleScope.launch {
                        quote(text)
                        delay(10.seconds)
                        refreshQuote(text)
                    }
            }
        }
    }

    private suspend fun quote(input: String) {
        val inputMint = fromToken?.address ?: return
        val outputMint = toToken?.address ?: return
        val amount = fromToken?.toLongAmount(input) ?: return
        if (amount <= 0L) return

        isLoading = true
        quoteResp = handleMixinResponse(
            invokeNetwork = { swapViewModel.web3Quote(inputMint, outputMint, amount.toString(), slippageBps) },
            successBlock = {
                return@handleMixinResponse it.data
            },
            endBlock = {
                isLoading = false
            },
        ) ?: return

        val inValue = fromToken?.realAmount(quoteResp?.inAmount?.toLongOrNull() ?: 0L)
        val outValue = toToken?.realAmount(quoteResp?.outAmount?.toLongOrNull() ?: 0L)
        exchangeRate =
            if (inValue == null || outValue == null || inValue == BigDecimal.ZERO || outValue == BigDecimal.ZERO) {
                0f
            } else {
                outValue.divide(inValue, RoundingMode.CEILING).toFloat()
            }
        slippageBps = quoteResp?.slippageBps ?: 0
        outputText = toToken?.toStringAmount(quoteResp?.outAmount?.toLongOrNull() ?: 0L) ?: "0"
    }

    private fun calcInput(half: Boolean): String {
        val from = this.fromToken ?: return ""
        val balance = from.balance ?: "0"
        val calc = fun(balance: BigDecimal): String {
            return if (half) {
                balance.divide(BigDecimal(2))
            } else {
                balance
            }.setScale(9, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
        }
        var b = BigDecimal(balance)
        if (!from.isSolToken() || b <= BigDecimal(maxLeftAmount)) {
            return calc(b)
        }
        b = b.subtract(BigDecimal(maxLeftAmount))
        return calc(b)
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
