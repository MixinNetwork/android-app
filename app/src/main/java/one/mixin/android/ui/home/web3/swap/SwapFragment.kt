package one.mixin.android.ui.home.web3.swap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.Constants.AssetId.XIN_ASSET_ID
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.api.response.web3.sortByKeywordAndBalance
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.solanaNativeTokenAssetKey
import one.mixin.android.db.web3.vo.wrappedSolTokenAssetKey
import one.mixin.android.event.BadgeEvent
import one.mixin.android.extension.addToList
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.extension.getList
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOrdersJob
import one.mixin.android.job.RefreshPendingOrdersJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.GasCheckBottomSheetDialogFragment
import one.mixin.android.ui.wallet.DepositFragment
import one.mixin.android.ui.wallet.SwapTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class SwapFragment : BaseFragment() {
    companion object {
        const val TAG = "SwapFragment"
        const val ARGS_WEB3_TOKENS = "args_web3_tokens"
        const val ARGS_TOKEN_ITEMS = "args_token_items"
        const val ARGS_INPUT = "args_input"
        const val ARGS_OUTPUT = "args_output"
        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_IN_MIXIN = "args_in_mixin"
        const val ARGS_REFERRAL = "args_referral"
        const val ARGS_WALLET_ID = "args_wallet_id"

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
            inMixin: Boolean = true,
            referral: String? = null,
            walletId: String? = null,
        ): SwapFragment =
            SwapFragment().withArgs {
                when (T::class) {
                    Web3TokenItem::class -> {
                        putParcelableArrayList(ARGS_WEB3_TOKENS, arrayListOf<T>().apply {
                            if (tokens != null) {
                                addAll(tokens)
                            }
                        })
                    }

                    TokenItem::class -> {
                        putParcelableArrayList(ARGS_TOKEN_ITEMS, arrayListOf<T>().apply {
                            if (tokens != null) {
                                addAll(tokens)
                            }
                        })
                    }
                }
                input?.let { putString(ARGS_INPUT, it) }
                output?.let { putString(ARGS_OUTPUT, it) }
                amount?.let { putString(ARGS_AMOUNT, it) }
                putBoolean(ARGS_IN_MIXIN, inMixin)
                referral?.let { putString(ARGS_REFERRAL, it) }
                walletId?.let { putString(ARGS_WALLET_ID, it) }
            }
    }

    enum class SwapDestination {
        Swap,
        OrderList,
        OrderDetail
    }

    @Inject
    lateinit var rpc: Rpc

    private var swapTokens: List<SwapToken> by mutableStateOf(emptyList())
    private var remoteSwapTokens: List<SwapToken> by mutableStateOf(emptyList())
    private var tokenItems: List<TokenItem>? = null
    private var web3tokens: List<Web3TokenItem>? = null
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)

    private var initialAmount: String? = null
    private var lastOrderTime: Long by mutableLongStateOf(0)
    private var reviewing: Boolean by mutableStateOf(false)
    private val walletId: String? by lazy { arguments?.getString(ARGS_WALLET_ID) }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val swapViewModel by viewModels<SwapViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderBadge = defaultSharedPreferences.getInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 1
    }

    private var orderBadge: Boolean by mutableStateOf(false)

    @FlowPreview
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        initAmount()
        lifecycleScope.launch {
            initFromTo()
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
                            jobManager.addJobInBackground(RefreshOrdersJob())
                            jobManager.addJobInBackground(RefreshPendingOrdersJob())
                            SwapPage(
                                walletId = walletId,
                                from = fromToken,
                                to = toToken,
                                inMixin = inMixin(),
                                orderBadge = orderBadge,
                                initialAmount = initialAmount,
                                lastOrderTime = lastOrderTime,
                                reviewing = reviewing,
                                onSelectToken = { isReverse, type ->
                                    if ((type == SelectTokenType.From && !isReverse) || (type == SelectTokenType.To && isReverse)) {
                                        selectCallback(swapTokens, isReverse, type)
                                    } else {
                                        selectCallback(remoteSwapTokens, isReverse, type)
                                    }
                                },
                                onReview = { quote, from, to, amount ->
                                    lifecycleScope.launch {
                                        handleReview(quote, from, to, amount, navController)
                                    }
                                },
                                source = getSource(),
                                onDeposit = { token ->
                                    hideKeyboard()
                                    if (inMixin()) {
                                        deposit(token.assetId)
                                    } else {
                                        navTo(
                                            Web3AddressFragment.newInstance(if (token.chain.chainId == Constants.ChainId.SOLANA_CHAIN_ID) JsSigner.solanaAddress else JsSigner.evmAddress),
                                            Web3AddressFragment.TAG
                                        )
                                    }
                                },
                                onOrderList = {
                                    navController.navigate(SwapDestination.OrderList.name)
                                    if (defaultSharedPreferences.getInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 1) {
                                        defaultSharedPreferences.putInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, 1)
                                        orderBadge = false
                                        RxBus.publish(BadgeEvent(Account.PREF_HAS_USED_SWAP_TRANSACTION))
                                    }
                                },
                                pop = {
                                    navigateUp(navController)
                                }
                            )
                        }

                        composable(SwapDestination.OrderList.name) {
                            jobManager.addJobInBackground(RefreshOrdersJob())
                            jobManager.addJobInBackground(RefreshPendingOrdersJob())
                            SwapOrderListPage(
                                walletId = walletId,
                                pop = {
                                    navigateUp(navController)
                                },
                                onOrderClick = { orderId ->
                                    navController.navigate("${SwapDestination.OrderDetail.name}/$orderId")
                                }
                            )
                        }
                        composable("${SwapDestination.OrderDetail.name}/{orderId}") { navBackStackEntry ->
                            jobManager.addJobInBackground(RefreshOrdersJob())
                            jobManager.addJobInBackground(RefreshPendingOrdersJob())
                            navBackStackEntry.arguments?.getString("orderId")?.toIntOrNull().let { orderId ->
                                SwapOrderDetailPage(
                                    walletId = walletId,
                                    orderId = navBackStackEntry.arguments?.getString("orderId") ?: "",
                                    onShare = { payAssetId, receiveAssetId ->
                                        lifecycleScope.launch {
                                            shareSwap(payAssetId, receiveAssetId)
                                        }
                                    },
                                    onTryAgain = { fromAssetId, toAssetId ->
                                        lifecycleScope.launch {
                                            val fromToken = swapViewModel.findToken(fromAssetId)?.toSwapToken()
                                            val toToken = swapViewModel.findToken(toAssetId)?.toSwapToken()
                                            if (fromToken != null && toToken != null) {
                                                this@SwapFragment.fromToken = fromToken
                                                this@SwapFragment.toToken = toToken
                                                navController.navigate(SwapDestination.Swap.name) {
                                                    popUpTo(SwapDestination.Swap.name) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    pop = {
                                        navigateUp(navController)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val selectCallback = fun(
        list: List<SwapToken>,
        isReverse: Boolean,
        type: SelectTokenType,
    ) {
        if ((type == SelectTokenType.From && !isReverse) || (type == SelectTokenType.To && isReverse)) {
            if (inMixin()) {
                SwapTokenListBottomSheetDialogFragment.newInstance(
                    Account.PREF_FROM_SWAP,
                    ArrayList(list), if (isReverse) toToken?.assetId else fromToken?.assetId,
                    isFrom = true
                ).apply {
                    if (list.isEmpty()) {
                        setLoading(true)
                    }
                    setOnDeposit {
                        parentFragmentManager.popBackStackImmediate()
                        dismissNow()
                    }
                    setOnClickListener { t, _ ->
                        saveQuoteToken(t, isReverse, type)
                        requireContext().defaultSharedPreferences.addToList(Account.PREF_FROM_SWAP, t, SwapToken::class.java)
                        dismissNow()
                    }
                }.show(parentFragmentManager, SwapTokenListBottomSheetDialogFragment.TAG)
            } else {
                SwapTokenListBottomSheetDialogFragment.newInstance(
                    Constants.Account.PREF_FROM_WEB3_SWAP,
                    ArrayList(
                        list,
                    ),
                    isFrom = true,
                ).apply {
                    setOnDeposit {
                        navTo(Web3AddressFragment.newInstance(JsSigner.evmAddress), Web3AddressFragment.TAG)
                        dismissNow()
                    }
                    setOnClickListener { token, alert ->
                        if (alert) {
                            SwapTokenBottomSheetDialogFragment.newInstance(token).showNow(parentFragmentManager, SwapTokenBottomSheetDialogFragment.TAG)
                            return@setOnClickListener
                        }
                        requireContext().defaultSharedPreferences.addToList(Constants.Account.PREF_FROM_WEB3_SWAP, token, SwapToken::class.java)
                        saveQuoteToken(token, isReverse, type)
                        dismissNow()
                    }
                }.show(parentFragmentManager, SwapTokenListBottomSheetDialogFragment.TAG)
            }
        } else {
            SwapTokenListBottomSheetDialogFragment.newInstance(
                if (inMixin()) Account.PREF_TO_SWAP else Constants.Account.PREF_TO_WEB3_SWAP,
                tokens =
                    ArrayList(
                        list.run {
                            this
                        },
                    ),
                if (inMixin()) {
                    if (isReverse) fromToken?.assetId else toToken?.assetId
                } else null,
                isFrom = false,
            ).apply {
                if (list.isEmpty()) {
                    setLoading(true)
                }
                setOnClickListener { token, alert ->
                    if (alert) {
                        SwapTokenBottomSheetDialogFragment.newInstance(token).showNow(parentFragmentManager, SwapTokenBottomSheetDialogFragment.TAG)
                        return@setOnClickListener
                    }
                    requireContext().defaultSharedPreferences.addToList(if (inMixin()) Account.PREF_TO_SWAP else Constants.Account.PREF_TO_WEB3_SWAP, token, SwapToken::class.java)
                    saveQuoteToken(token, isReverse, type)
                    dismissNow()
                }
            }.show(parentFragmentManager, SwapTokenListBottomSheetDialogFragment.TAG)
        }
    }

    private val dialog: Dialog by lazy {
        indeterminateProgressDialog(
            message = R.string.Please_wait_a_bit,
        ).apply {
            setCancelable(false)
        }
    }

    private fun deposit(tokenId: String) {
        lifecycleScope.launch {
            val token = swapViewModel.findToken(tokenId)
            if (token == null) {
                dialog.show()
                runCatching {
                    swapViewModel.checkAndSyncTokens(listOf(tokenId))
                    val t = swapViewModel.findToken(tokenId)
                    if (t != null)
                        navTo(DepositFragment.newInstance(t), DepositFragment.TAG)
                    else
                        toast(R.string.Not_found)
                }.onFailure {
                    ErrorHandler.handleError(it)
                }.getOrNull()

                dialog.dismiss()
            } else {
                runCatching {
                    navTo(DepositFragment.newInstance(token), DepositFragment.TAG)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private fun capFormat(vol: String, rate: BigDecimal, symbol: String): String {
        val formatVol = try {
            BigDecimal(vol).multiply(rate).numberFormatCompact()
        } catch (_: NumberFormatException) {
            null
        }
        if (formatVol != null) {
            return "$symbol$formatVol"
        }
        return requireContext().getString(R.string.N_A)
    }

    private suspend fun shareSwap(payAssetId: String, receiveAssetId: String) {
        dialog.show()
        runCatching {
            var payId = payAssetId
            var receiveId =
                if (receiveAssetId in Constants.AssetId.usdcAssets || receiveAssetId in Constants.AssetId.usdtAssets) {
                    payId = receiveAssetId
                    payAssetId
                } else {
                    receiveAssetId
                }

            var forwardMessage: ForwardMessage? = null
            swapViewModel.checkMarketById(receiveId)?.let { market ->
                forwardMessage = buildForwardMessage(market, payId, receiveId)
            }
            if (forwardMessage == null) {
                swapViewModel.syncAsset(receiveId)?.let { token ->
                    forwardMessage = buildForwardMessage(token, payId, receiveId)
                }
            }
            if (forwardMessage == null) {
                toast(R.string.Data_error)
                return@runCatching
            }
            ShareMessageBottomSheetDialogFragment.newInstance(forwardMessage!!, null)
                .showNow(parentFragmentManager, ShareMessageBottomSheetDialogFragment.TAG)
        }.onFailure { e ->
            ErrorHandler.handleError(e)
        }
        dialog.dismiss()
    }

    private fun buildForwardMessage(marketItem: MarketItem, payId: String, receiveId: String): ForwardMessage {
        val description = buildString {
            append("🔥 ${marketItem.name} (${marketItem.symbol})\n\n")
            append(
                "📈 ${getString(R.string.Market_Cap)}: ${
                    capFormat(
                        marketItem.marketCap,
                        BigDecimal(Fiats.getRate()),
                        Fiats.getSymbol()
                    )
                }\n"
            )
            append("🏷️ ${getString(R.string.Price)}: ${Fiats.getSymbol()}${BigDecimal(marketItem.currentPrice).priceFormat()}\n")
            append("💰 ${getString(R.string.price_change_24h)}: ${marketItem.priceChangePercentage24H}%")
        }

        val actions = listOf(
            ActionButtonData(
                label = getString(R.string.buy_token, marketItem.symbol),
                color = "#50BD5C",
                action = "${Constants.Scheme.HTTPS_SWAP}?input=$payId&output=$receiveId&referral=${Session.getAccount()?.identityNumber}"
            ),
            ActionButtonData(
                label = getString(R.string.sell_token, marketItem.symbol),
                color = "#DB454F",
                action = "${Constants.Scheme.HTTPS_SWAP}?input=$receiveId&output=$payId&referral=${Session.getAccount()?.identityNumber}"
            ),
            ActionButtonData(
                label = "${marketItem.symbol} ${getString(R.string.Market)}",
                color = "#3D75E3",
                action = "${Constants.Scheme.HTTPS_MARKET}/${marketItem.coinId}"
            )
        )

        val appCard = AppCardData(
            appId = ROUTE_BOT_USER_ID,
            iconUrl = null,
            coverUrl = null,
            cover = null,
            title = "${getString(R.string.Swap)} ${marketItem.symbol}",
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = actions,
        )

        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }

    private fun buildForwardMessage(token: TokenItem, payId: String, receiveId: String): ForwardMessage {
        val description = buildString {
            append("🔥 ${token.name} (${token.symbol})\n\n")
            append("🏷️ ${getString(R.string.Price)}: ${Fiats.getSymbol()}${BigDecimal(token.priceUsd).priceFormat()}\n")
            append(
                "💰 ${getString(R.string.price_change_24h)}: ${
                    runCatching { "${(BigDecimal(token.changeUsd) * BigDecimal(100)).numberFormat2()}%" }.getOrDefault(
                        "N/A"
                    )
                }"
            )
        }

        val actions = listOf(
            ActionButtonData(
                label = getString(R.string.buy_token, token.symbol),
                color = "#50BD5C",
                action = "${Constants.Scheme.HTTPS_SWAP}?input=$payId&output=$receiveId&referral=${Session.getAccount()?.identityNumber}"
            ),
            ActionButtonData(
                label = getString(R.string.sell_token, token.symbol),
                color = "#DB454F",
                action = "${Constants.Scheme.HTTPS_SWAP}?input=$receiveId&output=$payId&referral=${Session.getAccount()?.identityNumber}"
            ),
            ActionButtonData(
                label = "${token.symbol} ${getString(R.string.Market)}",
                color = "#3D75E3",
                action = "${Constants.Scheme.HTTPS_MARKET}/${token.assetId}"
            )
        )

        val appCard = AppCardData(
            appId = ROUTE_BOT_USER_ID,
            iconUrl = null,
            coverUrl = null,
            cover = null,
            title = "${getString(R.string.Swap)} ${token.symbol}",
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = actions,
        )

        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }

    private fun saveQuoteToken(
        token: SwapToken,
        isReverse: Boolean,
        type: SelectTokenType,
    ) {
        if (type == SelectTokenType.From) {
            if (token == toToken) {
                toToken = fromToken
            }
            fromToken = token
        } else {
            if (token == fromToken) {
                fromToken = toToken
            }
            toToken = token
        }

        fromToken?.let { from ->
            toToken?.let { to ->
                val tokenPair = if (isReverse) listOf(to, from) else listOf(from, to)
                val serializedPair = GsonHelper.customGson.toJson(tokenPair)
                defaultSharedPreferences.putString(preferenceKey, serializedPair)
            }
        }
    }

    private suspend fun handleReview(quote: QuoteResult, from: SwapToken, to: SwapToken, amount: String, navController: NavHostController) {
        val inputMint = from.assetId
        val outputMint = to.assetId

        val resp = requestRouteAPI(
            invokeNetwork = {
                swapViewModel.web3Swap(
                    SwapRequest(
                        Session.getAccountId()!!,
                        inputMint,
                        quote.inAmount,
                        outputMint,
                        quote.payload,
                        getSource(),
                        if (inMixin()) null else {
                            if (to.chain.chainId == Constants.ChainId.SOLANA_CHAIN_ID) JsSigner.solanaAddress else JsSigner.evmAddress
                        },
                        getReferral(),
                    )
                )
            },
            requestSession = { swapViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            successBlock = { it.data },
            exceptionBlock = { t ->
                Timber.e(t)
                false
            },
            failureBlock = { r ->
                Timber.e(r.errorDescription)
                false
            }
        )
        if (resp == null) return
        if (inMixin()) {
            AnalyticsTracker.trackSwapPreview()
            openSwapTransfer(resp, from, to)
        } else {
            AnalyticsTracker.trackSwapPreview()
            openSwapTransfer(resp, from, to)
        }
    }

    private fun openSwapTransfer(swapResult: SwapResponse, from: SwapToken, to: SwapToken) {
        if (from.chain.chainId == Constants.ChainId.Solana || inMixin()) {
            SwapTransferBottomSheetDialogFragment.newInstance(swapResult, from, to).apply {
                setOnDone {
                    initialAmount = null
                    lastOrderTime = System.currentTimeMillis()
                }
                setOnDestroy {
                    reviewing = false
                }
            }.showNow(parentFragmentManager, SwapTransferBottomSheetDialogFragment.TAG)
            reviewing = true
        } else {
            GasCheckBottomSheetDialogFragment.newInstance(swapResult, from, to).apply {
                setOnDone {
                    initialAmount = null
                    lastOrderTime = System.currentTimeMillis()
                }
                setOnDestroy {
                    reviewing = false
                }
            }.showNow(
                parentFragmentManager,
                GasCheckBottomSheetDialogFragment.TAG
            )
        }
    }

    private suspend fun initFromTo() {
        tokenItems = requireArguments().getParcelableArrayListCompat(ARGS_TOKEN_ITEMS, TokenItem::class.java)
        var swappable = web3tokens ?: tokenItems
        if (!inMixin() && web3tokens.isNullOrEmpty()) {
            if (walletId == null) {
                toast(R.string.Data_error)
                return
            }
            swappable = swapViewModel.findWeb3AssetItemsWithBalance(walletId!!)
            web3tokens = swappable
        } else if (swappable.isNullOrEmpty()) {
            swappable = swapViewModel.findAssetItemsWithBalance()
            tokenItems = swappable
        }
        swappable.map { it.toSwapToken() }.toList().let {
            swapTokens = it.sortByKeywordAndBalance()
        }
        swappable.let { tokens ->
            val input = requireArguments().getString(ARGS_INPUT)
            val output = requireArguments().getString(ARGS_OUTPUT)
            val lastSelectedPairJson = defaultSharedPreferences.getString(preferenceKey, null)
            val lastSelectedPair: List<SwapToken>? = lastSelectedPairJson?.let {
                val type = object : TypeToken<List<SwapToken>>() {}.type
                GsonHelper.customGson.fromJson(it, type)
            }
            val lastFrom = lastSelectedPair?.getOrNull(0)
            val lastTo = lastSelectedPair?.getOrNull(1)

            fromToken = if (input != null) {
                swapViewModel.findToken(input)?.toSwapToken() ?: swapViewModel.web3TokenItemById(walletId!!,input)?.toSwapToken()
            } else lastFrom
                ?: (tokens.firstOrNull { it.getUnique() == USDT_ASSET_ETH_ID }
                    ?: tokens.firstOrNull())?.toSwapToken()

            toToken = if (output != null) {
                swapViewModel.findToken(output)?.toSwapToken() ?: swapViewModel.web3TokenItemById(walletId!!,
                    output
                )?.toSwapToken()
            } else if (input != null) {
                val o = if (input == USDT_ASSET_ETH_ID) {
                    XIN_ASSET_ID
                } else {
                    USDT_ASSET_ETH_ID
                }
                swapViewModel.findToken(o)?.toSwapToken() ?: swapViewModel.web3TokenItemById(walletId!!, o)
                    ?.toSwapToken()
            } else lastTo
                ?: tokens.firstOrNull { t -> t.getUnique() != fromToken?.getUnique() }
                    ?.toSwapToken()
            if (toToken?.getUnique() == fromToken?.getUnique()) {
                toToken = tokens.firstOrNull { t -> t.getUnique() != fromToken?.getUnique() }?.toSwapToken()
            }
        }
    }

    private val saveSwapTokens by lazy {
        if (inMixin()) defaultSharedPreferences.getList(Account.PREF_FROM_SWAP, SwapToken::class.java) + defaultSharedPreferences.getList(Account.PREF_TO_SWAP, SwapToken::class.java)
        else emptyList()
    }

    private suspend fun refreshTokens() {
        requestRouteAPI(
            invokeNetwork = { swapViewModel.web3Tokens(getSource()) },
            successBlock = { resp ->
                resp.data
            },
            requestSession = { swapViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            failureBlock = { r ->
                if (r.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                    refreshTokens()
                } else if (r.errorCode == ErrorHandler.OLD_VERSION) {
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
                return@requestRouteAPI true
            },
        )?.let { remote ->
            if (!inMixin()) {
                remoteSwapTokens = remote.map { it.copy(isWeb3 = true, walletId = walletId) }.map { token ->
                    val t = web3tokens?.firstOrNull { web3Token ->
                        (web3Token.assetKey == token.address && web3Token.assetId == token.assetId) || (token.address == wrappedSolTokenAssetKey && web3Token.assetKey == solanaNativeTokenAssetKey)
                    } ?: return@map token
                    token.balance = t.balance
                    token
                }.sortByKeywordAndBalance()

                swapTokens = swapTokens.union(remoteSwapTokens).toList().sortByKeywordAndBalance()
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                }
                if (toToken == null || toToken?.getUnique() == fromToken?.getUnique()) {
                    toToken = swapTokens.firstOrNull { s -> s.assetId != fromToken?.assetId } ?: swapTokens.getOrNull(1) ?: swapTokens[0]
                }
                if (swapTokens.isNotEmpty()) {
                    (parentFragmentManager.findFragmentByTag(SwapTokenListBottomSheetDialogFragment.TAG) as? SwapTokenListBottomSheetDialogFragment)?.setLoading(false, swapTokens, remoteSwapTokens)
                }
            } else {
                remoteSwapTokens = remote.map { token ->
                    val t = tokenItems?.firstOrNull { tokenItem ->
                        tokenItem.assetId == token.assetId
                    } ?: return@map token
                    token.balance = t.balance
                    token.price = t.priceUsd
                    token
                }.sortByKeywordAndBalance()
                swapTokens = swapTokens.union(remoteSwapTokens).toList().sortByKeywordAndBalance()
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                }
                if (toToken == null || toToken?.getUnique() == fromToken?.getUnique()) {
                    toToken = swapTokens.firstOrNull { s -> s.assetId != fromToken?.assetId }
                }
            }
            if (swapTokens.isNotEmpty()) {
                (parentFragmentManager.findFragmentByTag(SwapTokenListBottomSheetDialogFragment.TAG) as? SwapTokenListBottomSheetDialogFragment)?.setLoading(false, swapTokens, remoteSwapTokens)
            }
            if (fromToken != null && toToken != null) {
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
        }
        return tokens
    }

    private fun initAmount() {
        initialAmount = arguments?.getString(ARGS_AMOUNT)
    }

    private fun inMixin(): Boolean = arguments?.getBoolean(ARGS_IN_MIXIN, true) ?: true
    private val preferenceKey by lazy { if (inMixin()) PREF_SWAP_LAST_PAIR else "$PREF_WEB3_SWAP_LAST_PAIR ${JsSigner.currentWalletId}"}
    private fun getSource(): String = if (inMixin()) "mixin" else "web3"

    private fun getReferral(): String? = arguments?.getString(ARGS_REFERRAL)

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
