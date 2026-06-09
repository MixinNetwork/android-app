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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.isClassic
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isMixinSafe
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.databinding.FragmentWalletHomeAllTokensBinding
import one.mixin.android.databinding.ViewClassicWalletBottomBinding
import one.mixin.android.databinding.ViewPrivacyWalletBottomBinding
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyAction
import one.mixin.android.ui.wallet.home.WalletHomeImportKeyKind
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.formatWalletHomeBtcTotal
import one.mixin.android.ui.wallet.home.components.WalletHomeAllTokensPage
import one.mixin.android.ui.wallet.home.walletHomeImportKeyAction
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE as WEB3_TYPE_FROM_RECEIVE
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND as WEB3_TYPE_FROM_SEND
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class WalletHomeAllTokensFragment : BaseFragment() {
    companion object {
        const val ARGS_WALLET_TYPE = "args_wallet_type"
        const val ARGS_WALLET_ID = "args_wallet_id"
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val walletType by lazy {
        WalletHomeType.valueOf(requireArguments().getString(ARGS_WALLET_TYPE) ?: WalletHomeType.PRIVACY.name)
    }
    private val walletId by lazy { requireArguments().getString(ARGS_WALLET_ID).orEmpty() }
    private val _walletId = MutableLiveData<String>()
    private val homeState = MutableStateFlow(WalletHomeState(walletType = WalletHomeType.PRIVACY))
    private var privacyTokens: List<TokenItem> = emptyList()
    private var web3Tokens: List<Web3TokenItem> = emptyList()
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
            lifecycleScope.launch {
                val tokens = walletViewModel.assetItemsNotHiddenRaw()
                if (tokens.isNotEmpty()) {
                    privacyTokens = tokens
                    renderHome()
                }
            }
            walletViewModel.assetItemsNotHidden().observe(viewLifecycleOwner) { tokens ->
                privacyTokens = tokens
                renderHome()
            }
        } else {
            _walletId.value = walletId
            lifecycleScope.launch {
                val tokens = web3ViewModel.web3TokensExcludeHiddenRaw(walletId)
                if (tokens.isNotEmpty()) {
                    web3Tokens = tokens
                    renderHome()
                }
            }
            web3TokensLiveData.observe(viewLifecycleOwner) { tokens ->
                web3Tokens = tokens
                renderHome()
            }
        }
        renderHome()
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
        val totalFiat = privacyTokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.fiat() }
        val totalBtc = privacyTokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.btc() }
        return WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            cards = listOf(WalletHomeCardType.BALANCE),
            fiatTotal = totalFiat.numberFormat2(),
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            privacyTokens = privacyTokens,
            totalTokenCount = privacyTokens.size,
        )
    }

    private fun buildWeb3State(): WalletHomeState {
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
            btcTotal = formatWalletHomeBtcTotal(totalBtc),
            fiatSymbol = Fiats.getSymbol(),
            web3Tokens = web3Tokens,
            totalTokenCount = web3Tokens.size,
            isWatchWallet = wallet?.isWatch() == true,
            importKeyAction = importKeyAction,
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

    private val callbacks = object : WalletHomeCallbacks {
        override fun onAddWalletClicked() = Unit
        override fun onBannerClosed() = Unit
        override fun onCashbackBannerClosed() = Unit
        override fun onReferralClicked() = Unit
        override fun onReferralClosed() = Unit
        override fun onSupportClicked() = Unit
        override fun onHelpCenterClicked() = Unit
        override fun onBuyClicked() {
            if (walletType == WalletHomeType.CLASSIC) {
                if (shouldHideWeb3Actions()) return
                WalletActivity.showBuy(requireActivity(), true, null, null, walletId)
            } else {
                WalletActivity.showBuy(requireActivity(), false, null, null)
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
        override fun onPendingIndicatorClicked() = Unit
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
                    WalletActivity.showWithToken(requireActivity(), it, WalletActivity.Destination.Transactions)
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
        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
        SwapActivity.show(
            requireActivity(),
            inMixin = true,
            entrySource = TradeSource.WALLET_HOME,
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
        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.WALLET_HOME)
        SwapActivity.show(
            requireActivity(),
            inMixin = false,
            walletId = walletId,
            entrySource = TradeSource.WALLET_HOME,
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
