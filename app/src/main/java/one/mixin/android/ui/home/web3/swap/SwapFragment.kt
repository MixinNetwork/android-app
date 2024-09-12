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
import kotlinx.coroutines.CoroutineExceptionHandler
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
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.solanaNativeTokenAssetKey
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.api.response.wrappedSolTokenAssetKey
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.putInt
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.ui.wallet.SwapTransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.ChainType
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
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
        const val ARGS_WEB3_TOKENS = "args_web3_tokens"
        const val ARGS_TOKEN_ITEMS = "args_token_items"
        const val ARGS_INPUT = "args_input"
        const val ARGS_OUTPUT = "args_output"
        const val ARGS_AMOUNT = "args_amount"

        const val MaxSlippage = 5000
        const val DangerousSlippage = 500
        const val MinSlippage = 10
        const val DefaultSlippage = 100

        const val maxLeftAmount = 0.01

        inline fun <reified T : Swappable> newInstance(
            tokens: List<T>? = null,
            input: String? = null,
            output: String? = null,
            amount: String? = null,
        ): SwapFragment =
            SwapFragment().withArgs {
                when (T::class) {
                    Web3Token::class -> {
                        putParcelableArrayList(ARGS_WEB3_TOKENS, arrayListOf<T>().apply { if (tokens != null) { addAll(tokens) } })
                    }

                    TokenItem::class -> {
                        putParcelableArrayList(ARGS_TOKEN_ITEMS, arrayListOf<T>().apply { tokens?.let { addAll(it) } })
                    }
                }
                input?.let { putString(ARGS_INPUT, it) }
                output?.let { putString(ARGS_OUTPUT, it) }
                amount?.let { putString(ARGS_AMOUNT, it) }
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
    private var quoteCountDown: Float by mutableFloatStateOf(0f)
    private var slippage: Int by mutableIntStateOf(0)
    private var isLoading by mutableStateOf(false)
    private var errorInfo: String? = null
    private val web3tokens: List<Web3Token>? by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_WEB3_TOKENS, Web3Token::class.java)
    }
    private var tokenItems: List<TokenItem>? = null

    private var quoteResp: QuoteResponse? = null
    private var txhash: String? = null

    private val swapViewModel by viewModels<SwapViewModel>()
    private val textInputFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slippage = defaultSharedPreferences.getInt(PREF_SWAP_SLIPPAGE, DefaultSlippage)
        if (slippage > DefaultSlippage) {
            slippage = DefaultSlippage
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
            initFromTo()
            refreshTokens()
            initAmount()
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
                            SwapPage(isLoading, fromToken, toToken, inputText, outputText, exchangeRate, slippage, quoteCountDown, errorInfo, {
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
                                SwapSlippageBottomSheetDialogFragment.newInstance(slippage)
                                    .setOnSlippage { bps ->
                                        slippage = bps
                                        defaultSharedPreferences.putInt(PREF_SWAP_SLIPPAGE, bps)
                                        refreshQuote(inputText.value)
                                    }
                                    .showNow(parentFragmentManager, SwapSlippageBottomSheetDialogFragment.TAG)
                            }, {
                                val a = calcInput()
                                inputText.value = a
                                refreshQuote(a)
                            }, {
                                lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
                                    toast(error.message ?: getString(R.string.Unknown))
                                }) {
                                    val qr = quoteResp ?: return@launch
                                    val inputMint = fromToken?.getUnique() ?: return@launch
                                    val outputMint = toToken?.getUnique() ?: return@launch

                                    quoteJob?.cancel()
                                    isLoading = true
                                    val swapResult =
                                        handleMixinResponse(
                                            invokeNetwork = { swapViewModel.web3Swap(SwapRequest(
                                                if (inMixin()) Session.getAccountId()!! else JsSigner.solanaAddress,
                                                inputMint,
                                                if (inMixin()) 0 else qr.inAmount.toLong(),
                                                if (inMixin()) qr.inAmount else "0",
                                                outputMint,
                                                qr.slippage,
                                                qr.source,
                                                qr.jupiterQuoteResponse,
                                            )) },
                                            successBlock = {
                                                return@handleMixinResponse it.data
                                            },
                                            exceptionBlock = { _ ->
                                                isLoading = false
                                                return@handleMixinResponse false
                                            },
                                            failureBlock = { r ->
                                                errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                                                isLoading = false
                                                return@handleMixinResponse false
                                            },
                                        )
                                    if (swapResult == null) {
                                        throw IllegalStateException(getString(R.string.Data_error))
                                    }
                                    if (inMixin()) {
                                        // Check tokens
                                        swapViewModel.checkAndSyncTokens(listOfNotNull(fromToken?.assetId, toToken?.assetId))
                                        isLoading = false
                                        openSwapTransfer(swapResult)
                                        return@launch
                                    }
                                    val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = swapResult.tx, solanaTxSource = SolanaTxSource.InnerSwap)
                                    JsSigner.useSolana()
                                    isLoading = false
                                    showBrowserBottomSheetDialogFragment(
                                        requireActivity(),
                                        signMessage,
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

    private suspend fun openSwapTransfer(swapResult: SwapResponse){
        val inputToken = tokenItems?.find { it.assetId == swapResult.quote.inputMint } ?: swapViewModel.findToken(swapResult.quote.inputMint) ?: throw IllegalStateException(getString(R.string.Data_error))
        val outToken = tokenItems?.find { it.assetId == swapResult.quote.outputMint } ?: swapViewModel.findToken(swapResult.quote.outputMint) ?: throw IllegalStateException(getString(R.string.Data_error))
        SwapTransferBottomSheetDialogFragment.newInstance(swapResult, inputToken, outToken).apply {
            setOnDone { clearInputAndRefreshInMixinFromToToken() }
        }.showNow(parentFragmentManager, SwapTransferBottomSheetDialogFragment.TAG)
    }

    private suspend fun initFromTo() {
        tokenItems = requireArguments().getParcelableArrayListCompat(ARGS_TOKEN_ITEMS, TokenItem::class.java)
        var swappable = web3tokens ?: tokenItems
        if (swappable == null) {
            swappable = swapViewModel.allAssetItems()
            tokenItems = swappable
        }
        swappable.let { tokens ->
            val input = requireArguments().getString(ARGS_INPUT)
            val output = requireArguments().getString(ARGS_OUTPUT)
            if (tokens.isNotEmpty()) {
                fromToken = input?.let { tokens.firstOrNull { t -> t.getUnique() == input }?.toSwapToken() } ?: tokens[0].toSwapToken()
            }
            if (tokens.size > 1) {
                toToken = output?.let { tokens.firstOrNull { t -> t.getUnique() == output }?.toSwapToken() } ?: tokens[1].toSwapToken()
            }
        }
    }

    private fun initAmount() {
        val amount = requireArguments().getString(ARGS_AMOUNT)
        if (amount?.toFloatOrNull() != null) {
            inputText.value = amount
            refreshQuote(amount)
        }
    }

    private val selectCallback = fun(
        list: List<SwapToken>,
        index: Int,
    ) {
        if (index == 0) {
            if (inMixin()) {
                AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_SEND, ArrayList(list.map { t -> t.assetId }))
                    .setOnAssetClick { t ->
                        val token = t.toSwapToken()
                        if (token == toToken) {
                            toToken = fromToken
                        }
                        fromToken = token
                        lifecycleScope.launch {
                            refreshTokensPrice(listOf(token))
                            onTextChanged(currentText)
                        }
                    }.setOnDepositClick {
                        parentFragmentManager.popBackStackImmediate()
                    }
                    .showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
            } else {
                Web3TokenListBottomSheetDialogFragment.newInstance(
                    ArrayList(web3tokens ?: emptyList()),
                ).apply {
                    setOnClickListener { t ->
                        val token = t.toSwapToken()
                        if (token == toToken) {
                            toToken = fromToken
                        }
                        fromToken = token
                        lifecycleScope.launch {
                            refreshTokensPrice(listOf(token))
                            onTextChanged(currentText)
                        }
                        dismissNow()
                    }
                }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
            }
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
                    if (token == fromToken) {
                        fromToken = toToken
                    }
                    toToken = token
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
                swapViewModel.web3Tokens(getSource())
            },
            successBlock = { resp ->
                resp.data
            },
            failureBlock = {
                if (it.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID)
                    refreshTokens()
                } else if (it.errorCode == ErrorHandler.OLD_VERSION) {
                    alertDialogBuilder()
                        .setTitle(R.string.Update_Mixin)
                        .setMessage(getString(R.string.update_mixin_description, requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName))
                        .setNegativeButton(R.string.Later) { dialog, _ ->
                            dialog.dismiss()
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }.setPositiveButton(R.string.Update) { dialog, _ ->
                            requireContext().openMarket()
                            dialog.dismiss()
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }.setCancelable(false)
                        .create().show()
                }
                return@handleMixinResponse true
            },
        )?.let {
            if (!inMixin()) {
                swapTokens = it.map { token ->
                    val t =
                        web3tokens?.firstOrNull { web3Token ->
                            web3Token.assetKey == token.address || (token.address == wrappedSolTokenAssetKey && web3Token.assetKey == solanaNativeTokenAssetKey)
                        } ?: return@map token
                    token.balance = t.balance
                    token
                }
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                }
                toToken = swapTokens.firstOrNull { s -> s.address != fromToken?.address }
            } else {
                swapTokens = it.map { token ->
                    val t = tokenItems?.firstOrNull { tokenItem ->
                        tokenItem.assetId == token.assetId
                    } ?: return@map token
                    token.balance = t.balance
                    token.price = t.priceUsd
                    token
                }
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                    toToken = swapTokens.getOrNull(1)
                } else {
                    val found = swapTokens.firstOrNull { s -> s.assetId == fromToken?.assetId }
                    if (found == null) {
                        if (fromToken != null) {
                            toast(getString(R.string.swap_not_supported, fromToken?.symbol))
                        }
                        fromToken = swapTokens[0]
                    }
                    if (toToken != null) {
                        val toFound = swapTokens.firstOrNull { s -> s.assetId == toToken?.assetId }
                        if (toFound == null) {
                            toToken = swapTokens.getOrNull(1)
                        }
                    } else {
                        toToken = swapTokens.getOrNull(1)
                    }
                }
            }
            if (fromToken != null  && toToken != null) {
                refreshTokensPrice(listOf(fromToken!!, toToken!!))
            }
        }
    }

    private suspend fun refreshTokensPrice(tokens: List<SwapToken>): List<SwapToken> {
        if (inMixin()) {
            val newTokens = swapViewModel.syncAndFindTokens(tokens.map { it.assetId })
            if (newTokens.isEmpty()) {
                return tokens
            }
            tokens.forEachWithIndex { _, token ->
                newTokens.forEach { t ->
                    if (t.assetId == token.assetId) {
                        token.price = t.priceUsd
                    }
                }
            }
        } else {
            val web3Tokens = swapViewModel.web3Tokens(chain = ChainType.solana.name, address = tokens.map { it.address })
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
                        repeat(100) { t ->
                            quoteCountDown = t / 100f
                            delay(.1.seconds)
                        }
                        refreshQuote(text)
                    }
            }
        }
    }

    private suspend fun quote(input: String) {
        val inputMint = fromToken?.getUnique() ?: return
        val outputMint = toToken?.getUnique() ?: return
        val amount = if (inMixin()) {
            input
        } else {
            val a = fromToken?.toLongAmount(input) ?: return
            if (a <= 0L) return
            a.toString()
        }

        isLoading = true
        errorInfo = null
        val resp = handleMixinResponse(
            invokeNetwork = { swapViewModel.web3Quote(inputMint, outputMint, amount, slippage.toString(), getSource()) },
            successBlock = {
                return@handleMixinResponse it.data
            },
            failureBlock = { r ->
                errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                return@handleMixinResponse true
            },
            endBlock = {
                isLoading = false
            },
        ) ?: return

        quoteResp = resp
        updateExchangeRate(resp.inAmount, resp.outAmount)
        slippage = resp.slippage
        outputText = toToken?.toStringAmount(resp.outAmount) ?: "0"
    }

    private fun updateExchangeRate(inAmount: String, outAmount: String) {
        val inValue = fromToken?.realAmount(inAmount)
        val outValue = toToken?.realAmount(outAmount)
        exchangeRate =
            if (inValue == null || outValue == null || inValue == BigDecimal.ZERO || outValue == BigDecimal.ZERO) {
                0f
            } else {
                outValue.divide(inValue, RoundingMode.CEILING).toFloat()
            }
    }

    private fun calcInput(): String {
        val from = this.fromToken ?: return ""
        val balance = from.balance ?: "0"
        val calc = fun(balance: BigDecimal): String {
            return balance.setScale(9, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
        }
        var b = BigDecimal(balance)
        if (!from.isSolToken() || b <= BigDecimal(maxLeftAmount)) {
            return calc(b)
        }
        b = b.subtract(BigDecimal(maxLeftAmount))
        return calc(b)
    }

    private fun clearInputAndRefreshInMixinFromToToken() {
        if (!inMixin()) return

        val list = mutableListOf<String>()
        fromToken?.let { list.add(it.assetId) }
        toToken?.let { list.add(it.assetId) }
        lifecycleScope.launch {
            val newTokens = swapViewModel.syncAndFindTokens(list)
            if (newTokens.isEmpty()) {
                return@launch
            }
            newTokens.forEach { token ->
                if (token.assetId == fromToken?.assetId) {
                    fromToken = null // keep
                    fromToken = token.toSwapToken()
                } else if (token.assetId == toToken?.assetId) {
                    toToken = null // keep
                    toToken = token.toSwapToken()
                }
            }
            inputText.value = ""
            exchangeRate = 0f
            quoteResp = null
            onTextChanged("")
            textInputFlow.emit("")
        }
    }

    private fun inMixin(): Boolean = web3tokens == null
    private fun getSource(): String = if (web3tokens == null) "mixin" else ""

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
