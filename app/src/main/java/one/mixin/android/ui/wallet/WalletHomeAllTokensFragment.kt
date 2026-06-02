package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.home.WalletHomeAllTokensPage
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WalletHomeAllTokensPage(
                    state = homeState.collectAsState().value,
                    callbacks = callbacks,
                )
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (walletType == WalletHomeType.PRIVACY) {
            walletViewModel.assetItemsNotHidden().observe(viewLifecycleOwner) { tokens ->
                privacyTokens = tokens
                renderHome()
            }
        } else {
            _walletId.value = walletId
            web3TokensLiveData.observe(viewLifecycleOwner) { tokens ->
                web3Tokens = tokens
                renderHome()
            }
        }
        renderHome()
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
            btcTotal = totalBtc.numberFormat8(),
            fiatSymbol = Fiats.getSymbol(),
            privacyTokens = privacyTokens,
            totalTokenCount = privacyTokens.size,
        )
    }

    private fun buildWeb3State(): WalletHomeState {
        val totalFiat = web3Tokens.fold(BigDecimal.ZERO) { acc, item -> acc + item.fiat() }
        return WalletHomeState(
            walletType = WalletHomeType.CLASSIC,
            cards = listOf(WalletHomeCardType.BALANCE),
            fiatTotal = totalFiat.numberFormat2(),
            fiatSymbol = Fiats.getSymbol(),
            web3Tokens = web3Tokens,
            totalTokenCount = web3Tokens.size,
        )
    }

    private val callbacks = object : WalletHomeCallbacks {
        override fun onAddWalletClicked() = Unit
        override fun onBannerClosed() = Unit
        override fun onCashbackBannerClosed() = Unit
        override fun onReferralClicked() = Unit
        override fun onReferralClosed() = Unit
        override fun onSupportClicked() = Unit
        override fun onHelpCenterClicked() = Unit
        override fun onBuyClicked() = Unit
        override fun onReceiveClicked() = Unit
        override fun onSendClicked() = Unit
        override fun onSwapClicked() = Unit
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
        override fun onPositionClicked(index: Int) = Unit
        override fun onTopMoverClicked(index: Int) {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Market)
        }
    }
}
