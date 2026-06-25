package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.WalletHomeBannerAction
import one.mixin.android.api.response.syncedWalletHomeClosedBannerIds
import one.mixin.android.api.response.visibleWalletHomeBanners
import one.mixin.android.databinding.FragmentPrivacyWalletBinding
import one.mixin.android.databinding.ViewWalletFragmentHeaderBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.job.RefreshWeb3TokenJob
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.bot.INTERNAL_REFERRAL_ID
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.wallet.home.WalletHomeBuilder
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeDataState
import one.mixin.android.ui.wallet.home.WalletHomePage
import one.mixin.android.ui.wallet.home.WalletHomeSection
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeBalanceHandoff
import one.mixin.android.ui.wallet.home.WalletHomeBalanceSnapshot
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.calculateWalletHomeBtcTotal
import one.mixin.android.ui.wallet.home.calculateWalletHomeTokenFiat
import one.mixin.android.ui.wallet.home.formatWalletHomeBtcTotal
import one.mixin.android.ui.wallet.home.getWalletHomeCache
import one.mixin.android.ui.wallet.home.putWalletHomeCache
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyKind
import one.mixin.android.ui.wallet.home.walletHomePendingTransactionCount
import one.mixin.android.ui.wallet.home.walletHomePendingTransactionIndicator
import one.mixin.android.ui.wallet.home.walletHomeWatchIndicator
import one.mixin.android.ui.wallet.home.walletHomeCacheKey
import one.mixin.android.ui.wallet.home.walletHomeImportKeyAction
import one.mixin.android.ui.wallet.adapter.WalletWeb3TokenAdapter
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.WalletHomeTokenSummary
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.details.Web3TransactionFragment
import one.mixin.android.web3.details.Web3TransactionsFragment
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import one.mixin.android.widget.calcPercent
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class WalletHomeClassicFragment : BaseFragment(R.layout.fragment_privacy_wallet), HeaderAdapter.OnItemListener {
    companion object {
        const val TAG = "WalletHomeClassicFragment"

        fun newInstance(): WalletHomeClassicFragment = WalletHomeClassicFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentPrivacyWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _headBinding: ViewWalletFragmentHeaderBinding? = null

    private val web3ViewModel by viewModels<Web3ViewModel>()
    private var assets: List<Web3TokenItem> = listOf()
    private var tokenSummary = WalletHomeTokenSummary()
    private var recentTransactions: List<Web3TransactionItem> = emptyList()
    private var isWatchWallet: Boolean = false
    private var importKeyAction: WalletHomeImportKeyAction? = null
    private var importKeyChainId: String? = null
    private var pendingRawTransactionCount: Int = 0
    private var pendingTransactionCount: Int = 0
    private var watchAddresses: List<String> = emptyList()
    private var dynamicBanners: List<WalletHomeBanner> = emptyList()
    private var isDynamicBannerLoaded = false
    private var closedDynamicBannerIds: Set<String> = emptySet()
    private val assetsAdapter by lazy { WalletWeb3TokenAdapter(false) }

    private var distance = 0
    private var snackBar: Snackbar? = null
    private var lastFiatCurrency :String? = null
    private var bitcoinPriceUsd: BigDecimal? = null
    private var walletHomeDataState = WalletHomeDataState.EMPTY
    private val _homeState = MutableStateFlow(
        WalletHomeState(
            walletType = WalletHomeType.CLASSIC,
            isLoading = true,
            showImportSafetyFooter = false,
        )
    )
    private var isLoading = true

    private val _walletId = MutableLiveData<String>()
    var walletId: String = ""
        set(value) {
            if (value != field) {
                field = value
                walletHomeDataState = WalletHomeDataState.EMPTY
                dynamicBanners = emptyList()
                isDynamicBannerLoaded = false
                _walletId.value = value
                loadWalletHomeCache()
            }
            Timber.e("walletId set to $value")
        }

    private val tokensLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData()
            } else {
                web3ViewModel.walletHomeWeb3TokenPreview(id, WalletHomeSection.PREVIEW_LIMIT)
            }
        }
    }
    private val pendingTxCountLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData(0)
            } else {
                web3ViewModel.getPendingTransactionCount(id)
            }
        }
    }
    private val pendingRawTxCountLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData(0)
            } else {
                web3ViewModel.getPendingRawTransactionCount(id)
            }
        }
    }
    private val tokenSummaryLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData(WalletHomeTokenSummary())
            } else {
                web3ViewModel.walletHomeWeb3TokenSummary(id)
            }
        }
    }
    private val recentTransactionsLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData<List<Web3TransactionItem>>(emptyList())
            } else {
                web3ViewModel.recentWeb3Transactions(id)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPrivacyWalletBinding.inflate(inflater, container, false)
        loadWalletHomeCache()
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
        Timber.e("onViewCreated called in WalletHomeClassicFragment")
        refreshBitcoinPrice()
        refreshWalletHomeBanners()

        binding.apply {
            _headBinding =
                ViewWalletFragmentHeaderBinding.bind(layoutInflater.inflate(R.layout.view_wallet_fragment_header, coinsRv, false)).apply {
                    sendReceiveView.enableBuy()
                    sendReceiveView.buy.setOnClickListener {
                        showBuyOptionsBottomSheet()
                    }
                    sendReceiveView.send.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val walletResume = web3ViewModel.findWalletById(walletId)
                                        val chainIdResume = web3ViewModel.getAddresses(walletId).first().chainId
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), chainIdResume)) return@launch
                                        Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, TYPE_FROM_SEND).apply {
                                            setOnClickListener { token ->
                                                this@WalletHomeClassicFragment.lifecycleScope.launch {
                                                    if (walletId.isEmpty()) {
                                                        toast(R.string.Data_error)
                                                        return@launch
                                                    }
                                                    val walletInner = web3ViewModel.findWalletById(walletId)?.toWeb3Wallet()
                                                    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                                                    if (walletInner == null) {
                                                        toast(R.string.Data_error)
                                                        return@launch
                                                    }
                                                    val chain = web3ViewModel.web3TokenItemById(token.walletId, token.chainId)
                                                    if (chain == null) {
                                                        toast(R.string.Data_error)
                                                        return@launch
                                                    }
                                                    Timber.e("chain ${chain.name} ${token.chainId} ${chain.chainId}")
                                                    WalletActivity.navigateToWalletActivity(this@WalletHomeClassicFragment.requireActivity(), address?.destination, token, chain, walletInner)
                                                }
                                                dismissNow()
                                            }
                                        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
                                    }
                                }
                            ) {
                                return@launch
                            }
                            Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, TYPE_FROM_SEND).apply {
                                setOnClickListener { token ->
                                    this@WalletHomeClassicFragment.lifecycleScope.launch {
                                        if (walletId.isEmpty()) {
                                            toast(R.string.Data_error)
                                            return@launch
                                        }
                                        val wallet = web3ViewModel.findWalletById(walletId)?.toWeb3Wallet()
                                        val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                                        if (wallet == null) {
                                            toast(R.string.Data_error)
                                            return@launch
                                        }
                                        val chain = web3ViewModel.web3TokenItemById(token.walletId, token.chainId)
                                        if (chain == null) {
                                            toast(R.string.Data_error)
                                            return@launch
                                        }
                                        Timber.e("chain ${chain.name} ${token.chainId} ${chain.chainId}")
                                        WalletActivity.navigateToWalletActivity(this@WalletHomeClassicFragment.requireActivity(), address?.destination, token, chain, wallet)
                                    }
                                    dismissNow()
                                }
                            }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
                        }
                    }
                    sendReceiveView.receive.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val walletResume = web3ViewModel.findWalletById(walletId)
                                        val chainIdResume = web3ViewModel.getAddresses(walletId).first().chainId
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), chainIdResume)) return@launch
                                        showReceiveAssetList()
                                    }
                                }
                            ) {
                                return@launch
                            }
                            showReceiveAssetList()
                        }
                    }
                    sendReceiveView.swap.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val walletResume = web3ViewModel.findWalletById(walletId)
                                        val chainIdResume = web3ViewModel.getAddresses(walletId).first().chainId
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), chainIdResume)) return@launch
                                        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.WALLET_HOME)
                                        SwapActivity.show(
                                            requireActivity(),
                                            inMixin = false,
                                            walletId = walletId,
                                            entrySource = TradeSource.WALLET_HOME,
                                            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                                        )
                                    }
                                }
                            ) {
                                return@launch
                            }
                            AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.WALLET_HOME)
                            SwapActivity.show(
                                requireActivity(),
                                inMixin = false,
                                walletId = walletId,
                                entrySource = TradeSource.WALLET_HOME,
                                entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                            )
                        }
                    }
                }
            _headBinding?.pendingView?.isVisible = false

            _headBinding?.web3PendingView?.setOnClickListener {
                if ((_headBinding?.web3PendingView?.getPendingCount() ?: 0) > 0) {
                    WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(walletId = walletId), pendingType = true)
                }
            }
            assetsAdapter.headerView = _headBinding!!.root
            coinsRv.itemAnimator = null
            coinsRv.setHasFixedSize(true)
            assetsAdapter.onItemListener = this@WalletHomeClassicFragment

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

        _headBinding?.watchLayout?.setOnClickListener {
            WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.VIEW_ADDRESS, walletId = walletId)
        }
        _walletId.observe(viewLifecycleOwner) { id ->
            if (id.isNotEmpty()) {
                lifecycleScope.launch {
                    refreshWalletHomeMetadata(id)
                }
            }
        }

        _headBinding?.web3PendingView?.observePendingCount(viewLifecycleOwner, pendingTxCountLiveData)
        pendingRawTxCountLiveData.observe(viewLifecycleOwner) {
            pendingRawTransactionCount = it
            renderHome()
        }
        pendingTxCountLiveData.observe(viewLifecycleOwner) {
            pendingTransactionCount = it
            renderHome()
        }
        tokensLiveData.observe(viewLifecycleOwner, observer)
        tokenSummaryLiveData.observe(viewLifecycleOwner) {
            tokenSummary = it
            walletHomeDataState = WalletHomeDataState.DATABASE
            isLoading = false
            renderHome()
        }
        recentTransactionsLiveData.observe(viewLifecycleOwner) {
            recentTransactions = it.take(WalletHomeSection.MORE_DETECTION_LIMIT)
            renderHome()
        }

        RxBus.listen(QuoteColorEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { _ ->
                assetsAdapter.notifyDataSetChanged()
            }

        renderHome()
    }

    private val observer = Observer<List<Web3TokenItem>> { data ->
        Timber.e("observe walletHomeWeb3TokenPreview data size: ${data.size}, walletId: $walletId")
        if (data.isEmpty()) {
            setEmpty()
            assets = data
            assetsAdapter.setAssetList(data)
            if (lastFiatCurrency != Session.getFiatCurrency()) {
                lastFiatCurrency = Session.getFiatCurrency()
                assetsAdapter.notifyDataSetChanged()
            }
            renderHome()
        } else {
            assets = data
            assetsAdapter.setAssetList(data)
            if (lastFiatCurrency != Session.getFiatCurrency()) {
                lastFiatCurrency = Session.getFiatCurrency()
                assetsAdapter.notifyDataSetChanged()
            }
            renderHome()
        }
    }

    private fun renderHome() {
        mainThread {
            if (_binding == null) return@mainThread
            if (walletHomeDataState != WalletHomeDataState.DATABASE) return@mainThread
            if (assets.isNotEmpty() || recentTransactions.isNotEmpty()) {
                isLoading = false
            }
            val state = buildHomeState()
            _homeState.value = state
            renderHeaderTotals(state)
        }
    }

    private fun buildHomeState(): WalletHomeState {
        val fiatRate = Fiats.getRate().toBigDecimal()
        val totalFiat = calculateWalletHomeTokenFiat(
            totalUsd = BigDecimal.valueOf(tokenSummary.totalUsd),
            fiatRate = fiatRate,
        )
        val totalBtc = calculateWalletHomeBtcTotal(
            tokenFiat = totalFiat,
            tokenBtc = BigDecimal.valueOf(tokenSummary.totalBtc),
            bitcoinPriceUsd = homeBitcoinPriceUsd(),
            fiatRate = fiatRate,
        )
        val showAddWalletBanner = !defaultSharedPreferences.getBoolean(PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED, false)
        val visibleDynamicBanners = dynamicBanners.visibleWalletHomeBanners(closedDynamicBannerIds)
        val showBanner = isDynamicBannerLoaded && (visibleDynamicBanners.isNotEmpty() || showAddWalletBanner)
        val showReferral = !defaultSharedPreferences.getBoolean(PREF_WALLET_HOME_REFERRAL_CLOSED, false)
        val currentImportKeyAction = importKeyAction
        val pendingCount = walletHomePendingTransactionCount(pendingRawTransactionCount, pendingTransactionCount)
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = totalFiat > BigDecimal.ZERO,
            showBanner = showBanner,
            showReferral = showReferral,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = recentTransactions.isNotEmpty(),
            hasImportKeyAction = currentImportKeyAction != null,
            hasPendingIndicator = pendingCount > 0,
            isLoading = isLoading,
        )
        val state = WalletHomeState(
            walletType = WalletHomeType.CLASSIC,
            cards = cards,
            fiatTotal = totalFiat.numberFormat2(),
            tokenFiatTotal = totalFiat.numberFormat2(),
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            web3Tokens = assets.take(WalletHomeSection.PREVIEW_LIMIT),
            web3Transactions = recentTransactions.take(WalletHomeSection.PREVIEW_LIMIT),
            totalTokenCount = tokenSummary.tokenCount,
            totalTransactionCount = recentTransactions.size,
            isLoading = isLoading,
            allTokensHidden = tokenSummary.hasOnlyHiddenTokens(),
            isWatchWallet = isWatchWallet,
            pendingIndicator = walletHomePendingTransactionIndicator(pendingCount),
            watchIndicator = if (isWatchWallet) walletHomeWatchIndicator(watchAddresses) else null,
            importKeyAction = currentImportKeyAction,
            showAddWalletBanner = showAddWalletBanner,
            isDynamicBannerLoaded = isDynamicBannerLoaded,
            dynamicBanners = visibleDynamicBanners,
            showReferralBanner = showReferral,
            showImportSafetyFooter = !isLoading,
        )
        defaultSharedPreferences.putWalletHomeCache(
            classicWalletHomeCacheKey(),
            state,
            watchAddresses = watchAddresses,
            importKeyChainId = importKeyChainId,
        )
        return state
    }

    private fun renderHeaderTotals(state: WalletHomeState) {
        _headBinding?.apply {
            totalAsTv.text = state.btcTotal
            totalTv.text = state.fiatTotal
            symbol.text = state.fiatSymbol
        }
    }

    private fun refreshBitcoinPrice() {
        lifecycleScope.launch {
            bitcoinPriceUsd = web3ViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                ?.priceUsd
                ?.toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }
            renderHome()
        }
    }

    private fun homeBitcoinPriceUsd(): BigDecimal? =
        tokenSummary.bitcoinPriceUsd
            ?.toBigDecimalOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?: bitcoinPriceUsd

    private suspend fun refreshWalletHomeMetadata(id: String) {
        val wallet = web3ViewModel.findWalletById(id)
        isWatchWallet = wallet?.isWatch() == true
        importKeyAction = wallet?.let { walletHomeImportKeyAction(it.category, it.hasLocalPrivateKey) }
        importKeyChainId = if (importKeyAction?.kind == WalletHomeImportKeyKind.PRIVATE_KEY) {
            web3ViewModel.getAddresses(id).firstOrNull()?.chainId
        } else {
            null
        }
        _headBinding?.sendReceiveView?.isVisible = !isWatchWallet
        _headBinding?.watchLayout?.isVisible = isWatchWallet

        if (isWatchWallet) {
            val addresses = web3ViewModel.getAddressesGroupedByDestination(id)
            watchAddresses = addresses.map { it.destination }
            if (addresses.isNotEmpty()) {
                if (addresses.size == 1) {
                    val address = addresses.first().destination
                    _headBinding?.watchTv?.text = getString(R.string.watching_address, "${address.take(6)}..${address.takeLast(4)}")
                } else {
                    _headBinding?.watchTv?.text = getString(R.string.watching_addresses, addresses.size)
                }
            }
        } else {
            watchAddresses = emptyList()
        }
        if (walletHomeDataState == WalletHomeDataState.CACHE) {
            _homeState.value = _homeState.value.copy(
                isWatchWallet = isWatchWallet,
                watchIndicator = if (isWatchWallet) walletHomeWatchIndicator(watchAddresses) else null,
                importKeyAction = importKeyAction,
            )
        }
        renderHome()
    }

    fun refreshWalletHomeBanners() {
        if (!isAdded || walletId.isEmpty()) return
        lifecycleScope.launch {
            val remoteBanners = runCatching {
                web3ViewModel.walletHomeBanners(walletHomeBannerChains())
            }.onFailure {
                Timber.w(it, "Fetch wallet home banners failed")
            }.getOrDefault(emptyList())
            runCatching {
                syncClosedDynamicBannerIds(remoteBanners)
            }.onFailure {
                Timber.w(it, "Sync wallet home banner closed ids failed")
            }
            dynamicBanners = remoteBanners
            isDynamicBannerLoaded = true
            renderHome()
        }
    }

    private suspend fun walletHomeBannerChains(): List<String> =
        web3ViewModel.getAddresses(walletId)
            .map { it.chainId }
            .filter(String::isNotBlank)
            .distinct()

    private suspend fun syncClosedDynamicBannerIds(remoteBanners: List<WalletHomeBanner>) {
        val closedBannerIds = findWalletHomeDynamicBannerClosedIds()
        val syncedClosedBannerIds = closedBannerIds.syncedWalletHomeClosedBannerIds(remoteBanners)
        if (syncedClosedBannerIds != closedBannerIds) {
            updateWalletHomeDynamicBannerClosedIds(syncedClosedBannerIds)
        }
        closedDynamicBannerIds = syncedClosedBannerIds
    }

    private fun loadWalletHomeCache() {
        if (!isAdded || walletId.isEmpty()) return
        walletHomeDataState = WalletHomeDataState.EMPTY
        val cache = defaultSharedPreferences.getWalletHomeCache(classicWalletHomeCacheKey())
        if (cache == null) {
            isLoading = true
            _homeState.value = WalletHomeState(
                walletType = WalletHomeType.CLASSIC,
                isLoading = true,
                showImportSafetyFooter = false,
            )
        } else {
            _homeState.value = cache.toState()
            isWatchWallet = cache.isWatchWallet
            watchAddresses = cache.watchAddresses.orEmpty()
            pendingTransactionCount = cache.pendingIndicator?.value?.toIntOrNull() ?: pendingTransactionCount
            importKeyAction = cache.importKeyAction
            importKeyChainId = cache.importKeyChainId
            walletHomeDataState = WalletHomeDataState.CACHE
            isLoading = false
        }
    }

    private fun classicWalletHomeCacheKey(): String =
        walletHomeCacheKey(WalletHomeType.CLASSIC, walletId)

    private fun showBuyOptionsBottomSheet() {
        WalletBuyOptionsBottomSheetDialogFragment.newInstance(
            walletName = getString(R.string.Common_Wallet),
        )
            .setOnGooglePayOrCard(::openClassicBuy)
            .setOnBankTransfer { openCashHome(addBank = true) }
            .showNow(parentFragmentManager, WalletBuyOptionsBottomSheetDialogFragment.TAG)
    }

    private fun openClassicBuy() {
        lifecycleScope.launch {
            val wallet = web3ViewModel.findWalletById(walletId)
            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), chainId)) return@launch
            WalletActivity.showBuy(requireActivity(), true, null, null, walletId)
        }
    }

    private fun openCashHome(addBank: Boolean = false) {
        lifecycleScope.launch {
            val app = web3ViewModel.findOrSyncApp(Constants.MIXIN_CASH_USER_ID)
            val url = cashHomeUrl(app?.homeUri, addBank)
            if (app == null) {
                WebActivity.show(requireActivity(), url = url, app = null, conversationId = null)
            } else {
                WebActivity.show(requireActivity(), url = url, app = app, conversationId = null)
            }
        }
    }

    private fun cashHomeUrl(
        homeUri: String?,
        addBank: Boolean,
    ): String {
        val url = homeUri.takeUnless { it.isNullOrBlank() } ?: Constants.API.CASH_HOME_URL
        return if (addBank) {
            Uri.parse(url).buildUpon()
                .appendQueryParameter("action", "add-cash-bank")
                .build()
                .toString()
        } else {
            url
        }
    }

    private val walletHomeCallbacks = object : WalletHomeCallbacks {
        override fun onAddWalletClicked() {
            AddWalletBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
        }

        override fun onBannerClosed() {
            defaultSharedPreferences.putBoolean(PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED, true)
            renderHome()
        }

        override fun onDynamicBannerClicked(banner: WalletHomeBanner) {
            AnalyticsTracker.trackWalletHomeAdBanner(
                banner.trackingKey,
                AnalyticsTracker.WalletHomeAdBannerSource.BACKGROUND,
            )
            banner.actionUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(::openClassicBannerAction)
        }

        override fun onDynamicBannerActionClicked(banner: WalletHomeBanner, action: WalletHomeBannerAction) {
            AnalyticsTracker.trackWalletHomeAdBanner(
                banner.trackingKey,
                AnalyticsTracker.WalletHomeAdBannerSource.BUTTON,
            )
            action.action
                ?.takeIf { it.isNotBlank() }
                ?.let(::openClassicBannerAction)
        }

        override fun onDynamicBannerClosed(banner: WalletHomeBanner) {
            lifecycleScope.launch {
                val closedIds = closedDynamicBannerIds.toMutableSet().apply { add(banner.key) }
                updateWalletHomeDynamicBannerClosedIds(closedIds)
                closedDynamicBannerIds = closedIds
                renderHome()
            }
        }

        override fun onReferralClicked() {
            lifecycleScope.launch {
                web3ViewModel.findOrSyncApp(INTERNAL_REFERRAL_ID)?.let { app ->
                    WebActivity.show(requireActivity(), url = app.homeUri, app = app, conversationId = null)
                }
            }
        }

        override fun onReferralClosed() {
            defaultSharedPreferences.putBoolean(PREF_WALLET_HOME_REFERRAL_CLOSED, true)
            renderHome()
        }

        override fun onCashClicked() = Unit

        override fun onSupportClicked() {
            lifecycleScope.launch {
                val user = web3ViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
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
        }

        override fun onReceiveClicked() {
            _headBinding?.sendReceiveView?.receive?.performClick()
        }

        override fun onSendClicked() {
            _headBinding?.sendReceiveView?.send?.performClick()
        }

        override fun onSwapClicked() {
            _headBinding?.sendReceiveView?.swap?.performClick()
        }

        override fun onPendingIndicatorClicked() {
            when (walletHomePendingTransactionCount(pendingRawTransactionCount, pendingTransactionCount)) {
                0 -> Unit
                1 -> lifecycleScope.launch {
                    val transaction = web3ViewModel.getPendingTransactionItems(walletId).singleOrNull()
                    if (transaction != null) {
                        showTransactionDetail(transaction)
                    } else {
                        showPendingTransactions()
                    }
                }
                else -> showPendingTransactions()
            }
        }

        override fun onWatchIndicatorClicked() {
            WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.VIEW_ADDRESS, walletId = walletId)
        }

        override fun onImportKeyClicked() {
            val mode = when (importKeyAction?.kind) {
                WalletHomeImportKeyKind.MNEMONIC_PHRASE -> WalletSecurityActivity.Mode.RE_IMPORT_MNEMONIC
                WalletHomeImportKeyKind.PRIVATE_KEY -> WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY
                null -> return
            }
            WalletSecurityActivity.show(requireActivity(), mode, chainId = importKeyChainId, walletId = walletId)
        }

        override fun onImportKeyLearnMoreClicked() {
            importKeyAction?.let { context?.openUrl(getString(it.learnMoreUrlRes)) }
        }

        override fun onViewMoreTokensClicked() {
            val state = _homeState.value
            WalletHomeBalanceHandoff.saveWeb3Balance(
                walletId,
                WalletHomeBalanceSnapshot(
                    fiatTotal = state.fiatTotal,
                    tokenFiatTotal = state.tokenFiatTotal,
                    btcTotal = state.btcTotal,
                    fiatSymbol = state.fiatSymbol,
                    totalTokenCount = state.totalTokenCount,
                ),
            )
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Tokens(walletId))
        }

        override fun onAllTokensBackClicked() = Unit

        override fun onViewMoreTransactionsClicked() {
            if (walletId.isNotEmpty()) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(walletId))
            }
        }

        override fun onViewMorePositionsClicked() = Unit

        override fun onTokenClicked(index: Int) {
            assets.getOrNull(index)?.let { token ->
                lifecycleScope.launch {
                    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                    WalletActivity.showWithWeb3Token(
                        requireActivity(),
                        token,
                        address?.destination,
                        WalletActivity.Destination.Web3Transactions
                    )
                }
            }
        }

        override fun onTransactionClicked(index: Int) {
            val transaction = recentTransactions.getOrNull(index) ?: return
            lifecycleScope.launch {
                showTransactionDetail(transaction)
            }
        }

        override fun onTransactionUserClicked(userId: String) = Unit

        override fun onPositionClicked(index: Int) = Unit

        override fun onTopMoverClicked(index: Int) = Unit
    }

    private fun openClassicBannerAction(url: String) {
        when (val target = url.toClassicWalletHomeBannerActionTarget()) {
            is WalletHomeBannerActionTarget.SpotTrade -> {
                val tab = if (target.action.openLimit) TradeFragment.TAB_ADVANCED else TradeFragment.TAB_SIMPLE
                AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
                defaultSharedPreferences.putInt("${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}${Session.getAccountId().orEmpty()}", tab)
                SwapActivity.show(
                    requireActivity(),
                    target.action.input,
                    target.action.output,
                    target.action.amount,
                    target.action.referral,
                    entrySource = TradeSource.WALLET_HOME,
                    entryType = if (target.action.openLimit) {
                        AnalyticsTracker.SpotTradeType.ADVANCED
                    } else {
                        AnalyticsTracker.SpotTradeType.SIMPLE
                    },
                    initialTab = tab,
                )
            }
            is WalletHomeBannerActionTarget.PerpsMarket -> {
                PerpsActivity.showDetail(
                    requireActivity(),
                    target.marketId,
                    "",
                    "",
                    "",
                    AnalyticsTracker.PerpsSource.WALLET_HOME,
                )
            }
            WalletHomeBannerActionTarget.PerpsTab -> {
                AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
                defaultSharedPreferences.putInt("${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}${Session.getAccountId().orEmpty()}", TradeFragment.TAB_PERPETUAL)
                SwapActivity.show(
                    requireActivity(),
                    entrySource = TradeSource.WALLET_HOME,
                    entryType = AnalyticsTracker.SpotTradeType.PERPETUAL,
                    initialTab = TradeFragment.TAB_PERPETUAL,
                )
            }
            WalletHomeBannerActionTarget.Buy -> {
                showBuyOptionsBottomSheet()
            }
            is WalletHomeBannerActionTarget.Web -> {
                WebActivity.show(requireActivity(), target.url, null)
            }
        }
    }

    private fun showPendingTransactions() {
        if (walletId.isNotEmpty()) {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(walletId = walletId), pendingType = true)
        }
    }

    private suspend fun showTransactionDetail(transaction: Web3TransactionItem) {
        val token = web3ViewModel.web3TokenItemById(walletId, transaction.getMainAssetId()) ?: return
        val wallet = web3ViewModel.findWalletById(walletId) ?: return
        if (!isAdded) return
        activity?.addFragment(
            this@WalletHomeClassicFragment,
            Web3TransactionFragment().withArgs {
                putParcelable(Web3TransactionFragment.ARGS_TRANSACTION, transaction)
                putString(Web3TransactionFragment.ARGS_CHAIN, transaction.chainId)
                putParcelable(Web3TransactionsFragment.ARGS_TOKEN, token)
                putParcelable(Web3TransactionFragment.ARGS_WALLET, wallet.toWeb3Wallet())
            },
            Web3TransactionFragment.TAG,
            name = Web3TransactionsFragment.TAG,
        )
    }

    fun update() {
        if (walletId.isEmpty().not()) {
            jobManager.addJobInBackground(RefreshWeb3TransactionsJob(walletId))
        }
        if (walletId.isEmpty().not()) {
            jobManager.addJobInBackground(RefreshWeb3TokenJob(walletId = walletId))
        }
    }

    override fun onResume() {
        super.onResume()
        jobManager.addJobInBackground(RefreshSingleWalletJob(Web3Signer.currentWalletId))
        if (walletId.isNotEmpty()) {
            lifecycleScope.launch {
                refreshWalletHomeMetadata(walletId)
            }
            refreshWalletHomeBanners()
        }
        refreshJob = PendingTransactionRefreshHelper.startRefreshData(
            fragment = this,
            web3ViewModel = web3ViewModel,
            jobManager = jobManager,
            refreshJob = refreshJob
        )
    }
    private var refreshJob: Job? = null

    override fun onPause() {
        super.onPause()
        refreshJob = PendingTransactionRefreshHelper.cancelRefreshData(refreshJob)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            jobManager.addJobInBackground(RefreshSingleWalletJob(Web3Signer.currentWalletId))
            refreshWalletHomeBanners()
        }
    }

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

    private suspend fun renderPie(
        assets: List<Web3TokenItem>,
        bitcoin: TokenItem?,
    ) {
        var totalBTC = BigDecimal.ZERO
        var totalFiat = BigDecimal.ZERO
        assets.map {
            totalFiat = totalFiat.add(it.fiat())
            if (bitcoin == null) {
                totalBTC = totalBTC.add(it.btcValue(bitcoin?.priceUsd?.toBigDecimalOrNull() ?: BigDecimal.ZERO))
            }
        }
        if (bitcoin != null) {
            totalBTC =
                totalFiat.divide(BigDecimal(Fiats.getRate()), 16, RoundingMode.HALF_UP)
                    .divide(BigDecimal(bitcoin.priceUsd), 16, RoundingMode.HALF_UP)
        }
        withContext(Dispatchers.Main) {
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
                    return@withContext
                }

                btcRl.updateLayoutParams<LinearLayout.LayoutParams> {
                    bottomMargin = requireContext().dpToPx(16f)
                }
                pieItemContainer.visibility = VISIBLE
                percentView.visibility = VISIBLE
                setPieView(assets, totalFiat)
            }
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
        r: List<Web3TokenItem>,
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
        Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, TYPE_FROM_RECEIVE).apply {
            setOnClickListener { token ->
                this@WalletHomeClassicFragment.lifecycleScope.launch {
                    val address = web3ViewModel.getAddressesByChainId(this@WalletHomeClassicFragment.walletId, token.chainId)
                    WalletActivity.showWithAddress(this@WalletHomeClassicFragment.requireActivity(), address?.destination, token, WalletActivity.Destination.Address)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showRecoveryReminderForRiskAction(onContinue: (() -> Unit)? = null): Boolean {
        return RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager, onContinue)
    }

    private fun showImportKeyReminderIfNeeded(wallet: Web3Wallet?, chainId: String?): Boolean {
        if (wallet?.isImported() != true || wallet.hasLocalPrivateKey) return false
        ImportKeyBottomSheetDialogFragment.newInstance(
            if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) {
                ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase
            } else {
                ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey
            },
            walletId = walletId,
            chainId = chainId,
        ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
        return true
    }

    override fun <T> onNormalItemClick(item: T) {
        val token = item as Web3TokenItem
        lifecycleScope.launch {
            val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
            WalletActivity.showWithWeb3Token(
                requireActivity(),
                token,
                address?.destination,
                WalletActivity.Destination.Web3Transactions
            )
        }
    }

}
