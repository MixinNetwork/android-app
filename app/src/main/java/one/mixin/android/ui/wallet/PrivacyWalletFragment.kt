package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_HAS_USED_BUY
import one.mixin.android.Constants.Account.PREF_HAS_USED_SWAP
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentPrivacyWalletBinding
import one.mixin.android.databinding.ViewWalletFragmentHeaderBinding
import one.mixin.android.event.BadgeEvent
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.bot.INTERNAL_REFERRAL_ID
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.perps.AllPositionsFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.home.web3.trade.perps.PerpsMarketListBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel
import one.mixin.android.ui.home.web3.trade.perps.topMoversPreview
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.ui.wallet.home.WalletHomeBuilder
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomePage
import one.mixin.android.ui.wallet.home.WalletHomePositionSummary
import one.mixin.android.ui.wallet.home.WalletHomeSection
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.formatWalletHomeBtcTotal
import one.mixin.android.ui.wallet.home.getWalletHomeCacheState
import one.mixin.android.ui.wallet.home.putWalletHomeCache
import one.mixin.android.ui.wallet.home.toWalletHomePendingIndicator
import one.mixin.android.ui.wallet.home.walletHomeCacheKey
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.ui.wallet.adapter.WalletAssetAdapter
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.reportException
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.PendingDisplay
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import one.mixin.android.widget.calcPercent
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.measureTime

@AndroidEntryPoint
class PrivacyWalletFragment : BaseFragment(R.layout.fragment_privacy_wallet), HeaderAdapter.OnItemListener {
    companion object {
        const val TAG = "PrivacyWalletFragment"
        private const val PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED = "pref_wallet_home_add_wallet_banner_closed"
        private const val PREF_WALLET_HOME_CASHBACK_BANNER_CLOSED = "pref_wallet_home_cashback_banner_closed"
        private const val PREF_WALLET_HOME_REFERRAL_CLOSED = "pref_wallet_home_referral_closed"

        fun newInstance(): PrivacyWalletFragment = PrivacyWalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentPrivacyWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _headBinding: ViewWalletFragmentHeaderBinding? = null

    private val walletViewModel by viewModels<WalletViewModel>()
    private var assets: List<TokenItem> = listOf()
    private var topTokens: List<TokenItem> = emptyList()
    private var recentSnapshots: List<SnapshotItem> = emptyList()
    private var positions: List<PerpsPositionItem> = emptyList()
    private var topMovers: List<PerpsMarket> = emptyList()
    private var pendingDisplays: List<PendingDisplay> = emptyList()
    private val assetsAdapter by lazy { WalletAssetAdapter(false) }
    private val perpetualViewModel by viewModels<PerpetualViewModel>()
    private var hasLoadedHomeCache = false
    private val _homeState = MutableStateFlow(
        WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            isLoading = true,
            showImportSafetyFooter = false,
        )
    )
    private val _walletId = MutableLiveData<String>()
    private val positionsLiveData by lazy {
        _walletId.switchMap { walletId ->
            if (walletId.isNullOrEmpty()) {
                MutableLiveData<List<PerpsPositionItem>>(emptyList())
            } else {
                perpetualViewModel.observeOpenPositions(walletId).asLiveData()
            }
        }
    }
    private val marketsLiveData by lazy {
        perpetualViewModel.observeMarkets().asLiveData()
    }

    private var distance = 0
    private var snackBar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPrivacyWalletBinding.inflate(inflater, container, false)
        defaultSharedPreferences.getWalletHomeCacheState(privacyWalletHomeCacheKey())?.let {
            _homeState.value = it
            hasLoadedHomeCache = true
            isLoading = false
        }
        binding.compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.compose.setContent {
            val homeState = _homeState.collectAsState().value
            WalletHomePage(
                state = homeState,
                callbacks = walletHomeCallbacks,
            )
        }
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("onViewCreated called in PrivacyWalletFragment")
        _walletId.value = Session.getAccountId().orEmpty()
        lifecycleScope.launch {
            val queryDuration = measureTime {
                val data = walletViewModel.assetItemsNotHiddenRaw()
                assets = data
                Timber.e("assetItemsNotHiddenRaw query completed: data size: ${data.size}")
            }
            Timber.e("assetItemsNotHiddenRaw query took: $queryDuration")
            if (isAdded) {
                assetsAdapter.setAssetList(assets)
                if (lastFiatCurrency != Session.getFiatCurrency()) {
                    lastFiatCurrency = Session.getFiatCurrency()
                    assetsAdapter.notifyDataSetChanged()
                }
                if (assets.isNotEmpty()) {
                    isLoading = false
                    renderHome()
                }
                var bitcoin = assets.find { a -> a.assetId == Constants.ChainId.BITCOIN_CHAIN_ID }
                if (bitcoin == null) {
                    bitcoin = walletViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                }
                renderPie(assets, bitcoin)
                renderHome()
            }
        }

        binding.apply {
            _headBinding =
                ViewWalletFragmentHeaderBinding.bind(layoutInflater.inflate(R.layout.view_wallet_fragment_header, coinsRv, false)).apply {
                    sendReceiveView.isVisible = true
                    sendReceiveView.enableBuy()
                    sendReceiveView.buy.setOnClickListener {
                        lifecycleScope.launch {
                            WalletActivity.showBuy(requireActivity(), false, null, null)
                            defaultSharedPreferences.putBoolean(PREF_HAS_USED_BUY, false)
                            RxBus.publish(BadgeEvent(PREF_HAS_USED_BUY))
                            sendReceiveView.buyBadge.isVisible = false
                        }
                    }
                    sendReceiveView.send.setOnClickListener {
                        if (
                            showRecoveryReminderForRiskAction {
                                TokenListBottomSheetDialogFragment.newInstance(TYPE_FROM_SEND)
                                    .setOnAssetClick {
                                        WalletActivity.navigateToWalletActivity(this@PrivacyWalletFragment.requireActivity(), it)
                                    }.setOnDepositClick {
                                        // do nothing
                                    }
                                    .showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                            }
                        ) {
                            return@setOnClickListener
                        }
                        TokenListBottomSheetDialogFragment.newInstance(TYPE_FROM_SEND)
                            .setOnAssetClick {
                                WalletActivity.navigateToWalletActivity(this@PrivacyWalletFragment.requireActivity(), it)
                            }.setOnDepositClick {
                                // do nothing
                            }
                            .showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                    }
                    sendReceiveView.receive.setOnClickListener {
                        if (showRecoveryReminderForRiskAction { showReceiveAssetList() }) return@setOnClickListener
                        showReceiveAssetList()
                    }
                    sendReceiveView.swap.setOnClickListener {
                        if (
                            showRecoveryReminderForRiskAction {
                                AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
                                SwapActivity.show(
                                    requireActivity(),
                                    inMixin = true,
                                    entrySource = TradeSource.WALLET_HOME,
                                    entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                                )
                                defaultSharedPreferences.putBoolean(PREF_HAS_USED_SWAP, false)
                                RxBus.publish(BadgeEvent(PREF_HAS_USED_SWAP))
                                sendReceiveView.swapBadge.isVisible = false
                            }
                        ) {
                            return@setOnClickListener
                        }
                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
                        SwapActivity.show(
                            requireActivity(),
                            inMixin = true,
                            entrySource = TradeSource.WALLET_HOME,
                            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                        )
                        defaultSharedPreferences.putBoolean(PREF_HAS_USED_SWAP, false)
                        RxBus.publish(BadgeEvent(PREF_HAS_USED_SWAP))
                        sendReceiveView.swapBadge.isVisible = false
                    }
                }
            assetsAdapter.headerView = _headBinding!!.root
            coinsRv.itemAnimator = null
            coinsRv.setHasFixedSize(true)
            assetsAdapter.onItemListener = this@PrivacyWalletFragment

            coinsRv.adapter = assetsAdapter
            coinsRv.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        if (abs(distance) > 50.dp && snackBar?.isShown == true) {
                            snackBar?.dismiss()
                            distance = 0
                        }
                        distance += dy
                    }
                },
            )
        }

        walletViewModel.assetItemsNotHidden().observe(viewLifecycleOwner) {
            Timber.e("observe assetItemsNotHidden data size: ${it.size}")
            isLoading = false
            if (it.isEmpty()) {
                setEmpty()
                assets = emptyList()
                renderHome()
            } else {
                assets = it
                assetsAdapter.setAssetList(it)
                // Refresh the entire list when the fiat currency changes
                if (lastFiatCurrency != Session.getFiatCurrency()) {
                    lastFiatCurrency = Session.getFiatCurrency()
                    assetsAdapter.notifyDataSetChanged()
                }
                lifecycleScope.launch {
                    var bitcoin = assets.find { a -> a.assetId == Constants.ChainId.BITCOIN_CHAIN_ID }
                    if (bitcoin == null) {
                        bitcoin = walletViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                    }

                    renderPie(assets, bitcoin)
                    renderHome()
                }
            }
        }

        walletViewModel.topAssetItemsNotHiddenLimit().observe(viewLifecycleOwner) {
            topTokens = it
            renderHome()
        }

        walletViewModel.recentSnapshotsLimit().observe(viewLifecycleOwner) {
            recentSnapshots = it.take(WalletHomeSection.MORE_DETECTION_LIMIT)
            renderHome()
        }

        marketsLiveData.observe(viewLifecycleOwner) {
            topMovers = it.topMoversPreview()
            renderHome()
        }

        positionsLiveData.observe(viewLifecycleOwner) {
            positions = it
            renderHome()
        }

        walletViewModel.getPendingDisplays().observe(viewLifecycleOwner) {
            pendingDisplays = it
            _headBinding?.apply {
                pendingView.isVisible = it.isNotEmpty()
                pendingView.updateTokens(it)
                pendingView.setOnClickListener { v ->
                    if (it.size == 1) {
                        lifecycleScope.launch {
                            val token = walletViewModel.simpleAssetItem(it[0].assetId) ?: return@launch
                            WalletActivity.showWithToken(requireActivity(), token, WalletActivity.Destination.Transactions)
                        }
                    } else {
                        WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions, true)
                    }
                }
            }
            renderHome()
        }

        RxBus.listen(QuoteColorEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { _ ->
                assetsAdapter.notifyDataSetChanged()
            }

        val swap = defaultSharedPreferences.getBoolean(PREF_HAS_USED_SWAP, true)
        _headBinding?.sendReceiveView?.swapBadge?.isVisible = swap
        val buy = defaultSharedPreferences.getBoolean(PREF_HAS_USED_BUY, true)
        _headBinding?.sendReceiveView?.buyBadge?.isVisible = buy

        renderHome()
    }

    private fun renderHome() {
        if (_binding == null) return
        if (hasLoadedHomeCache && isLoading && assets.isEmpty() && recentSnapshots.isEmpty()) return
        hasLoadedHomeCache = false
        if (assets.isNotEmpty() || recentSnapshots.isNotEmpty() || topMovers.isNotEmpty() || positions.isNotEmpty()) {
            isLoading = false
        }
        _homeState.value = buildHomeState()
    }

    private var isLoading = true

    private fun buildHomeState(): WalletHomeState {
        val totalFiat = assets.fold(BigDecimal.ZERO) { acc, item -> acc + item.fiat() }
        val totalBtc = assets.fold(BigDecimal.ZERO) { acc, item -> acc + item.btc() }
        val showAddWalletBanner = !defaultSharedPreferences.getBoolean(PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED, false)
        val showCashbackBanner = !defaultSharedPreferences.getBoolean(PREF_WALLET_HOME_CASHBACK_BANNER_CLOSED, false)
        val showBanner = showAddWalletBanner || showCashbackBanner
        val showReferral = !defaultSharedPreferences.getBoolean(PREF_WALLET_HOME_REFERRAL_CLOSED, false)
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = totalFiat > BigDecimal.ZERO,
            showBanner = showBanner,
            showReferral = showReferral,
            hasPositions = positions.isNotEmpty(),
            hasTopMovers = topMovers.isNotEmpty(),
            hasTransactions = recentSnapshots.isNotEmpty(),
            hasPendingIndicator = pendingDisplays.isNotEmpty(),
            isLoading = isLoading,
        )
        val state = WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            cards = cards,
            fiatTotal = totalFiat.numberFormat2(),
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            privacyTokens = assets.take(WalletHomeSection.PREVIEW_LIMIT),
            privacyTransactions = recentSnapshots.take(WalletHomeSection.PREVIEW_LIMIT),
            positions = positions.take(WalletHomeSection.PREVIEW_LIMIT),
            positionSummary = positions.toWalletHomePositionSummary(),
            totalTokenCount = assets.size,
            totalTransactionCount = recentSnapshots.size,
            totalPositionCount = positions.size,
            isLoading = isLoading,
            topMovers = topMovers,
            allTokensHidden = assets.isEmpty() && topTokens.isNotEmpty(),
            pendingIndicator = pendingDisplays.toWalletHomePendingIndicator(),
            quoteColorReversed = defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false),
            showAddWalletBanner = showAddWalletBanner,
            showCashbackBanner = showCashbackBanner,
            showReferralBanner = showReferral,
            showBuyBadge = defaultSharedPreferences.getBoolean(PREF_HAS_USED_BUY, true),
            showSwapBadge = defaultSharedPreferences.getBoolean(PREF_HAS_USED_SWAP, true),
            showImportSafetyFooter = !isLoading,
        )
        defaultSharedPreferences.putWalletHomeCache(privacyWalletHomeCacheKey(), state)
        return state
    }

    private fun privacyWalletHomeCacheKey(): String =
        walletHomeCacheKey(WalletHomeType.PRIVACY, Session.getAccountId().orEmpty())

    private fun List<PerpsPositionItem>.toWalletHomePositionSummary(): WalletHomePositionSummary? {
        if (isEmpty()) return null
        val totalMargin = fold(BigDecimal.ZERO) { total, position ->
            total + (position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }.multiply(BigDecimal(Fiats.getRate()))
        val totalPnl = fold(BigDecimal.ZERO) { total, position ->
            total + (position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }.multiply(BigDecimal(Fiats.getRate()))
        val isProfit = when {
            totalPnl > BigDecimal.ZERO -> true
            totalPnl < BigDecimal.ZERO -> false
            else -> null
        }
        val absPnlText = "${Fiats.getSymbol()}${totalPnl.abs().numberFormat2()}"
        return WalletHomePositionSummary(
            valueText = "${Fiats.getSymbol()}${totalMargin.abs().numberFormat2()}",
            pnlText = when {
                totalPnl > BigDecimal.ZERO -> "+$absPnlText"
                totalPnl < BigDecimal.ZERO -> "-$absPnlText"
                else -> absPnlText
            },
            isProfit = isProfit,
        )
    }

    private val walletHomeCallbacks = object : WalletHomeCallbacks {
        override fun onAddWalletClicked() {
            AddWalletBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
        }

        override fun onBannerClosed() {
            defaultSharedPreferences.putBoolean(PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED, true)
            renderHome()
        }

        override fun onCashbackBannerClosed() {
            defaultSharedPreferences.putBoolean(PREF_WALLET_HOME_CASHBACK_BANNER_CLOSED, true)
            renderHome()
        }

        override fun onReferralClicked() {
            lifecycleScope.launch {
                walletViewModel.findOrSyncApp(INTERNAL_REFERRAL_ID)?.let { app ->
                    WebActivity.show(requireActivity(), url = app.homeUri, app = app, conversationId = null)
                }
            }
        }

        override fun onReferralClosed() {
            defaultSharedPreferences.putBoolean(PREF_WALLET_HOME_REFERRAL_CLOSED, true)
            renderHome()
        }

        override fun onSupportClicked() {
            lifecycleScope.launch {
                val user = walletViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                if (user == null) {
                    toast(R.string.Data_error)
                } else {
                    ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                }
            }
        }

        override fun onHelpCenterClicked() {
            context?.openUrl(getString(R.string.wallet_home_help_center_url))
        }

        override fun onBuyClicked() {
            _headBinding?.sendReceiveView?.buy?.performClick()
            renderHome()
        }

        override fun onReceiveClicked() {
            _headBinding?.sendReceiveView?.receive?.performClick()
        }

        override fun onSendClicked() {
            _headBinding?.sendReceiveView?.send?.performClick()
        }

        override fun onSwapClicked() {
            _headBinding?.sendReceiveView?.swap?.performClick()
            renderHome()
        }

        override fun onPendingIndicatorClicked() {
            if (pendingDisplays.size == 1) {
                lifecycleScope.launch {
                    val token = walletViewModel.simpleAssetItem(pendingDisplays[0].assetId) ?: return@launch
                    WalletActivity.showWithToken(requireActivity(), token, WalletActivity.Destination.Transactions)
                }
            } else if (pendingDisplays.size > 1) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions, true)
            }
        }

        override fun onWatchIndicatorClicked() = Unit

        override fun onImportKeyClicked() = Unit

        override fun onImportKeyLearnMoreClicked() = Unit

        override fun onViewMoreTokensClicked() {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTokens)
        }

        override fun onAllTokensBackClicked() = Unit

        override fun onViewMoreTransactionsClicked() {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions)
        }

        override fun onViewMorePositionsClicked() {
            AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
            navTo(
                AllPositionsFragment.newOpenInstance(AnalyticsTracker.PerpsSource.WALLET_HOME),
                AllPositionsFragment.TAG,
            )
        }

        override fun onTokenClicked(index: Int) {
            assets.getOrNull(index)?.let {
                WalletActivity.showWithToken(requireActivity(), it, WalletActivity.Destination.Transactions)
            }
        }

        override fun onTransactionClicked(index: Int) {
            val snapshot = recentSnapshots.getOrNull(index) ?: return
            lifecycleScope.launch {
                val asset = walletViewModel.simpleAssetItem(snapshot.assetId) ?: return@launch
                if (!isAdded) return@launch
                activity?.addFragment(
                    this@PrivacyWalletFragment,
                    TransactionFragment.newInstance(snapshot, asset),
                    TransactionFragment.TAG,
                )
            }
        }

        override fun onPositionClicked(index: Int) {
            val position = positions.getOrNull(index) ?: return
            lifecycleScope.launch {
                val market = perpetualViewModel.getMarketFromDb(position.marketId)
                val activity = activity ?: return@launch
                PerpsActivity.showDetail(
                    activity,
                    position.marketId,
                    market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                    market?.displaySymbol ?: position.displaySymbol.orEmpty(),
                    market?.tokenSymbol ?: position.tokenSymbol.orEmpty(),
                    AnalyticsTracker.PerpsSource.WALLET_HOME,
                )
            }
        }

        override fun onTopMoverClicked(index: Int) {
            topMovers.getOrNull(index)?.let(::onTopMoverMarketClicked) ?: onViewMoreTopMoversClicked()
        }

        override fun onTopMoverMarketClicked(market: PerpsMarket) {
            PerpsActivity.showDetail(
                requireActivity(),
                market.marketId,
                market.displaySymbol,
                market.displaySymbol,
                market.tokenSymbol,
                AnalyticsTracker.PerpsSource.PERPS_HOME_LIST,
            )
        }

        override fun onViewMoreTopMoversClicked() {
            PerpsMarketListBottomSheetDialogFragment.newInstance(
                initialSort = MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING,
            ).show(parentFragmentManager, PerpsMarketListBottomSheetDialogFragment.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        _walletId.value = Session.getAccountId().orEmpty()
        jobManager.addJobInBackground(RefreshTokensJob())
        jobManager.addJobInBackground(RefreshSnapshotsJob())
        jobManager.addJobInBackground(SyncOutputJob())
        perpetualViewModel.loadMarkets(
            onSuccess = { },
            onError = { error -> Timber.e(error) },
        )
        refreshAllPendingDeposit()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            _walletId.value = Session.getAccountId().orEmpty()
            jobManager.addJobInBackground(RefreshTokensJob())
            jobManager.addJobInBackground(RefreshSnapshotsJob())
            jobManager.addJobInBackground(SyncOutputJob())
        }
    }

    private fun refreshAllPendingDeposit() =
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = { walletViewModel.allPendingDeposit() },
                exceptionBlock = { e ->
                    reportException(e)
                    false
                },
                successBlock = {
                    val pendingDeposits = it.data ?: emptyList()
                    val destinationTags = walletViewModel.findDepositEntryDestinations()
                    pendingDeposits
                        .filter { pd ->
                            destinationTags.any { dt ->
                                dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                            }
                        }
                        .map { pd -> pd.toSnapshot() }.let { snapshots ->
                            // If there are no pending deposit snapshots belonging to the current user, clear all pending deposits
                            if (snapshots.isEmpty()) {
                                walletViewModel.clearAllPendingDeposits()
                                return@let
                            }
                            lifecycleScope.launch {
                                snapshots.map { it.assetId }.distinct().forEach {
                                    walletViewModel.findOrSyncAsset(it)
                                }
                                walletViewModel.insertPendingDeposit(snapshots)
                            }
                        }
                },
            )
        }

    private var lastFiatCurrency :String? = null

    override fun onStop() {
        super.onStop()
        snackBar?.dismiss()
    }

    override fun onDestroyView() {
        assetsAdapter.headerView = null
        assetsAdapter.onItemListener = null
        _binding = null
        _headBinding = null
        super.onDestroyView()
    }

    private fun renderPie(
        assets: List<TokenItem>,
        bitcoin: TokenItem?,
    ) {
        var totalBTC = BigDecimal.ZERO
        var totalFiat = BigDecimal.ZERO
        assets.map {
            totalFiat = totalFiat.add(it.fiat())
            if (bitcoin == null) {
                totalBTC = totalBTC.add(it.btc())
            }
        }
        if (bitcoin != null) {
            totalBTC =
                totalFiat.divide(BigDecimal(Fiats.getRate()), 16, RoundingMode.HALF_UP)
                    .divide(BigDecimal(bitcoin.priceUsd), 16, RoundingMode.HALF_UP)
        }
        _headBinding?.apply {
            totalAsTv.text =
                try {
                    if (totalBTC.numberFormat8().toFloat() == 0f) {
                        "0.00"
                    } else {
                        totalBTC.numberFormat8()
                    }
                } catch (ignored: NumberFormatException) {
                    totalBTC.numberFormat8()
                }
            totalTv.text =
                try {
                    if (totalFiat.numberFormat2().toFloat() == 0f) {
                        "0.00"
                    } else {
                        totalFiat.numberFormat2()
                    }
                } catch (ignored: NumberFormatException) {
                    totalFiat.numberFormat2()
                }
            symbol.text = Fiats.getSymbol()

            if (totalFiat.compareTo(BigDecimal.ZERO) == 0) {
                pieItemContainer.visibility = GONE
                percentView.visibility = GONE
                btcRl.updateLayoutParams<LinearLayout.LayoutParams> {
                    bottomMargin = requireContext().dpToPx(32f)
                }
                return
            }

            btcRl.updateLayoutParams<LinearLayout.LayoutParams> {
                bottomMargin = requireContext().dpToPx(16f)
            }
            pieItemContainer.visibility = VISIBLE
            percentView.visibility = VISIBLE
            setPieView(assets, totalFiat)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setEmpty() {
        _headBinding?.apply {
            pieItemContainer.visibility = GONE
            percentView.visibility = GONE
            assetsAdapter.setAssetList(emptyList())
            totalAsTv.text = "0.00"
            totalTv.text = "0.00"
        }
    }

    private fun setPieView(
        r: List<TokenItem>,
        totalUSD: BigDecimal,
    ) {
        val list =
            r.asSequence().filter {
                (it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) != 0
            }.map {
                val p = it.fiat().calcPercent(totalUSD)
                PercentView.PercentItem(it.symbol, p)
            }.toMutableList()
        if (list.isNotEmpty()) {
            _headBinding?.pieItemContainer?.removeAllViews()
            list.sortWith { o1, o2 -> ((o2.percent - o1.percent) * 100).toInt() }
            mainThread {
                _headBinding?.percentView?.setPercents(list)
            }
            when {
                list.size == 1 -> {
                    val p = list[0]
                    addItem(PercentView.PercentItem(p.name, 1f), 0)
                }
                list.size == 2 -> {
                    addItem(list[0], 0)
                    val p1 = list[1]
                    val newP1 = PercentView.PercentItem(p1.name, 1 - list[0].percent)
                    addItem(newP1, 1)
                }
                list[1].percent < 0.01f && list[1].percent > 0f -> {
                    addItem(list[0], 0)
                    addItem(PercentView.PercentItem(getString(R.string.OTHER), 0.01f), 1)
                }
                list.size == 3 -> {
                    addItem(list[0], 0)
                    addItem(list[1], 1)
                    val p2 = list[2]
                    val p2Percent = 1 - list[0].percent - list[1].percent
                    val newP2 = PercentView.PercentItem(p2.name, p2Percent)
                    addItem(newP2, 2)
                }
                else -> {
                    var pre = 0
                    for (i in 0 until 2) {
                        val p = list[i]
                        addItem(p, i)
                        pre += (p.percent * 100).toInt()
                    }
                    val other = (100 - pre) / 100f
                    val item = PercentItemView(requireContext())
                    item.setPercentItem(PercentView.PercentItem(getString(R.string.OTHER), other), 2)
                    _headBinding?.pieItemContainer?.addView(item)
                }
            }

            _headBinding?.pieItemContainer?.visibility = VISIBLE
        }
    }

    private fun addItem(
        p: PercentView.PercentItem,
        index: Int,
    ) {
        val item = PercentItemView(requireContext())
        item.setPercentItem(p, index)
        _headBinding?.pieItemContainer?.addView(item)
    }

    private fun showReceiveAssetList() {
        TokenListBottomSheetDialogFragment.newInstance(TYPE_FROM_RECEIVE)
            .setOnAssetClick { asset ->
                WalletActivity.showWithToken(requireActivity(), asset, WalletActivity.Destination.Deposit)
            }.showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showRecoveryReminderForRiskAction(onContinue: (() -> Unit)? = null): Boolean {
        return RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager, onContinue)
    }

    override fun <T> onNormalItemClick(item: T) {
        WalletActivity.showWithToken(requireActivity(), item as TokenItem, WalletActivity.Destination.Transactions)
    }
}
