package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.runtime.remember
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
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.api.response.web3.sortByKeywordAndBalance
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
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
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.GasCheckBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AllOrdersFragment
import one.mixin.android.ui.wallet.DepositFragment
import one.mixin.android.ui.wallet.LimitTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.SwapTransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TradeFragment : BaseFragment() {
    companion object {
        const val TAG = "SwapFragment"
        const val ARGS_WEB3_TOKENS = "args_web3_tokens"
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

        const val PREF_TRADE_SELECTED_TAB_PREFIX: String = "pref_trade_selected_tab_"

        inline fun <reified T : Swappable> newInstance(
            input: String? = null,
            output: String? = null,
            amount: String? = null,
            inMixin: Boolean = true,
            referral: String? = null,
            walletId: String? = null,
        ): TradeFragment =
            TradeFragment().withArgs {
                input?.let { putString(ARGS_INPUT, it) }
                output?.let { putString(ARGS_OUTPUT, it) }
                amount?.let { putString(ARGS_AMOUNT, it) }
                putBoolean(ARGS_IN_MIXIN, inMixin)
                referral?.let { putString(ARGS_REFERRAL, it) }
                walletId?.let { putString(ARGS_WALLET_ID, it) }
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
            val chainIds = walletId?.let {
                swapViewModel.getAddresses(it).map {
                    it.chainId
                }
            }
            initFromTo()
            refreshTokens(chainIds)
            refreshStocks(chainIds)
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
                            startOrdersPolling()
                            val currentWalletId = walletId ?: Session.getAccountId() ?: ""
                            val initialTabIndex = remember(currentWalletId) {
                                val preferenceKey = "$PREF_TRADE_SELECTED_TAB_PREFIX$currentWalletId"
                                defaultSharedPreferences.getInt(preferenceKey, 0)
                            }
                            var isLimitOrderTabBadgeDismissed by remember(currentWalletId) {
                                mutableStateOf(defaultSharedPreferences.getBoolean(Account.PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED, false))
                            }

                            if (!isLimitOrderTabBadgeDismissed) {
                                isLimitOrderTabBadgeDismissed = true
                                defaultSharedPreferences.putBoolean(Account.PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED, true)
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
                                initialAmount = initialAmount,
                                lastOrderTime = lastOrderTime,
                                reviewing = reviewing,
                                initialTabIndex = initialTabIndex,
                                source = getSource(),
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
                                        defaultSharedPreferences.putBoolean(Account.PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED, true)
                                    }
                                },
                                onTabChanged = { index ->
                                    val preferenceKey = "$PREF_TRADE_SELECTED_TAB_PREFIX$currentWalletId"
                                    defaultSharedPreferences.putInt(preferenceKey, index)
                                },
                                onReview = { quote, from, to, amount ->
                                    AnalyticsTracker.trackTradePreview()
                                    this@apply.hideKeyboard()
                                    lifecycleScope.launch {
                                        handleReview(quote, from, to, amount, navController)
                                    }
                                },
                                onLimitReview = { from, to, order ->
                                    AnalyticsTracker.trackTradePreview()
                                    this@apply.hideKeyboard()
                                    lifecycleScope.launch {
                                        openLimitTransfer(from, to, order)
                                    }
                                },
                                onDeposit = { token ->
                                    hideKeyboard()
                                    if (inMixin()) {
                                        deposit(token.assetId)
                                    } else {
                                        this@TradeFragment.lifecycleScope.launch {
                                            val t = swapViewModel.getTokenByWalletAndAssetId(Web3Signer.currentWalletId, token.assetId) ?: return@launch
                                            val address = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, token.chain.chainId)
                                            if (address == null) {
                                                toast(R.string.Alert_Not_Support)
                                                return@launch
                                            }
                                            navTo(Web3AddressFragment.newInstance(t, address?.destination, true), Web3AddressFragment.TAG)
                                        }
                                    }
                                },
                                onOrderList = { currentWalletId, filterPending ->
                                    this@apply.hideKeyboard()
                                    val target = AllOrdersFragment.newInstanceWithWalletIds(arrayListOf(currentWalletId), filterPending)
                                    if (defaultSharedPreferences.getInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 1) {
                                        defaultSharedPreferences.putInt(Account.PREF_HAS_USED_SWAP_TRANSACTION, 1)
                                        orderBadge = false
                                        RxBus.publish(BadgeEvent(Account.PREF_HAS_USED_SWAP_TRANSACTION))
                                    }
                                    navTo(target, AllOrdersFragment.TAG)
                                },
                                onLimitOrderClick = { orderId ->
                                    this@apply.hideKeyboard()
                                    navTo(OrderDetailFragment.newInstance(orderId), OrderDetailFragment.TAG)
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
                        this@TradeFragment.lifecycleScope.launch {
                            val t = swapViewModel.getTokenByWalletAndAssetId(
                                Web3Signer.currentWalletId, if (Web3Signer.evmAddress.isBlank()) {
                                    Constants.ChainId.SOLANA_CHAIN_ID
                                } else {
                                    Constants.ChainId.ETHEREUM_CHAIN_ID
                                }) ?: return@launch
                            val address = if (t.isSolanaChain()) { Web3Signer.solanaAddress } else { Web3Signer.evmAddress }
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
        lifecycleScope.launch {
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
                toast(R.string.Alert_Not_Support)
                return
            }
            val fromAddress = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, from.chain.chainId)
            if (fromAddress == null){
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
                            if (to.chain.chainId == Constants.ChainId.SOLANA_CHAIN_ID) Web3Signer.solanaAddress else Web3Signer.evmAddress
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
        if (resp == null) return
        if (inMixin()) {
            openSwapTransfer(resp, from, to)
        } else {
            openSwapTransfer(resp, from, to)
        }
    }

    private fun openSwapTransfer(swapResult: SwapResponse, from: SwapToken, to: SwapToken) {
        if (from.chain.chainId == Constants.ChainId.Solana || inMixin()) {
            AnalyticsTracker.trackTradePreview()
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

    private suspend fun openLimitTransfer(from: SwapToken, to: SwapToken, order: CreateLimitOrderResponse) {
        AnalyticsTracker.trackTradePreview()
        val senderWalletId = if (inMixin()) Session.getAccountId()!! else Web3Signer.currentWalletId
        if (!inMixin()) {
            val address = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, to.chain.chainId)
            if (address == null){
                toast(R.string.Alert_Not_Support)
                return
            }
            val fromAddress = swapViewModel.getAddressesByChainId(Web3Signer.currentWalletId, from.chain.chainId)
            if (fromAddress == null){
                toast(R.string.Alert_Not_Support)
                return
            }
        }
        LimitTransferBottomSheetDialogFragment.newInstance(order, from, to, senderWalletId).apply {
            setOnDone {
                initialAmount = null
                lastOrderTime = System.currentTimeMillis()
            }
            setOnDestroy {
                reviewing = false
            }
        }.showNow(parentFragmentManager, LimitTransferBottomSheetDialogFragment.TAG)
        reviewing = true
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

        val tempFromToken = if (input != null) {
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
        if (tempToToken?.getUnique() == tempFromToken?.getUnique()) {
            tempToToken = tokens.firstOrNull { t -> t.getUnique() != tempFromToken?.getUnique() && t.getUnique() in Constants.usdIds } ?: tokens.firstOrNull { t -> t.getUnique() != tempFromToken?.getUnique() }
        }

        if (isLimit) {
            limitFromToken = tempFromToken
            limitToToken = tempToToken
        } else {
            fromToken = tempFromToken
            toToken = tempToToken
        }
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

    private fun getReferral(): String? = arguments?.getString(ARGS_REFERRAL)

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun startOrdersPolling() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
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

    override fun onDestroyView() {
        super.onDestroyView()
        stopOrdersPolling()
    }
}
