package one.mixin.android.ui.home.web3.trade

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.Constants.Account.PREF_LIMIT_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_LIMIT_SWAP_LAST_PAIR
import one.mixin.android.Constants.Account.PREF_WEB3_SWAP_LAST_PAIR
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.Constants.AssetId.XIN_ASSET_ID
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.GaslessTxRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.GaslessTxResponse
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.api.response.web3.sortByKeywordAndBalance
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenFeeItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.event.BadgeEvent
import one.mixin.android.extension.addToList
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.forEachWithIndex
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.trade.perps.AllPositionsFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpetualGuideBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.home.web3.trade.perps.PerpsMarketListBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.perps.PositionDetailFragment
import one.mixin.android.ui.wallet.AllOrdersFragment
import one.mixin.android.ui.wallet.DepositFragment
import one.mixin.android.ui.wallet.LimitTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.SwapTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.SwapTransferPreviewData
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.ui.wallet.transfer.TransferWeb3BalanceErrorBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.SOLANA_RENT_EXEMPTION
import one.mixin.android.web3.SOLANA_TOKEN_ACCOUNT_RENT_EXEMPTION
import one.mixin.android.web3.SolanaRecipientAccountState
import one.mixin.android.web3.isNativeSolAsset
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.web3.requiredSolBalance
import one.mixin.android.web3.solanaRecipientAccountState
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class TradeFragment : BaseFragment() {
    private data class FeeCheckResult(
        val isSufficient: Boolean,
        val swapPreviewData: SwapTransferPreviewData? = null,
    )

    private data class OnChainPreviewEstimate(
        val fee: BigDecimal?,
        val swapPreviewData: SwapTransferPreviewData? = null,
    )

    companion object {
        const val TAG = "SwapFragment"
        const val ARGS_WEB3_TOKENS = "args_web3_tokens"
        const val ARGS_INPUT = "args_input"
        const val ARGS_OUTPUT = "args_output"
        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_IN_MIXIN = "args_in_mixin"
        const val ARGS_REFERRAL = "args_referral"
        const val ARGS_ENTRY_SOURCE = "args_entry_source"
        const val ARGS_ENTRY_TYPE = "args_entry_type"

        const val ARGS_WALLET_ID = "args_wallet_id"

        const val MaxSlippage = 5000
        const val DangerousSlippage = 500
        const val MinSlippage = 10
        const val DefaultSlippage = 100

        const val maxLeftAmount = 0.01

        const val PREF_TRADE_SELECTED_TAB_PREFIX: String = "pref_trade_selected_tab_"
        const val PREF_TRADE_SPOT_GUIDE_SHOWN: String = "pref_trade_spot_guide_shown"
        const val PREF_TRADE_PERPETUAL_GUIDE_SHOWN: String = "pref_trade_perpetual_guide_shown"

        const val TAB_SIMPLE = 0
        const val TAB_ADVANCED = 1
        const val TAB_PERPETUAL = 2

        private const val RECOMMENDED_MARKET_LIMIT = 8
        private const val MARKET_SORT_MARKET_CAP_DESC = "market_cap_desc"
        private const val MARKET_CATEGORY_TOP_GAINERS = "top_gainers"
        private const val MARKET_CATEGORY_TOP_LOSERS = "top_losers"

        inline fun <reified T : Swappable> newInstance(
            input: String? = null,
            output: String? = null,
            amount: String? = null,
            inMixin: Boolean = true,
            referral: String? = null,
            walletId: String? = null,
            entrySource: String? = null,
            entryType: String? = null,
        ): TradeFragment =
            TradeFragment().withArgs {
                input?.let { putString(ARGS_INPUT, it) }
                output?.let { putString(ARGS_OUTPUT, it) }
                amount?.let { putString(ARGS_AMOUNT, it) }
                putBoolean(ARGS_IN_MIXIN, inMixin)
                referral?.let { putString(ARGS_REFERRAL, it) }
                walletId?.let { putString(ARGS_WALLET_ID, it) }
                entrySource?.let { putString(ARGS_ENTRY_SOURCE, it) }
                entryType?.let { putString(ARGS_ENTRY_TYPE, it) }
            }
    }

    enum class TradeDestination {
        Swap,
    }

    @Inject
    lateinit var rpc: Rpc

    private var swapTokens: List<SwapToken> by mutableStateOf(emptyList())
    private var remoteSwapTokens: List<SwapToken> by mutableStateOf(emptyList())
    private var stocks: List<SwapToken> by mutableStateOf(emptyList())
    private var trendingMarkets: List<MarketItem> by mutableStateOf(emptyList())
    private var topGainerMarkets: List<MarketItem> by mutableStateOf(emptyList())
    private var topLoserMarkets: List<MarketItem> by mutableStateOf(emptyList())
    private var tokenItems: List<TokenItem>? = null
    private var web3tokens: List<Web3TokenItem>? = null
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)
    private var limitFromToken: SwapToken? by mutableStateOf(null)
    private var limitToToken: SwapToken? by mutableStateOf(null)

    private var initialAmount: String? = null
    private var lastOrderTime: Long by mutableLongStateOf(0)
    private var reviewing: Boolean by mutableStateOf(false)
    private val walletId: String? by lazy { arguments?.getString(ARGS_WALLET_ID) }
    private var refreshJob: Job? = null

    @Inject
    lateinit var jobManager: MixinJobManager

    private val swapViewModel by viewModels<SwapViewModel>()
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val coroutineErrorHandler = CoroutineExceptionHandler { _, error ->
        Timber.e(error)
        ErrorHandler.handleError(error)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderBadge = defaultSharedPreferences.getInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 1
    }

    private var orderBadge: Boolean by mutableStateOf(false)

    private fun limitOrderBadgeDismissedPrefKey(walletId: String): String {
        return "${Account.PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED}_$walletId"
    }

    private fun perpetualBadgeDismissedPrefKey(walletId: String): String {
        return "${Account.PREF_TRADE_PERPETUAL_BADGE_DISMISSED}_$walletId"
    }

    private fun perpetualOrderBadgeDismissedPrefKey(walletId: String): String {
        return "${Account.PREF_TRADE_PERPETUAL_ORDER_BADGE_DISMISSED}_$walletId"
    }

    @FlowPreview
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        initAmount()
        lifecycleScope.launch(coroutineErrorHandler) {
            val chainIds = walletId?.let { it ->
                swapViewModel.getAddresses(it).map {
                    it.chainId
                }
            }
            initFromTo()
            refreshTokens(chainIds)
            refreshStocks(chainIds)
            refreshRecommendedMarkets()
        }
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = TradeDestination.Swap.name,
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
                        composable(TradeDestination.Swap.name) {
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    when (event) {
                                        Lifecycle.Event.ON_RESUME -> {
                                            startOrdersPolling()
                                        }

                                        Lifecycle.Event.ON_PAUSE -> {
                                            stopOrdersPolling()
                                        }

                                        else -> {}
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                }
                            }
                            
                            val currentWalletId = walletId ?: Session.getAccountId() ?: ""
                            val limitBadgePrefKey = remember(currentWalletId) {
                                limitOrderBadgeDismissedPrefKey(currentWalletId)
                            }
                            val perpetualBadgePrefKey = remember(currentWalletId) {
                                perpetualBadgeDismissedPrefKey(currentWalletId)
                            }
                            val perpetualOrderBadgePrefKey = remember(currentWalletId) {
                                perpetualOrderBadgeDismissedPrefKey(currentWalletId)
                            }
                            val initialTabIndex = remember(currentWalletId) {
                                getInitialTabIndex(currentWalletId)
                            }
                            var isLimitOrderTabBadgeDismissed by remember(currentWalletId) {
                                mutableStateOf(defaultSharedPreferences.getBoolean(limitBadgePrefKey, false))
                            }
                            var isPerpetualTabBadgeDismissed by remember(currentWalletId) {
                                mutableStateOf(defaultSharedPreferences.getBoolean(perpetualBadgePrefKey, false))
                            }
                            var isPerpetualOrderBadgeDismissed by remember(currentWalletId) {
                                mutableStateOf(defaultSharedPreferences.getBoolean(perpetualOrderBadgePrefKey, false))
                            }

                            TradePage(
                                walletId = walletId,
                                swapFrom = fromToken,
                                swapTo = toToken,
                                limitFrom = limitFromToken,
                                limitTo = limitToToken,
                                inMixin = inMixin(),
                                orderBadge = orderBadge,
                                isLimitOrderTabBadgeDismissed = isLimitOrderTabBadgeDismissed,
                                isPerpetualTabBadgeDismissed = isPerpetualTabBadgeDismissed,
                                isPerpetualOrderBadgeDismissed = isPerpetualOrderBadgeDismissed,
                                initialAmount = initialAmount,
                                lastOrderTime = lastOrderTime,
                                reviewing = reviewing,
                            initialTabIndex = initialTabIndex,
                            source = getSource(),
                            entrySource = getEntrySource(),
                            trendingMarkets = trendingMarkets,
                            stockTokens = stocks,
                            topGainerMarkets = topGainerMarkets,
                            topLoserMarkets = topLoserMarkets,
                            onSelectToken = { isReverse, type, isLimit ->
                                    if ((type == SelectTokenType.From && !isReverse) || (type == SelectTokenType.To && isReverse)) {
                                        selectCallback(swapTokens, isReverse, type, isLimit)
                                    } else {
                                        selectCallback(swapTokens, isReverse, type, isLimit)
                                    }
                                },
                                onDismissLimitOrderTabBadge = {
                                    if (!isLimitOrderTabBadgeDismissed) {
                                        isLimitOrderTabBadgeDismissed = true
                                        defaultSharedPreferences.putBoolean(limitBadgePrefKey, true)
                                    }
                                },
                                onDismissPerpetualTabBadge = {
                                    if (!isPerpetualTabBadgeDismissed) {
                                        isPerpetualTabBadgeDismissed = true
                                        defaultSharedPreferences.putBoolean(perpetualBadgePrefKey, true)
                                    }
                                },
                                onDismissPerpetualOrderBadge = {
                                    if (!isPerpetualOrderBadgeDismissed) {
                                        isPerpetualOrderBadgeDismissed = true
                                        defaultSharedPreferences.putBoolean(perpetualOrderBadgePrefKey, true)
                                    }
                                },
                                onTabChanged = { index ->
                                    val preferenceKey = "$PREF_TRADE_SELECTED_TAB_PREFIX$currentWalletId"
                                    defaultSharedPreferences.putInt(preferenceKey, index)
                                },
                                onSwitchToLimitOrder = { inputText, from, to ->
                                    initialAmount = inputText
                                    limitFromToken = from
                                    limitToToken = to
                                },
                                onReview = { quote, from, to, amount ->
                                    AnalyticsTracker.trackTradePreview()
                                    AnalyticsTracker.trackSpotPreview(
                                        sendChain = from.chain.name,
                                        sendAssetSymbol = from.symbol,
                                        receiveChain = to.chain.name,
                                        receiveAssetSymbol = to.symbol,
                                    )
                                    this@apply.hideKeyboard()
                                    reviewing = true
                                    lifecycleScope.launch {
                                        runCatching {
                                            handleReview(quote, from, to, amount, navController)
                                        }.onFailure {
                                            reviewing = false
                                            toast(ErrorHandler.getErrorMessage(it))
                                        }
                                    }
                                },
                                onLimitReview = { from, to, order ->
                                    AnalyticsTracker.trackTradePreview()
                                    AnalyticsTracker.trackSpotPreview(
                                        sendChain = from.chain.name,
                                        sendAssetSymbol = from.symbol,
                                        receiveChain = to.chain.name,
                                        receiveAssetSymbol = to.symbol,
                                    )
                                    this@apply.hideKeyboard()
                                    reviewing = true
                                    lifecycleScope.launch {
                                        runCatching {
                                            openLimitTransfer(from, to, order)
                                        }.onFailure {
                                            reviewing = false
                                            toast(ErrorHandler.getErrorMessage(it))
                                        }
                                    }
                                },
                                onDeposit = { token ->
                                    hideKeyboard()
                                    if (inMixin()) {
                                        deposit(token.assetId)
                                    } else {
                                        this@TradeFragment.lifecycleScope.launch(coroutineErrorHandler) {
                                            val t = swapViewModel.getTokenByWalletAndAssetId(Web3Signer.currentWalletId, token.assetId) ?: return@launch
                                            val address = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, token.chain.chainId)
                                            if (address == null) {
                                                toast(R.string.Alert_Not_Support)
                                                return@launch
                                            }
                                            navTo(Web3AddressFragment.newInstance(t, address.destination, true), Web3AddressFragment.TAG)
                                        }
                                    }
                                },
                                onOrderList = { currentWalletId, filterPending, spotType ->
                                    this@apply.hideKeyboard()
                                    val target = AllOrdersFragment.newInstanceWithWalletIds(
                                        walletIds = arrayListOf(currentWalletId),
                                        filterPending = filterPending,
                                        spotType = spotType,
                                    )
                                    if (defaultSharedPreferences.getInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 1) {
                                        defaultSharedPreferences.putInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, 1)
                                        orderBadge = false
                                        RxBus.publish(BadgeEvent(Account.PREF_HAS_USED_SWAP_TRANSACTION))
                                    }
                                    navTo(target, AllOrdersFragment.TAG)
                                },
                                onLimitOrderClick = { orderId ->
                                    this@apply.hideKeyboard()
                                    navTo(
                                        OrderDetailFragment.newInstance(
                                            orderId = orderId,
                                            spotType = AnalyticsTracker.SpotTradeType.ADVANCED,
                                        ),
                                        OrderDetailFragment.TAG
                                    )
                                },
                                onShowTradingGuideIfNeeded = { tabIndex ->
                                    this@apply.hideKeyboard()
                                    when {
                                        walletId == null && tabIndex >= SpotTradeGuideBottomSheetDialogFragment.TAB_LIMIT -> {
                                            if (!defaultSharedPreferences.getBoolean(PREF_TRADE_PERPETUAL_GUIDE_SHOWN, false)) {
                                                isPerpetualTabBadgeDismissed = true
                                                defaultSharedPreferences.putBoolean(perpetualBadgePrefKey, true)
                                                AnalyticsTracker.trackPerpsGuide(AnalyticsTracker.PerpsSource.FIRST_GUIDE)
                                                PerpetualGuideBottomSheetDialogFragment.newInstance()
                                                    .show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                                            }
                                        }
                                        tabIndex == 1 || tabIndex == 0 -> {
                                            if (!defaultSharedPreferences.getBoolean(PREF_TRADE_SPOT_GUIDE_SHOWN, false)) {
                                                AnalyticsTracker.trackSpotGuide(
                                                    currentSpotType(tabIndex),
                                                    AnalyticsTracker.SpotGuideSource.FIRST_GUIDE,
                                                )
                                                val initialGuideTab = if (tabIndex == 1) {
                                                    SpotTradeGuideBottomSheetDialogFragment.TAB_LIMIT
                                                } else {
                                                    SpotTradeGuideBottomSheetDialogFragment.TAB_SWAP
                                                }
                                                SpotTradeGuideBottomSheetDialogFragment.newInstance(initialGuideTab)
                                                    .show(parentFragmentManager, SpotTradeGuideBottomSheetDialogFragment.TAG)
                                            }
                                        }
                                    }
                                },
                                onShowTradingGuide = { tabIndex ->
                                    this@apply.hideKeyboard()
                                    when {
                                        walletId == null && tabIndex >= SpotTradeGuideBottomSheetDialogFragment.TAB_LIMIT -> {
                                            AnalyticsTracker.trackPerpsGuide(AnalyticsTracker.PerpsSource.PERPS_HOME_MENU)
                                            PerpetualGuideBottomSheetDialogFragment.newInstance()
                                                .show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                                        }
                                        tabIndex == 1 -> {
                                            AnalyticsTracker.trackSpotGuide(
                                                AnalyticsTracker.SpotTradeType.ADVANCED,
                                                AnalyticsTracker.SpotGuideSource.MENU,
                                            )
                                            SpotTradeGuideBottomSheetDialogFragment.newInstance(
                                                SpotTradeGuideBottomSheetDialogFragment.TAB_LIMIT
                                            ).show(parentFragmentManager, SpotTradeGuideBottomSheetDialogFragment.TAG)
                                        }
                                        tabIndex == 0 -> {
                                            AnalyticsTracker.trackSpotGuide(
                                                AnalyticsTracker.SpotTradeType.SIMPLE,
                                                AnalyticsTracker.SpotGuideSource.MENU,
                                            )
                                            SpotTradeGuideBottomSheetDialogFragment.newInstance(
                                                SpotTradeGuideBottomSheetDialogFragment.TAB_SWAP
                                            ).show(parentFragmentManager, SpotTradeGuideBottomSheetDialogFragment.TAG)
                                        }
                                        else -> {
                                            SpotTradeGuideBottomSheetDialogFragment.newInstance(
                                                SpotTradeGuideBottomSheetDialogFragment.TAB_OVERVIEW
                                            )
                                                .show(parentFragmentManager, SpotTradeGuideBottomSheetDialogFragment.TAG)
                                        }
                                    }
                                },
                                onShowHelpBottomSheet = { onContactSupport, onTradingGuide ->
                                    this@apply.hideKeyboard()
                                    TradeHelpBottomSheetDialogFragment.newInstance().apply {
                                        this.onContactSupport = onContactSupport
                                        this.onTradingGuide = onTradingGuide
                                    }.show(parentFragmentManager, TradeHelpBottomSheetDialogFragment.TAG)
                                },
                            pop = {
                                navigateUp(navController)
                            },
                            onRecommendedMarketClick = { marketItem ->
                                showMarketDetails(marketItem)
                            },
                            onRecommendedStockClick = { token ->
                                showStockMarketDetails(token)
                            },
                            onRecommendedMarketViewAllClick = {
                                requireContext().openMarket()
                            },
                            onShowMarketList = { isLong ->
                                    PerpsMarketListBottomSheetDialogFragment.newInstance(isLong).show(parentFragmentManager, PerpsMarketListBottomSheetDialogFragment.TAG)
                                },
                                onShowAllMarkets = { initialCategory, initialSort ->
                                    PerpsMarketListBottomSheetDialogFragment.newInstance(initialCategory, initialSort).show(parentFragmentManager, PerpsMarketListBottomSheetDialogFragment.TAG)
                                },
                                onShowAllOpenPositions = {
                                    navTo(AllPositionsFragment.newOpenInstance(), AllPositionsFragment.TAG)
                                },
                                onShowAllClosedPositions = {
                                    navTo(AllPositionsFragment.newClosedInstance(), AllPositionsFragment.TAG)
                                },
                                onOpenPositionClick = { position ->
                                    navTo(
                                        PositionDetailFragment.newInstance(
                                            position,
                                            AnalyticsTracker.PerpsSource.PERPS_HOME_LIST,
                                        ),
                                        PositionDetailFragment.TAG,
                                    )
                                },
                                onMarketItemClick = { market ->
                                    PerpsActivity.showDetail(
                                        requireContext(),
                                        market.marketId,
                                        market.displaySymbol,
                                        market.displaySymbol,
                                        market.tokenSymbol,
                                        AnalyticsTracker.PerpsSource.MORE_EXPLORE,
                                    )
                                },
                                onClosedPositionClick = { position ->
                                    navTo(
                                        PositionDetailFragment.newInstance(
                                            position,
                                            AnalyticsTracker.PerpsSource.PERPS_HOME_LIST,
                                        ),
                                        PositionDetailFragment.TAG,
                                    )
                                }
                            )
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
        isLimit: Boolean,
    ) {
        if ((type == SelectTokenType.From && !isReverse) || (type == SelectTokenType.To && isReverse)) {
            val targetPref = if (isLimit) {
                if (inMixin()) Account.PREF_FROM_LIMIT_SWAP else Account.PREF_FROM_WEB3_LIMIT_SWAP
            } else {
                if (inMixin()) Account.PREF_FROM_SWAP else Account.PREF_FROM_WEB3_SWAP
            }
            if (inMixin()) {
                SwapTokenListBottomSheetDialogFragment.newInstance(
                    targetPref,
                    ArrayList(list), stocks, if (isReverse) (if (isLimit) limitToToken?.assetId else toToken?.assetId) else (if (isLimit) limitFromToken?.assetId else fromToken?.assetId),
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
                        saveQuoteToken(t, isReverse, type, isLimit)
                        requireContext().defaultSharedPreferences.addToList(targetPref, t, SwapToken::class.java)
                        dismissNow()
                    }
                }.show(parentFragmentManager, SwapTokenListBottomSheetDialogFragment.TAG)
            } else {
                SwapTokenListBottomSheetDialogFragment.newInstance(
                    targetPref,
                    ArrayList(
                        list,
                    ),
                    stocks,
                    isFrom = true,
                ).apply {
                    setOnDeposit {
                        this@TradeFragment.lifecycleScope.launch(coroutineErrorHandler) {
                            val t = swapViewModel.getTokenByWalletAndAssetId(
                                Web3Signer.currentWalletId, if (Web3Signer.evmAddress.isBlank()) {
                                    Constants.ChainId.SOLANA_CHAIN_ID
                                } else {
                                    Constants.ChainId.ETHEREUM_CHAIN_ID
                                }) ?: return@launch
                            val address =
                                when (t.chainId) {
                                    Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
                                    Constants.ChainId.BITCOIN_CHAIN_ID -> Web3Signer.btcAddress
                                    else -> Web3Signer.evmAddress
                                }
                            navTo(Web3AddressFragment.newInstance(t, address), Web3AddressFragment.TAG)
                            dismissNow()
                        }
                    }
                    setOnClickListener { token, alert ->
                        if (alert) {
                            SwapTokenBottomSheetDialogFragment.newInstance(token).showNow(parentFragmentManager, SwapTokenBottomSheetDialogFragment.TAG)
                            return@setOnClickListener
                        }
                        requireContext().defaultSharedPreferences.addToList(targetPref, token, SwapToken::class.java)
                        saveQuoteToken(token, isReverse, type, isLimit)
                        dismissNow()
                    }
                }.show(parentFragmentManager, SwapTokenListBottomSheetDialogFragment.TAG)
            }
        } else {
            val targetPref = if (isLimit) {
                if (inMixin()) Account.PREF_TO_LIMIT_SWAP else Account.PREF_TO_WEB3_LIMIT_SWAP
            } else {
                if (inMixin()) Account.PREF_TO_SWAP else Account.PREF_TO_WEB3_SWAP
            }
            SwapTokenListBottomSheetDialogFragment.newInstance(
                targetPref,
                tokens =
                    ArrayList(
                        list.run {
                            this
                        },
                    )
                ,
                stocks,
                if (inMixin()) {
                    if (isReverse) (if (isLimit) limitFromToken?.assetId else fromToken?.assetId) else (if (isLimit) limitToToken?.assetId else toToken?.assetId)
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
                    requireContext().defaultSharedPreferences.addToList(targetPref, token, SwapToken::class.java)
                    saveQuoteToken(token, isReverse, type, isLimit)
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
        lifecycleScope.launch(coroutineErrorHandler) {
            val token = swapViewModel.findToken(tokenId)
            if (token == null) {
                dialog.show()
                runCatching {
                    swapViewModel.checkAndSyncTokens(listOf(tokenId))
                    val t = swapViewModel.findToken(tokenId)
                    if (t != null)
                        navTo(DepositFragment.newInstance(t, hideNetworkSwitch = true), DepositFragment.TAG)
                    else
                        toast(R.string.Not_found)
                }.onFailure {
                    ErrorHandler.handleError(it)
                }.getOrNull()

                dialog.dismiss()
            } else {
                runCatching {
                    navTo(DepositFragment.newInstance(token, hideNetworkSwitch = true), DepositFragment.TAG)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }


    private fun saveQuoteToken(
        token: SwapToken,
        isReverse: Boolean,
        type: SelectTokenType,
        isLimit: Boolean,
    ) {
        if (isLimit) {
            if (type == SelectTokenType.From) {
                if (token == limitToToken) {
                    limitToToken = limitFromToken
                }
                limitFromToken = token
            } else {
                if (token == limitFromToken) {
                    limitFromToken = limitToToken
                }
                limitToToken = token
            }

            limitFromToken?.let { from ->
                limitToToken?.let { to ->
                    val tokenPair = if (isReverse) listOf(to, from) else listOf(from, to)
                    val serializedPair = GsonHelper.customGson.toJson(tokenPair)
                    defaultSharedPreferences.putString(getPreferenceKey(true), serializedPair)
                }
            }
        } else {
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
                    defaultSharedPreferences.putString(getPreferenceKey(false), serializedPair)
                }
            }
        }
    }

    private suspend fun handleReview(quote: QuoteResult, from: SwapToken, to: SwapToken, amount: String, navController: NavHostController) {
        val inputMint = from.assetId
        val outputMint = to.assetId
        if (!inMixin()) {
            val address = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, to.chain.chainId)
            if (address == null){
                reviewing = false
                toast(R.string.Alert_Not_Support)
                return
            }
            val fromAddress = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, from.chain.chainId)
            if (fromAddress == null){
                reviewing = false
                toast(R.string.Alert_Not_Support)
                return
            }
        }
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
                            when (to.chain.chainId) {
                                Constants.ChainId.SOLANA_CHAIN_ID -> {
                                    Web3Signer.solanaAddress
                                }
                                Constants.ChainId.BITCOIN_CHAIN_ID -> {
                                    Web3Signer.btcAddress
                                }
                                in Constants.Web3EvmChainIds -> {
                                    Web3Signer.evmAddress
                                }
                                else -> {
                                    null
                                }
                            }
                        },
                        getReferral(),
                        walletId,
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
        if (resp == null) {
            reviewing = false
            return
        }
        val feeCheckResult = ensureWeb3FeeSufficient(
            from = from,
            destination = resp.depositDestination,
            amount = quote.inAmount,
            allowGasless = true,
            includeSwapPreviewData = true,
        )
        if (!feeCheckResult.isSufficient) {
            reviewing = false
            return
        }
        openSwapTransfer(resp, from, to, feeCheckResult.swapPreviewData)
    }

    private fun openSwapTransfer(
        swapResult: SwapResponse,
        from: SwapToken,
        to: SwapToken,
        previewData: SwapTransferPreviewData?,
    ) {
        SwapTransferBottomSheetDialogFragment.newInstance(swapResult, from, to, previewData).apply {
            setOnDone {
                initialAmount = null
                lastOrderTime = System.currentTimeMillis()
            }
            setOnDestroy {
                reviewing = false
            }
        }.showNow(parentFragmentManager, SwapTransferBottomSheetDialogFragment.TAG)
    }

    private suspend fun openLimitTransfer(from: SwapToken, to: SwapToken, order: CreateLimitOrderResponse) {
        val senderWalletId = if (inMixin()) Session.getAccountId()!! else Web3Signer.currentWalletId
        if (!inMixin()) {
            val address = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, to.chain.chainId)
            if (address == null){
                reviewing = false
                toast(R.string.Alert_Not_Support)
                return
            }
            val fromAddress = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, from.chain.chainId)
            if (fromAddress == null){
                reviewing = false
                toast(R.string.Alert_Not_Support)
                return
            }
        }
        val feeCheckResult = ensureWeb3FeeSufficient(
                from = from,
                destination = order.depositDestination,
                amount = order.order.payAmount,
                allowGasless = true,
                includeSwapPreviewData = true,
            )
        if (!feeCheckResult.isSufficient) {
            reviewing = false
            return
        }
        LimitTransferBottomSheetDialogFragment.newInstance(order, from, to, senderWalletId, feeCheckResult.swapPreviewData).apply {
            setOnDone {
                initialAmount = null
                lastOrderTime = System.currentTimeMillis()
            }
            setOnDestroy {
                reviewing = false
            }
        }.showNow(parentFragmentManager, LimitTransferBottomSheetDialogFragment.TAG)
    }

    private suspend fun ensureWeb3FeeSufficient(
        from: SwapToken,
        destination: String?,
        amount: String,
        allowGasless: Boolean,
        includeSwapPreviewData: Boolean = false,
    ): FeeCheckResult {
        if (inMixin()) return FeeCheckResult(true)
        val walletId = Web3Signer.currentWalletId
        val token = swapViewModel.getTokenByWalletAndAssetId(walletId, from.assetId) ?: return FeeCheckResult(true)
        val toAddress = destination ?: return FeeCheckResult(true)
        val fromAddress = when (token.chainId) {
            Constants.ChainId.SOLANA_CHAIN_ID -> Web3Signer.solanaAddress
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val btcAddress = swapViewModel.getAddressesByChainId(walletId, Constants.ChainId.BITCOIN_CHAIN_ID)?.destination ?: return FeeCheckResult(true)
                btcAddress
            }
            else -> Web3Signer.evmAddress
        }
        val chainToken = swapViewModel.web3TokenItemById(walletId, token.chainId)
            ?: return FeeCheckResult(true)
        val chainBalance = chainToken.balance.toBigDecimalOrNull()
            ?: return FeeCheckResult(true)
        val transferAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val sameAssetFee = token.assetId == chainToken.assetId && token.chainId == chainToken.chainId
        val usesSolRentRule = chainToken.isNativeSolAsset()
        val solanaRecipientAccountState = if (token.chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
            runCatching {
                token.solanaRecipientAccountState(rpc, toAddress)
            }.getOrDefault(SolanaRecipientAccountState.EXISTS)
        } else {
            SolanaRecipientAccountState.EXISTS
        }
        val extraSolReserve = if (solanaRecipientAccountState == SolanaRecipientAccountState.NEEDS_TOKEN_ACCOUNT) {
            SOLANA_TOKEN_ACCOUNT_RENT_EXEMPTION
        } else {
            BigDecimal.ZERO
        }

        if (allowGasless && token.chainId != Constants.ChainId.BITCOIN_CHAIN_ID) {
            val gaslessPrepared = runCatching {
                web3ViewModel.gaslessPrepare(
                    GaslessTxRequest(
                        from = fromAddress,
                        to = toAddress,
                        assetId = token.assetId,
                        amount = amount,
                        feeAssetId = token.assetId,
                        feeAmount = null,
                        chainId = token.chainId,
                    )
                )
            }.getOrNull()?.data
            if (gaslessPrepared != null) {
                val previewData = if (includeSwapPreviewData) {
                    buildGaslessSwapPreviewData(
                        senderAddress = fromAddress,
                        token = token,
                        feeAmount = BigDecimal.ZERO.toPlainString(),
                        gaslessPrepared = gaslessPrepared,
                    )
                } else {
                    null
                }
                return FeeCheckResult(true, previewData)
            }
        }

        val estimate = estimateOnChainPreview(
            token = token,
            chainToken = chainToken,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amount = amount,
            includeSwapPreviewData = includeSwapPreviewData,
        )
        val fee = estimate.fee
        if (fee == null) {
            val fallbackRequiredBalance = if (usesSolRentRule) {
                requiredSolBalance(
                    transferAmount = transferAmount,
                    solFee = extraSolReserve,
                    sendingNativeSol = token.isNativeSolAsset(),
                )
            } else if (sameAssetFee) {
                transferAmount
            } else {
                BigDecimal.ZERO
            }
            val insufficientByFullBalanceFallback =
                if (usesSolRentRule) {
                    fallbackRequiredBalance >= chainBalance
                } else {
                    sameAssetFee && fallbackRequiredBalance >= chainBalance
                }
            if (!insufficientByFullBalanceFallback) {
                return FeeCheckResult(true)
            }
            val effectiveFee = if (usesSolRentRule) SOLANA_RENT_EXEMPTION + extraSolReserve else BigDecimal.ZERO
            TransferWeb3BalanceErrorBottomSheetDialogFragment.newInstance(
                Web3TokenFeeItem(
                    token = chainToken,
                    amount = if (token.isNativeSolAsset()) transferAmount else BigDecimal.ZERO,
                    fee = effectiveFee,
                )
            ).showNow(parentFragmentManager, TransferWeb3BalanceErrorBottomSheetDialogFragment.TAG)
            return FeeCheckResult(false)
        }
        val effectiveFee = if (usesSolRentRule) fee + SOLANA_RENT_EXEMPTION + extraSolReserve else fee
        val requiredBalance = if (usesSolRentRule) {
            requiredSolBalance(
                transferAmount = transferAmount,
                solFee = fee + extraSolReserve,
                sendingNativeSol = token.isNativeSolAsset(),
            )
        } else if (sameAssetFee) {
            transferAmount + fee
        } else {
            fee
        }

        if (requiredBalance <= chainBalance) {
            return FeeCheckResult(true, estimate.swapPreviewData)
        }

        TransferWeb3BalanceErrorBottomSheetDialogFragment.newInstance(
            Web3TokenFeeItem(
                token = chainToken,
                amount = if (token.isNativeSolAsset()) transferAmount else BigDecimal.ZERO,
                fee = effectiveFee,
            )
        ).showNow(parentFragmentManager, TransferWeb3BalanceErrorBottomSheetDialogFragment.TAG)
        return FeeCheckResult(false)
    }

    private suspend fun estimateOnChainPreview(
        token: Web3TokenItem,
        chainToken: Web3TokenItem,
        fromAddress: String,
        toAddress: String,
        amount: String,
        includeSwapPreviewData: Boolean,
    ): OnChainPreviewEstimate {
        if (!includeSwapPreviewData) {
            return OnChainPreviewEstimate(
                fee = estimateWeb3Fee(token, fromAddress, toAddress, amount),
            )
        }

        return when (token.chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val localUtxos = web3ViewModel.outputsByAddress(fromAddress, Constants.ChainId.BITCOIN_CHAIN_ID)
                val zeroFeeTx = token.buildTransaction(
                    rpc = rpc,
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    v = amount,
                    localUtxos = localUtxos,
                    rate = BigDecimal.ONE,
                )
                val estimatedFee = web3ViewModel.calcFee(token, zeroFeeTx, fromAddress)
                val fee = estimatedFee.fee
                val transaction = if (fee == null) {
                    null
                } else {
                    token.buildTransaction(
                        rpc = rpc,
                        fromAddress = fromAddress,
                        toAddress = toAddress,
                        v = amount,
                        localUtxos = localUtxos,
                        rate = estimatedFee.rate,
                        minFee = estimatedFee.minFee,
                    )
                }
                OnChainPreviewEstimate(
                    fee = fee,
                    swapPreviewData = transaction?.let {
                        buildSwapPreviewData(
                            senderAddress = fromAddress,
                            web3Transaction = it,
                            fee = fee,
                            feeToken = chainToken,
                        )
                    },
                )
            }
            Constants.ChainId.SOLANA_CHAIN_ID -> {
                val transaction = token.buildTransaction(
                    rpc = rpc,
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    v = amount,
                )
                val fee = web3ViewModel.calcFee(token, transaction, fromAddress).fee
                OnChainPreviewEstimate(
                    fee = fee,
                    swapPreviewData = buildSwapPreviewData(
                        senderAddress = fromAddress,
                        web3Transaction = transaction,
                        fee = fee,
                        feeToken = chainToken,
                    ),
                )
            }
            else -> {
                val transaction = token.buildTransaction(
                    rpc = rpc,
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    v = amount,
                )
                val estimateResponse = runCatching {
                    web3ViewModel.estimateFee(
                        EstimateFeeRequest(
                            chainId = token.chainId,
                            rawTransaction = null,
                            data = transaction.data ?: transaction.wcEthereumTransaction?.data,
                            from = fromAddress,
                            to = transaction.wcEthereumTransaction?.to,
                            value = transaction.wcEthereumTransaction?.value,
                        )
                    )
                }.getOrNull()
                val tipGas = if (estimateResponse?.isSuccess == true && estimateResponse.data != null) {
                    buildTipGas(token.chainId, estimateResponse.data!!)
                } else {
                    null
                }
                val fee = tipGas?.displayValue(transaction.wcEthereumTransaction?.maxFeePerGas)
                OnChainPreviewEstimate(
                    fee = fee,
                    swapPreviewData = buildSwapPreviewData(
                        senderAddress = fromAddress,
                        web3Transaction = transaction,
                        fee = fee,
                        feeToken = chainToken,
                        tipGas = tipGas,
                    ),
                )
            }
        }
    }

    private fun buildGaslessSwapPreviewData(
        senderAddress: String,
        token: Web3TokenItem,
        feeAmount: String?,
        gaslessPrepared: GaslessTxResponse,
    ): SwapTransferPreviewData {
        val fee = feeAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return SwapTransferPreviewData(
            senderAddress = senderAddress,
            web3Transaction = JsSignMessage(
                callbackId = 0L,
                type = JsSignMessage.TYPE_GASLESS_TRANSFER,
            ),
            gaslessPrepareResponseJson = GsonHelper.customGson.toJson(gaslessPrepared),
            feeAmount = fee.stripTrailingZeros().toPlainString(),
            feeSymbol = token.symbol,
            feeUsd = formatFeeUsd(fee, token),
        )
    }

    private fun buildSwapPreviewData(
        senderAddress: String,
        web3Transaction: JsSignMessage,
        fee: BigDecimal?,
        feeToken: Web3TokenItem,
        tipGas: TipGas? = null,
    ): SwapTransferPreviewData {
        val feeValue = fee ?: BigDecimal.ZERO
        return SwapTransferPreviewData(
            senderAddress = senderAddress,
            web3Transaction = web3Transaction,
            feeAmount = feeValue.stripTrailingZeros().toPlainString(),
            feeSymbol = feeToken.symbol,
            feeUsd = formatFeeUsd(feeValue, feeToken),
            gasPrice = tipGas?.displayGas(web3Transaction.wcEthereumTransaction?.maxFeePerGas)?.toPlainString(),
            tipGasLimit = tipGas?.gasLimit?.toString(),
            tipGasMaxFeePerGas = tipGas?.maxFeePerGas?.toString(),
            tipGasMaxPriorityFeePerGas = tipGas?.maxPriorityFeePerGas?.toString(),
        )
    }

    private fun formatFeeUsd(
        fee: BigDecimal,
        feeToken: Web3TokenItem,
    ): String {
        val price = feeToken.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return fee.multiply(price).stripTrailingZeros().toPlainString()
    }

    private suspend fun estimateWeb3Fee(
        token: Web3TokenItem,
        fromAddress: String,
        toAddress: String,
        amount: String,
    ): BigDecimal? {
        return if (token.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
            val localUtxos = web3ViewModel.outputsByAddress(fromAddress, Constants.ChainId.BITCOIN_CHAIN_ID)
            val zeroFeeTx = token.buildTransaction(
                rpc = rpc,
                fromAddress = fromAddress,
                toAddress = toAddress,
                v = amount,
                localUtxos = localUtxos,
                rate = BigDecimal.ONE,
            )
            web3ViewModel.calcFee(token, zeroFeeTx, fromAddress).fee
        } else {
            val transaction = token.buildTransaction(
                rpc = rpc,
                fromAddress = fromAddress,
                toAddress = toAddress,
                v = amount,
            )
            web3ViewModel.calcFee(token, transaction, fromAddress).fee
        }
    }

    private suspend fun initFromTo() {
        var swappable = web3tokens ?: tokenItems
        if (!inMixin() && web3tokens.isNullOrEmpty()) {
            if (walletId == null) {
                toast(R.string.Data_error)
                return
            }
            swappable = swapViewModel.findWeb3AssetItemsWithBalance(walletId!!)
            if (swappable.isEmpty()) {
                swappable = swapViewModel.findWeb3AssetItems(walletId!!)
            }
            web3tokens = swappable
        } else if (swappable.isNullOrEmpty()) {
            swappable = swapViewModel.findAssetItemsWithBalance()
            if (swappable.isEmpty()) {
                swappable = swapViewModel.findAssetItems()
            }
            tokenItems = swappable
        }
        swappable.map { it.toSwapToken() }.toList().let {
            swapTokens = it.sortByKeywordAndBalance()
        }
        initTokenPair(swapTokens, false)
        initTokenPair(swapTokens, true)
    }

    private suspend fun initTokenPair(tokens: List<SwapToken>, isLimit: Boolean) {
        val input = requireArguments().getString(ARGS_INPUT)
        val output = requireArguments().getString(ARGS_OUTPUT)
        val lastSelectedPairJson = defaultSharedPreferences.getString(getPreferenceKey(isLimit), null)
        val lastSelectedPair: List<SwapToken>? = lastSelectedPairJson?.let {
            val type = object : TypeToken<List<SwapToken>>() {}.type
            GsonHelper.customGson.fromJson(it, type)
        }
        val lastFrom = lastSelectedPair?.getOrNull(0)
        val lastTo = lastSelectedPair?.getOrNull(1)

        var tempFromToken = if (input != null) {
            if (inMixin()) swapViewModel.findToken(input)?.toSwapToken() else swapViewModel.web3TokenItemById(walletId!!, input)?.toSwapToken()
        } else if (lastFrom != null) {
            if (inMixin()) swapViewModel.findToken(lastFrom.assetId)?.toSwapToken() else swapViewModel.web3TokenItemById(walletId!!, lastFrom.assetId)?.toSwapToken()
        } else {
            tokens.firstOrNull { t -> t.getUnique() in Constants.usdIds }
        }
        var tempToToken = if (output != null) {
            if (inMixin()) swapViewModel.findToken(output)?.toSwapToken() else swapViewModel.web3TokenItemById(walletId!!, output)?.toSwapToken()
        } else if (input != null) {
            val o = if (input in Constants.usdIds) {
                XIN_ASSET_ID
            } else {
                USDT_ASSET_ETH_ID
            }
            if (inMixin()) swapViewModel.findToken(o)?.toSwapToken() else swapViewModel.web3TokenItemById(walletId!!, o)?.toSwapToken()
        } else if (lastTo != null) {
            if (inMixin()) swapViewModel.findToken(lastTo.assetId)?.toSwapToken() else swapViewModel.web3TokenItemById(walletId!!, lastTo.assetId)?.toSwapToken()
        } else {
            tokens.firstOrNull { t -> t.getUnique() != tempFromToken?.getUnique() && t.getUnique() in Constants.usdIds }
        }
        resolveDuplicateSwapTokenPair(
            tokens = tokens,
            fromToken = tempFromToken,
            toToken = tempToToken,
            keepToToken = output != null,
        ).let { pair ->
            tempFromToken = pair.from
            tempToToken = pair.to
        }

        if (isLimit) {
            limitFromToken = tempFromToken
            limitToToken = tempToToken
        } else {
            fromToken = tempFromToken
            toToken = tempToToken
        }
    }
    private fun showMarketDetails(marketItem: MarketItem) {
        WalletActivity.showWithMarket(
            requireActivity(),
            marketItem,
            Destination.Market,
            AnalyticsTracker.MarketSource.MORE_MARKET_CAP,
        )
    }

    private fun showStockMarketDetails(token: SwapToken) {
        lifecycleScope.launch(coroutineErrorHandler) {
            val marketItem = swapViewModel.checkMarketById(token.assetId, false)
            if (marketItem == null) {
                toast(R.string.Data_error)
            } else {
                showMarketDetails(marketItem)
            }
        }
    }

    private suspend fun refreshRecommendedMarkets() {
        refreshRecommendedMarket(
            sort = MARKET_SORT_MARKET_CAP_DESC,
            onSuccess = { trendingMarkets = it },
        )
        refreshRecommendedMarket(
            category = MARKET_CATEGORY_TOP_GAINERS,
            onSuccess = { topGainerMarkets = it },
        )
        refreshRecommendedMarket(
            category = MARKET_CATEGORY_TOP_LOSERS,
            onSuccess = { topLoserMarkets = it },
        )
    }

    private suspend fun refreshRecommendedMarket(
        category: String? = null,
        sort: String? = null,
        onSuccess: (List<MarketItem>) -> Unit,
    ) {
        requestRouteAPI(
            invokeNetwork = {
                swapViewModel.markets(
                    category = category,
                    limit = RECOMMENDED_MARKET_LIMIT,
                    sort = sort,
                )
            },
            successBlock = { resp ->
                resp.data?.map { MarketItem.fromMarket(it) }.orEmpty()
            },
            requestSession = { swapViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            failureBlock = { r ->
                if (r.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                    refreshRecommendedMarket(category, sort, onSuccess)
                    return@requestRouteAPI true
                }
                if (r.errorCode == ErrorHandler.OLD_VERSION) {
                    alertDialogBuilder()
                        .setTitle(R.string.Update_Mixin)
                        .setMessage(getString(R.string.update_mixin_description, requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)))
                        .setNegativeButton(R.string.Later) { dialog, _ ->
                            dialog.dismiss()
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }.setPositiveButton(R.string.Update) { dialog, _ ->
                            dialog.dismiss()
                            activity?.onBackPressedDispatcher?.onBackPressed()
                            context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                        }.setCancelable(false)
                        .create().show()

                    return@requestRouteAPI true
                }
                false
            },
        )?.let(onSuccess)
    }

    private suspend fun refreshStocks(chainIds: List<String>?) {
        val chainIdSet: Set<String>? = chainIds?.toSet()?.takeIf { it.isNotEmpty() }
        requestRouteAPI(
            invokeNetwork = { swapViewModel.web3Tokens(getSource(), category = "stock") },
            successBlock = { resp ->
                resp.data
            },
            requestSession = { swapViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            failureBlock = { r ->
                if (r.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                    refreshStocks(chainIds)
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
        )?.let { remote: List<SwapToken> ->
            val filteredRemote: List<SwapToken> = if (chainIdSet == null) {
                remote
            } else {
                remote.filter { token: SwapToken ->
                    chainIdSet.contains(token.chain.chainId)
                }
            }
            stocks = filteredRemote.map { it.copy(isWeb3 = !inMixin(), walletId = walletId) }.map { token ->
                val t = web3tokens?.firstOrNull { web3Token ->
                    (web3Token.assetKey == token.address && web3Token.assetId == token.assetId)
                } ?: return@map token
                token.balance = t.balance
                token
            }.sortByKeywordAndBalance()
            if (stocks.isNotEmpty()) {
                (parentFragmentManager.findFragmentByTag(SwapTokenListBottomSheetDialogFragment.TAG) as? SwapTokenListBottomSheetDialogFragment)?.setStocks(stocks)
            }
        }
    }
    private suspend fun refreshTokens(chainIds: List<String>?) {
        val chainIdSet: Set<String>? = chainIds?.toSet()?.takeIf { it.isNotEmpty() }
        requestRouteAPI(
            invokeNetwork = { swapViewModel.web3Tokens(getSource()) },
            successBlock = { resp ->
                resp.data
            },
            requestSession = { swapViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            failureBlock = { r ->
                if (r.errorCode == 401) {
                    swapViewModel.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                    refreshTokens(chainIds)
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
        )?.let { remote: List<SwapToken> ->
            val filteredRemote: List<SwapToken> = if (chainIdSet == null) {
                remote
            } else {
                remote.filter { token: SwapToken ->
                    chainIdSet.contains(token.chain.chainId)
                }
            }
            if (!inMixin()) {
                remoteSwapTokens = filteredRemote.map { it.copy(isWeb3 = true, walletId = walletId) }.mapNotNull { token ->
                    val local = swapViewModel.web3TokenItemById(walletId ?: "", token.assetId)
                    if (local != null) {
                        if (local.hidden == true) {
                            null
                        } else {
                            token.copy(balance = local.balance, price = local.priceUsd, level = local.level)
                        }
                    } else {
                        token
                    }
                }.sortByKeywordAndBalance()

                swapTokens = swapTokens.union(remoteSwapTokens).toList().sortByKeywordAndBalance()
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                }
                if (toToken == null || toToken?.getUnique() == fromToken?.getUnique()) {
                    toToken = swapTokens.firstOrNull { s -> s.assetId != fromToken?.assetId } ?: swapTokens.getOrNull(1) ?: swapTokens[0]
                }
                if (limitFromToken == null) {
                    limitFromToken = swapTokens.firstOrNull { t -> limitFromToken == t } ?: swapTokens[0]
                }
                if (limitToToken == null || limitToToken?.getUnique() == limitFromToken?.getUnique()) {
                    limitToToken = swapTokens.firstOrNull { s -> s.assetId != limitFromToken?.assetId } ?: swapTokens.getOrNull(1) ?: swapTokens[0]
                }
                if (swapTokens.isNotEmpty()) {
                    (parentFragmentManager.findFragmentByTag(SwapTokenListBottomSheetDialogFragment.TAG) as? SwapTokenListBottomSheetDialogFragment)?.setLoading(false, swapTokens, remoteSwapTokens)
                }
            } else {
                remoteSwapTokens = filteredRemote.mapNotNull { token ->
                    val local = swapViewModel.findToken(token.assetId)
                    if (local != null) {
                        if (local.hidden == true) {
                            null
                        } else {
                            token.copy(balance = local.balance, price = local.priceUsd)
                        }
                    } else {
                        token
                    }
                }.sortByKeywordAndBalance()
                swapTokens = swapTokens.union(remoteSwapTokens).toList().sortByKeywordAndBalance()
                if (fromToken == null) {
                    fromToken = swapTokens.firstOrNull { t -> fromToken == t } ?: swapTokens[0]
                }
                if (toToken == null || toToken?.getUnique() == fromToken?.getUnique()) {
                    toToken = swapTokens.firstOrNull { s -> s.assetId != fromToken?.assetId }
                }
                if (limitFromToken == null) {
                    limitFromToken = swapTokens.firstOrNull { t -> limitFromToken == t } ?: swapTokens[0]
                }
                if (limitToToken == null || limitToToken?.getUnique() == limitFromToken?.getUnique()) {
                    limitToToken = swapTokens.firstOrNull { s -> s.assetId != limitFromToken?.assetId }
                }
            }
            if (swapTokens.isNotEmpty()) {
                (parentFragmentManager.findFragmentByTag(SwapTokenListBottomSheetDialogFragment.TAG) as? SwapTokenListBottomSheetDialogFragment)?.setLoading(false, swapTokens, remoteSwapTokens)
            }
            if (fromToken != null && toToken != null) {
                refreshTokensPrice(listOfNotNull(fromToken, toToken, limitFromToken, limitToToken).distinct())
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

    private fun getPreferenceKey(isLimit: Boolean): String {
        return if (isLimit) {
            if (inMixin()) PREF_LIMIT_SWAP_LAST_PAIR else "$PREF_WEB3_LIMIT_SWAP_LAST_PAIR ${Web3Signer.currentWalletId}"
        } else {
            if (inMixin()) PREF_SWAP_LAST_PAIR else "$PREF_WEB3_SWAP_LAST_PAIR ${Web3Signer.currentWalletId}"
        }
    }
    private fun getSource(): String = if (inMixin()) "mixin" else "web3"

    private fun getEntrySource(): String {
        return arguments?.getString(ARGS_ENTRY_SOURCE) ?: AnalyticsTracker.TradeSource.WALLET_HOME
    }

    private fun getInitialTabIndex(currentWalletId: String): Int {
        val entryType = arguments?.getString(ARGS_ENTRY_TYPE)
        val entrySource = arguments?.getString(ARGS_ENTRY_SOURCE)
        if (entrySource == AnalyticsTracker.TradeSource.MARKET_DETAIL && entryType == AnalyticsTracker.SpotTradeType.SIMPLE) return TAB_SIMPLE
        val preferenceKey = "$PREF_TRADE_SELECTED_TAB_PREFIX$currentWalletId"
        return defaultSharedPreferences.getInt(preferenceKey, 0)
    }

    private fun currentSpotType(tabIndex: Int): String {
        return if (tabIndex == 1) {
            AnalyticsTracker.SpotTradeType.ADVANCED
        } else {
            AnalyticsTracker.SpotTradeType.SIMPLE
        }
    }

    private fun getReferral(): String? = arguments?.getString(ARGS_REFERRAL)

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun startOrdersPolling() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch(coroutineErrorHandler) {
            while (isAdded) {
                swapViewModel.refreshOrders(walletId ?: Session.getAccountId()!!)
                swapViewModel.refreshPendingOrders()
                delay(3000)
            }
        }
    }

    private fun stopOrdersPolling() {
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onPause() {
        super.onPause()
        stopOrdersPolling()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            startOrdersPolling()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopOrdersPolling()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        swapTokens = emptyList()
        stocks = emptyList()
        trendingMarkets = emptyList()
        topGainerMarkets = emptyList()
        topLoserMarkets = emptyList()
        tokenItems = null
        web3tokens = null
    }
}
