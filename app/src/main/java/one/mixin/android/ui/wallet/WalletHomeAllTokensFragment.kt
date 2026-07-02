package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.FragmentWalletHomeAllTokensBinding
import one.mixin.android.databinding.ViewClassicWalletBottomBinding
import one.mixin.android.databinding.ViewPrivacyWalletBottomBinding
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.isClassic
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isMixinSafe
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyKind
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeBalanceHandoff
import one.mixin.android.ui.wallet.home.WalletHomeBalanceSnapshot
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.calculateWalletHomeTotalFiat
import one.mixin.android.ui.wallet.home.components.WalletHomeAllTokensPage
import one.mixin.android.ui.wallet.home.formatWalletHomeBtcTotal
import one.mixin.android.ui.wallet.home.positionMarginUsdTotal
import one.mixin.android.ui.wallet.home.toWalletHomePendingIndicator
import one.mixin.android.ui.wallet.home.walletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.walletHomePendingTransactionIndicator
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.reportException
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.PendingDisplay
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.web3.details.Web3TransactionFragment
import one.mixin.android.web3.details.Web3TransactionsFragment
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal
import java.math.RoundingMode
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE as WEB3_TYPE_FROM_RECEIVE
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND as WEB3_TYPE_FROM_SEND

@AndroidEntryPoint
class WalletHomeAllTokensFragment : BaseFragment() {
    companion object {
        const val ARGS_WALLET_TYPE = "args_wallet_type"
        const val ARGS_WALLET_ID = "args_wallet_id"
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val perpetualViewModel by viewModels<PerpetualViewModel>()

    private val walletType by lazy {
        WalletHomeType.valueOf(requireArguments().getString(ARGS_WALLET_TYPE) ?: WalletHomeType.PRIVACY.name)
    }
    private val walletId by lazy { requireArguments().getString(ARGS_WALLET_ID).orEmpty() }
    private val _walletId = MutableLiveData<String>()
    private val homeState = MutableStateFlow(WalletHomeState(walletType = WalletHomeType.PRIVACY))
    private var privacyTokens: List<TokenItem> = emptyList()
    private var web3Tokens: List<Web3TokenItem> = emptyList()
    private var balanceSnapshot: WalletHomeBalanceSnapshot? = null
    private var positions: List<PerpsPositionItem> = emptyList()
    private var pendingTxCount: Int = 0
    private var pendingDisplays: List<PendingDisplay> = emptyList()
    private var wallet: WalletItem? = null
    private var bitcoinPriceUsd: BigDecimal? = null
    private var importKeyAction: WalletHomeImportKeyAction? = null
    private var importKeyChainId: String? = null
    private var _binding: FragmentWalletHomeAllTokensBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val web3TokensLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData<List<Web3TokenItem>>(emptyList())
            } else {
                web3ViewModel.web3TokensExcludeHidden(id)
            }
        }
    }
    private val positionsLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData<List<PerpsPositionItem>>(emptyList())
            } else {
                perpetualViewModel.observeOpenPositions(id).asLiveData()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWalletHomeAllTokensBinding.inflate(inflater, container, false)
        binding.compose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.compose.setContent {
            WalletHomeAllTokensPage(
                state = homeState.collectAsState().value,
                callbacks = callbacks,
            )
        }
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        lifecycleScope.launch {
            homeState.collect {
                updateTitle()
            }
        }
        binding.searchIb.setOnClickListener {
            if (walletType == WalletHomeType.PRIVACY) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Search)
            } else {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.SearchWeb3(walletId))
            }
        }
        binding.moreIb.setOnClickListener {
            if (walletType == WalletHomeType.PRIVACY) {
                showPrivacyBottom()
            } else {
                showClassicBottom()
            }
        }

        applyInitialBalanceSnapshot()
        walletViewModel.getPendingDisplays().observe(viewLifecycleOwner) {
            pendingDisplays = it
            renderHome()
        }
        if (walletType == WalletHomeType.PRIVACY) {
            refreshAllPendingDeposit()
        }

        if (walletType == WalletHomeType.CLASSIC) {
            lifecycleScope.launch {
                wallet = walletViewModel.findWalletById(walletId)
                importKeyAction = wallet?.let { walletHomeImportKeyAction(it.category, it.hasLocalPrivateKey) }
                importKeyChainId = if (importKeyAction?.kind == WalletHomeImportKeyKind.PRIVATE_KEY) {
                    web3ViewModel.getAddresses(walletId).firstOrNull()?.chainId
                } else {
                    null
                }
                bitcoinPriceUsd = web3ViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                    ?.priceUsd
                    ?.toBigDecimalOrNull()
                    ?.takeIf { it > BigDecimal.ZERO }
                renderHome()
            }
        }

        if (walletType == WalletHomeType.PRIVACY) {
            walletViewModel.assetItemsNotHidden().observe(viewLifecycleOwner) { tokens ->
                privacyTokens = tokens
                renderHome()
            }
            _walletId.value = walletId
            positionsLiveData.observe(viewLifecycleOwner) { positions ->
                this.positions = positions
                renderHome()
            }
        } else {
            _walletId.value = walletId
            web3TokensLiveData.observe(viewLifecycleOwner) { tokens ->
                web3Tokens = tokens
                renderHome()
            }
            pendingTxCountLiveData.observe(viewLifecycleOwner) { count ->
                pendingTxCount = count
                renderHome()
            }
        }
        renderHome()
    }

    private fun applyInitialBalanceSnapshot() {
        balanceSnapshot = if (walletType == WalletHomeType.PRIVACY) {
            WalletHomeBalanceHandoff.consumePrivacyBalance()
        } else {
            WalletHomeBalanceHandoff.consumeWeb3Balance(walletId)
        }
        if (balanceSnapshot != null) {
            renderHome()
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

    @SuppressLint("InflateParams")
    private fun showPrivacyBottom() {
        val bottomView = View.inflate(
            ContextThemeWrapper(requireActivity(), R.style.Custom),
            R.layout.view_privacy_wallet_bottom,
            null,
        )
        val bottomBinding = ViewPrivacyWalletBottomBinding.bind(bottomView)
        val bottomSheet = BottomSheet.Builder(requireActivity())
            .setCustomView(bottomBinding.root)
            .create()
        bottomBinding.migrate.visibility = View.GONE
        bottomBinding.hide.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Hidden)
            bottomSheet.dismiss()
        }
        bottomBinding.transactionsTv.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions)
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    @SuppressLint("InflateParams")
    private fun showClassicBottom() {
        val bottomView = View.inflate(
            ContextThemeWrapper(requireActivity(), R.style.Custom),
            R.layout.view_classic_wallet_bottom,
            null,
        )
        val bottomBinding = ViewClassicWalletBottomBinding.bind(bottomView)
        val bottomSheet = BottomSheet.Builder(requireActivity())
            .setCustomView(bottomBinding.root)
            .create()
        bottomBinding.rename.visibility = View.GONE
        bottomBinding.hide.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden(walletId))
            bottomSheet.dismiss()
        }
        bottomBinding.transactionsTv.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(walletId))
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun renderHome() {
        homeState.value = when (walletType) {
            WalletHomeType.PRIVACY -> buildPrivacyState()
            WalletHomeType.CLASSIC -> buildWeb3State()
        }
    }

    private fun buildPrivacyState(): WalletHomeState {
        balanceSnapshot?.let { snapshot ->
            if (privacyTokens.isEmpty()) {
                return WalletHomeState(
                    walletType = WalletHomeType.PRIVACY,
                    cards = listOf(WalletHomeCardType.BALANCE),
                    fiatTotal = snapshot.fiatTotal,
                    tokenFiatTotal = snapshot.tokenFiatTotal,
                    btcTotal = snapshot.btcTotal,
                    fiatSymbol = snapshot.fiatSymbol,
                    totalTokenCount = snapshot.totalTokenCount,
                    pendingIndicator = pendingDisplays.toWalletHomePendingIndicator(),
                )
            }
        }
        val fiatRate = Fiats.getRate().toBigDecimal()
        val tokenFiat = privacyTokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.fiat() }
        val totalFiat = calculateWalletHomeTotalFiat(
            tokenFiat = tokenFiat,
            positionUsd = positions.positionMarginUsdTotal(),
            fiatRate = fiatRate,
        )
        val tokenBtc = privacyTokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.btc() }
        val totalBtc = privacyTokens
            .find { it.assetId == Constants.ChainId.BITCOIN_CHAIN_ID }
            ?.priceUsd
            ?.toBigDecimalOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?.let { bitcoinPriceUsd ->
                totalFiat.divide(fiatRate, 16, RoundingMode.HALF_UP)
                    .divide(bitcoinPriceUsd, 16, RoundingMode.HALF_UP)
            } ?: tokenBtc
        return WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            cards = listOf(WalletHomeCardType.BALANCE),
            fiatTotal = totalFiat.numberFormat2(),
            tokenFiatTotal = tokenFiat.numberFormat2(),
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            privacyTokens = privacyTokens,
            totalTokenCount = privacyTokens.size,
            pendingIndicator = pendingDisplays.toWalletHomePendingIndicator(),
        )
    }

    private fun buildWeb3State(): WalletHomeState {
        balanceSnapshot?.let { snapshot ->
            if (web3Tokens.isEmpty()) {
                return WalletHomeState(
                    walletType = WalletHomeType.CLASSIC,
                    cards = listOf(WalletHomeCardType.BALANCE),
                    fiatTotal = snapshot.fiatTotal,
                    tokenFiatTotal = snapshot.tokenFiatTotal,
                    btcTotal = snapshot.btcTotal,
                    fiatSymbol = snapshot.fiatSymbol,
                    totalTokenCount = snapshot.totalTokenCount,
                    isWatchWallet = wallet?.isWatch() == true,
                    importKeyAction = importKeyAction,
                    pendingIndicator = walletHomePendingTransactionIndicator(pendingTxCount) ?: pendingDisplays.toWalletHomePendingIndicator(),
                    hideActions = shouldHideWeb3Actions(),
                )
            }
        }
        val totalFiat = web3Tokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.fiat() }
        val totalBtc = bitcoinPriceUsd?.let { priceUsd ->
            totalFiat
                .divide(BigDecimal(Fiats.getRate()), 16, RoundingMode.HALF_UP)
                .divide(priceUsd, 16, RoundingMode.HALF_UP)
        } ?: BigDecimal.ZERO
        return WalletHomeState(
            walletType = WalletHomeType.CLASSIC,
            cards = listOf(WalletHomeCardType.BALANCE),
            fiatTotal = totalFiat.numberFormat2(),
            tokenFiatTotal = totalFiat.numberFormat2(),
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            web3Tokens = web3Tokens,
            totalTokenCount = web3Tokens.size,
            isWatchWallet = wallet?.isWatch() == true,
            importKeyAction = importKeyAction,
            pendingIndicator = walletHomePendingTransactionIndicator(pendingTxCount) ?: pendingDisplays.toWalletHomePendingIndicator(),
            hideActions = shouldHideWeb3Actions(),
        )
    }

    private fun shouldHideWeb3Actions(): Boolean {
        val currentWallet = wallet ?: return false
        return currentWallet.isWatch() ||
            (currentWallet.isImported() && !currentWallet.hasLocalPrivateKey) ||
            (currentWallet.isClassic() && !currentWallet.hasLocalPrivateKey)
    }

    private fun getWalletName(): String {
        return if (walletType == WalletHomeType.PRIVACY) {
            getString(R.string.Privacy_Wallet)
        } else {
            wallet?.name?.takeIf { it.isNotBlank() } ?: if (wallet?.isWatch() == true) {
                getString(R.string.Watch_Wallet)
            } else {
                getString(R.string.Common_Wallet)
            }
        }
    }

    private fun updateTitle() {
        val title = getString(R.string.wallet_home_tokens)
        val subtitle = getWalletName()
        val icon = getWalletIcon()
        if (icon != null) {
            binding.titleView.setSubTitle(title, subtitle, icon)
        } else {
            binding.titleView.setSubTitle(title, subtitle)
        }
        binding.titleView.setWalletNameSubTitleStyle()
    }

    private fun getWalletIcon(): Int? {
        return if (walletType == WalletHomeType.PRIVACY) {
            R.drawable.ic_wallet_privacy
        } else {
            when {
                wallet?.isMixinSafe() == true -> R.drawable.ic_wallet_safe
                wallet?.isWatch() == true && wallet?.hasLocalPrivateKey != true -> R.drawable.ic_wallet_watch
                else -> null
            }
        }
    }

    private suspend fun showWeb3PendingTransactionDetail(transaction: Web3TransactionItem): Boolean {
        val token = web3ViewModel.web3TokenItemById(walletId, transaction.getMainAssetId()) ?: return false
        val wallet = web3ViewModel.findWalletById(walletId) ?: return false
        if (!isAdded) return false
        activity?.addFragment(
            this@WalletHomeAllTokensFragment,
            Web3TransactionFragment().withArgs {
                putParcelable(Web3TransactionFragment.ARGS_TRANSACTION, transaction)
                putString(Web3TransactionFragment.ARGS_CHAIN, transaction.chainId)
                putParcelable(Web3TransactionsFragment.ARGS_TOKEN, token)
                putParcelable(Web3TransactionFragment.ARGS_WALLET, wallet.toWeb3Wallet())
            },
            Web3TransactionFragment.TAG,
            name = Web3TransactionsFragment.TAG,
        )
        return true
    }

    private suspend fun showPendingDepositDetail(pendingDisplay: PendingDisplay): Boolean {
        val snapshot = walletViewModel.getPendingSnapshot(pendingDisplay.assetId) ?: return false
        val token = walletViewModel.simpleAssetItem(snapshot.assetId) ?: return false
        if (!isAdded) return false
        activity?.addFragment(
            this@WalletHomeAllTokensFragment,
            TransactionFragment.newInstance(snapshot, token),
            TransactionFragment.TAG,
        )
        return true
    }

    private val callbacks = object : WalletHomeCallbacks {
        override fun onAddWalletClicked() = Unit
        override fun onBannerClosed() = Unit
        override fun onReferralClicked() = Unit
        override fun onReferralClosed() = Unit
        override fun onCashClicked() = Unit
        override fun onSupportClicked() = Unit
        override fun onHelpCenterClicked() = Unit
        override fun onBuyClicked() {
            if (walletType == WalletHomeType.CLASSIC) {
                if (shouldHideWeb3Actions()) return
                WalletActivity.showBuy(
                    requireActivity(),
                    true,
                    null,
                    null,
                    walletId = walletId,
                    source = TradeSource.TOKEN_LIST,
                )
            } else {
                WalletActivity.showBuy(requireActivity(), false, null, null, source = TradeSource.TOKEN_LIST)
            }
        }

        override fun onReceiveClicked() {
            if (walletType == WalletHomeType.CLASSIC) {
                if (shouldHideWeb3Actions()) return
                if (showRecoveryReminderForRiskAction { showWeb3ReceiveAssetList() }) return
                showWeb3ReceiveAssetList()
            } else {
                if (showRecoveryReminderForRiskAction { showPrivacyReceiveAssetList() }) return
                showPrivacyReceiveAssetList()
            }
        }

        override fun onSendClicked() {
            if (walletType == WalletHomeType.CLASSIC) {
                if (shouldHideWeb3Actions()) return
                if (showRecoveryReminderForRiskAction { showWeb3SendAssetList() }) return
                showWeb3SendAssetList()
            } else {
                if (showRecoveryReminderForRiskAction { showPrivacySendAssetList() }) return
                showPrivacySendAssetList()
            }
        }

        override fun onSwapClicked() {
            if (walletType == WalletHomeType.CLASSIC) {
                if (shouldHideWeb3Actions()) return
                if (showRecoveryReminderForRiskAction { showWeb3Swap() }) return
                showWeb3Swap()
            } else {
                if (showRecoveryReminderForRiskAction { showPrivacySwap() }) return
                showPrivacySwap()
            }
        }
        override fun onPendingIndicatorClicked() {
            if (walletType == WalletHomeType.CLASSIC && pendingTxCount > 0) {
                lifecycleScope.launch {
                    val pendingTransactions = web3ViewModel.getPendingTransactionItems(walletId)
                    if (pendingTransactions.size == 1 && showWeb3PendingTransactionDetail(pendingTransactions.first())) {
                        return@launch
                    }
                    if (!isAdded) return@launch
                    WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(walletId = walletId), pendingType = true)
                }
                return
            }
            if (pendingDisplays.size == 1) {
                lifecycleScope.launch {
                    if (showPendingDepositDetail(pendingDisplays[0])) {
                        return@launch
                    }
                    if (!isAdded) return@launch
                    WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions, true)
                }
            } else if (pendingDisplays.size > 1) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions, true)
            }
        }

        override fun onWatchIndicatorClicked() = Unit
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
        override fun onViewMoreTokensClicked() = Unit
        override fun onAllTokensBackClicked() {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        override fun onViewMoreTransactionsClicked() = Unit
        override fun onViewMorePositionsClicked() = Unit

        override fun onTokenClicked(index: Int) {
            if (walletType == WalletHomeType.PRIVACY) {
                privacyTokens.getOrNull(index)?.let {
                    WalletActivity.showWithToken(
                        requireActivity(),
                        it,
                        WalletActivity.Destination.Transactions,
                        source = AnalyticsTracker.AssetSource.TOKEN_LIST,
                    )
                }
            } else {
                web3Tokens.getOrNull(index)?.let { token ->
                    lifecycleScope.launch {
                        val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                        WalletActivity.showWithWeb3Token(
                            requireActivity(),
                            token,
                            address?.destination,
                            WalletActivity.Destination.Web3Transactions,
                        )
                    }
                }
            }
        }

        override fun onTransactionClicked(index: Int) = Unit
        override fun onTransactionUserClicked(userId: String) = Unit
        override fun onPositionClicked(index: Int) = Unit
        override fun onTopMoverClicked(index: Int) {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Market)
        }
    }

    private fun showPrivacySendAssetList() {
        TokenListBottomSheetDialogFragment.newInstance(TokenListBottomSheetDialogFragment.TYPE_FROM_SEND)
            .setOnAssetClick {
                WalletActivity.navigateToWalletActivity(requireActivity(), it)
            }.setOnDepositClick {
                // do nothing
            }
            .showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showPrivacyReceiveAssetList() {
        TokenListBottomSheetDialogFragment.newInstance(TokenListBottomSheetDialogFragment.TYPE_FROM_RECEIVE)
            .setOnAssetClick { asset ->
                WalletActivity.showWithToken(requireActivity(), asset, WalletActivity.Destination.Deposit)
            }.showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showPrivacySwap() {
        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.TOKEN_LIST)
        SwapActivity.show(
            requireActivity(),
            inMixin = true,
            entrySource = TradeSource.TOKEN_LIST,
            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
        )
    }

    private fun showWeb3SendAssetList() {
        Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, WEB3_TYPE_FROM_SEND).apply {
            setOnClickListener { token ->
                this@WalletHomeAllTokensFragment.lifecycleScope.launch {
                    val walletValue = web3ViewModel.findWalletById(walletId)?.toWeb3Wallet()
                    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                    val chain = web3ViewModel.web3TokenItemById(token.walletId, token.chainId)
                    if (walletValue == null || chain == null) {
                        toast(R.string.Data_error)
                        return@launch
                    }
                    WalletActivity.navigateToWalletActivity(this@WalletHomeAllTokensFragment.requireActivity(), address?.destination, token, chain, walletValue)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showWeb3ReceiveAssetList() {
        Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, WEB3_TYPE_FROM_RECEIVE).apply {
            setOnClickListener { token ->
                this@WalletHomeAllTokensFragment.lifecycleScope.launch {
                    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)
                    WalletActivity.showWithAddress(this@WalletHomeAllTokensFragment.requireActivity(), address?.destination, token, WalletActivity.Destination.Address)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showWeb3Swap() {
        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.TOKEN_LIST)
        SwapActivity.show(
            requireActivity(),
            inMixin = false,
            walletId = walletId,
            entrySource = TradeSource.TOKEN_LIST,
            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
        )
    }

    private fun showRecoveryReminderForRiskAction(onContinue: () -> Unit): Boolean {
        return RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager, onContinue)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
