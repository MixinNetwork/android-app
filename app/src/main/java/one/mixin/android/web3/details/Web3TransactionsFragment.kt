@file:Suppress("DEPRECATION")

package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RadioGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.databinding.FragmentWeb3TransactionsBinding
import one.mixin.android.databinding.ItemAssetAllocationBinding
import one.mixin.android.databinding.ViewWalletWeb3TokenBottomBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TokenGroup
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.db.web3.vo.solLamportToAmount
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.extension.buildBalanceAmountSymbol
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.setQuoteText
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.job.RefreshPriceJob
import one.mixin.android.job.RefreshWeb3TokenJob
import one.mixin.android.tip.Tip
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.stake.StakeFragment
import one.mixin.android.ui.home.web3.stake.StakingFragment
import one.mixin.android.ui.home.web3.stake.ValidatorsFragment
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.wallet.AllWeb3TransactionsFragment
import one.mixin.android.ui.wallet.ImportKeyBottomSheetDialogFragment
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_ASSET_ID
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET_SOURCE
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.ui.wallet.Web3FilterParams.Companion.FILTER_GOOD_AND_SPAM
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.web3.isNativeSolAsset
import one.mixin.android.web3.swap.showTokenNetworks
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DebugClickListener
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class Web3TransactionsFragment : BaseFragment(R.layout.fragment_web3_transactions), OnSnapshotListener, ViewTreeObserver.OnScrollChangedListener {
    companion object {
        const val TAG = "Web3TransactionsFragment"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_ADDRESS = "args_address"
        const val ARGS_NETWORK = "args_network"

        fun newInstance(
            address: String,
            web3Token: Web3TokenItem,
            network: String? = null,
        ) =
            Web3TransactionsFragment().withArgs {
                putString(ARGS_ADDRESS, address)
                putParcelable(ARGS_TOKEN, web3Token)
                putString(ARGS_NETWORK, network)
            }
    }

    private val binding by viewBinding(FragmentWeb3TransactionsBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var _bottomBinding: ViewWalletWeb3TokenBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var tip: Tip

    private val initialAddress: String? by lazy {
        requireArguments().getString(ARGS_ADDRESS)
    }

    private val initialToken: Web3TokenItem by lazy {
        requireNotNull(requireArguments().getParcelable<Web3TokenItem>(ARGS_TOKEN) ?: requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java))
    }

    private var groupedTokens: List<Web3TokenItem> = emptyList()
    private var selectedNetwork: String? = null
    private var networkIds: List<String> = emptyList()
    private var addresses: Map<String, String> = emptyMap()
    private val selectedTokens: List<Web3TokenItem>
        get() = groupedTokens.filter { selectedNetwork == null || it.chainId == selectedNetwork }.ifEmpty { listOf(initialToken) }
    private val token: Web3TokenItem get() = selectedTokens.first()
    private val address: String? get() = addresses[token.chainId] ?: initialAddress.takeIf { token.chainId == initialToken.chainId }
    private val transactionAssetIds = MutableLiveData<List<String>>()
    private var stakeAssetId: String? = null

    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupedTokens = listOf(initialToken)
        selectedNetwork = if (savedInstanceState != null) savedInstanceState.getString(ARGS_NETWORK)
            else requireArguments().getString(ARGS_NETWORK)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARGS_NETWORK, selectedNetwork)
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val wallet = web3ViewModel.findWalletById(token.walletId)
            binding.sendReceiveView.isVisible = wallet?.isWatch() != true
            binding.empty.isVisible = wallet?.isWatch() == true
        }

        jobManager.addJobInBackground(RefreshPriceJob(token.assetId))
        jobManager.addJobInBackground(RefreshMarketJob(initialToken.assetId))
        refreshToken(token.assetId)
        binding.titleView.apply {
            val sub = getChainName(token.chainId, token.chainName, token.assetKey)
            if (sub != null)
                setSubTitle(token.name, sub)
            else
                titleTv.setTextOnly(token.name)
            leftIb.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            rightIb.setOnClickListener {
                showBottom()
            }

            binding.apply {
                value.text = try {
                    if (token.priceFiat().toFloat() == 0f) {
                        getString(R.string.NA)
                    } else {
                        "${Fiats.getSymbol()}${token.priceFiat().priceFormat()}"
                    }
                } catch (ignored: NumberFormatException) {
                    "${Fiats.getSymbol()}${token.priceFiat().priceFormat()}"
                }
                spamLl.isVisible = token.isSpam()
                web3ViewModel.marketById(token.assetId).observe(viewLifecycleOwner) { market ->
                    if (market != null) {
                        val priceChangePercentage24H = BigDecimal(market.priceChangePercentage24H)
                        val isRising = priceChangePercentage24H >= BigDecimal.ZERO
                        rise.setQuoteText(
                            "${(priceChangePercentage24H).numberFormat2()}%",
                            isRising
                        )
                    } else if (token.priceUsd == "0") {
                        rise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        rise.text = "0.00%"
                    } else if (token.changeUsd.isNotEmpty()) {
                        val changeUsd = BigDecimal(token.changeUsd)
                        val isRising = changeUsd >= BigDecimal.ZERO
                        rise.setQuoteText(
                            "${(changeUsd * BigDecimal(100)).numberFormat2()}%",
                            isRising
                        )
                    }
                }
                transactionsRv.listener = this@Web3TransactionsFragment
                bottomCard.post {
                    bottomCard.isVisible = true
                    val remainingHeight =
                        requireContext().screenHeight() - requireContext().statusBarHeight() - requireContext().navigationBarHeight() - titleView.height - topLl.height - marketRl.height - binding.networkTabsScroll.height - binding.allocationCard.height - 70.dp
                    bottomRl.updateLayoutParams {
                        height = remainingHeight.coerceAtLeast(0)
                    }

                    if (scrollY > 0) {
                        scrollView.isInvisible = true
                        scrollView.postDelayed(
                            {
                                scrollView.scrollTo(0, scrollY)
                                scrollView.isInvisible = false
                            }, 1
                        )
                    }
                }

                sendReceiveView.send.setOnClickListener {
                    chooseToken { token, address ->
                        lifecycleScope.launch {
                            val chain = web3ViewModel.web3TokenItemById(token.walletId, token.chainId)
                            val wallet = web3ViewModel.findWalletById(token.walletId)
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), token.chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val chainResume = web3ViewModel.web3TokenItemById(token.walletId, token.chainId)
                                        val walletResume = web3ViewModel.findWalletById(token.walletId)
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), token.chainId)) return@launch
                                        if (chainResume == null) {
                                            refreshToken(token.chainId, address)
                                            toast(R.string.Please_wait_a_bit)
                                        } else {
                                            requireView().navigate(
                                                R.id.action_web3_transactions_to_transfer_destination,
                                                Bundle().apply {
                                                    address?.let {
                                                        putString(TransferDestinationInputFragment.ARGS_ADDRESS, it)
                                                    }
                                                    putParcelable(TransferDestinationInputFragment.ARGS_WALLET, walletResume?.toWeb3Wallet())
                                                    putParcelable(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token)
                                                    putParcelable(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chainResume)
                                                }
                                            )
                                        }
                                    }
                                }
                            ) {
                                return@launch
                            }
                            if (chain == null) {
                                refreshToken(token.chainId, address)
                                toast(R.string.Please_wait_a_bit)
                            } else {
                                requireView().navigate(
                                    R.id.action_web3_transactions_to_transfer_destination,
                                    Bundle().apply {
                                        address?.let {
                                            putString(TransferDestinationInputFragment.ARGS_ADDRESS, it)
                                        }
                                        putParcelable(TransferDestinationInputFragment.ARGS_WALLET, wallet?.toWeb3Wallet())
                                        putParcelable(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token)
                                        putParcelable(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chain)
                                    }
                                )
                            }
                        }
                    }
                }
                sendReceiveView.receive.setOnClickListener {
                    chooseToken { token, address ->
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(token.walletId)
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), token.chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val walletResume = web3ViewModel.findWalletById(token.walletId)
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), token.chainId)) return@launch
                                        requireView().navigate(
                                            R.id.action_web3_transactions_to_web3_address,
                                            Bundle().apply {
                                                address?.let {
                                                    putString("address", it)
                                                }
                                                putParcelable("web3_token", token)
                                            }
                                        )
                                    }
                                }
                            ) {
                                return@launch
                            }
                            requireView().navigate(
                                R.id.action_web3_transactions_to_web3_address,
                                Bundle().apply {
                                    address?.let {
                                        putString("address", it)
                                    }
                                    putParcelable("web3_token", token)
                                }
                            )
                        }
                    }
                }
                sendReceiveView.swap.setOnClickListener {
                    chooseToken { token, address ->
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(token.walletId)
                            if (showImportKeyReminderIfNeeded(wallet?.toWeb3Wallet(), token.chainId)) return@launch
                            if (
                                showRecoveryReminderForRiskAction {
                                    lifecycleScope.launch {
                                        val walletResume = web3ViewModel.findWalletById(token.walletId)
                                        if (showImportKeyReminderIfNeeded(walletResume?.toWeb3Wallet(), token.chainId)) return@launch
                                        AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.ASSET_DETAIL)
                                        requireView().navigate(
                                            R.id.action_web3_transactions_to_swap,
                                            Bundle().apply {
                                                putString(TradeFragment.ARGS_INPUT, token.assetId)
                                                putBoolean(TradeFragment.ARGS_IN_MIXIN, false)
                                                putString(TradeFragment.ARGS_WALLET_ID, token.walletId)
                                            }
                                        )
                                    }
                                }
                            ) {
                                return@launch
                            }
                            AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.ASSET_DETAIL)
                            requireView().navigate(
                                R.id.action_web3_transactions_to_swap,
                                Bundle().apply {
                                    putString(TradeFragment.ARGS_INPUT, token.assetId)
                                    putBoolean(TradeFragment.ARGS_IN_MIXIN, false)
                                    putString(TradeFragment.ARGS_WALLET_ID, token.walletId)
                                }
                            )
                        }
                    }
                }

                transactionsTitleLl.setOnClickListener {
                    view.navigate(
                        R.id.action_web3_transactions_to_all_web3_transactions,
                        Bundle().apply {
                            putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams(walletId = token.walletId, level = if (token.isSpam()) FILTER_GOOD_AND_SPAM else Web3FilterParams.FILTER_GOOD_ONLY, tokenItems = selectedTokens))
                        }
                    )
                }
                marketRl.setOnClickListener {
                    lifecycleScope.launch {
                        var market = web3ViewModel.findMarketItemByAssetId(token.assetId)
                        if (market == null) {
                            jobManager.addJobInBackground(RefreshMarketJob(token.assetId))
                            market = MarketItem(
                                "", token.name, token.symbol, token.iconUrl, token.priceUsd,
                                "", "", "", "", "", runCatching {
                                    (BigDecimal(token.priceUsd) * BigDecimal(token.changeUsd)).toPlainString()
                                }.getOrNull() ?: "0", "", token.changeUsd, "", "", "", "", "", "", "", "", "",
                                "", "", "", "", listOf(token.assetId), "", "", null
                            )
                        }
                        view.navigate(
                            R.id.action_web3_transactions_to_market_details,
                            Bundle().apply {
                                putParcelable(ARGS_MARKET, market)
                                putString(ARGS_ASSET_ID, token.assetId)
                                putString(ARGS_MARKET_SOURCE, AnalyticsTracker.MarketSource.TOKEN_DETAIL)
                            },
                        )
                    }
                }
                marketView.setContent {
                    Market(token.assetId)
                }
            }
        }

        var hasScrolled = false
        val offset = web3ViewModel.scrollOffset

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener(this@Web3TransactionsFragment)
        transactionAssetIds.switchMap { ids ->
            web3ViewModel.web3Transactions(initialToken.walletId, ids)
        }.observe(viewLifecycleOwner) { list ->
            binding.transactionsRv.isVisible = list.isNotEmpty()
            binding.bottomRl.isVisible = list.isEmpty()
            binding.transactionsRv.list = list
            if (!hasScrolled && isAdded) {
                hasScrolled = true
                binding.scrollView.post { binding.scrollView.scrollTo(0, offset) }
            }
        }
        web3ViewModel.groupedTokenItems(initialToken.walletId, initialToken.assetId).observe(viewLifecycleOwner) { tokens ->
            groupedTokens = tokens.filter { (it.hidden == true) == (initialToken.hidden == true) }.ifEmpty { listOf(initialToken) }
            if (selectedNetwork != null && groupedTokens.none { it.chainId == selectedNetwork }) selectedNetwork = null
            renderSelection()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            addresses = web3ViewModel.getAddresses(initialToken.walletId).associate { it.chainId to it.destination }
            renderSelection()
        }
        renderSelection()
    }

    private fun renderSelection() {
        val chains = groupedTokens.distinctBy { it.chainId }.sortedBy { it.getChainDisplayName() }
        binding.networkTabsScroll.isVisible = chains.size > 1
        val ids = chains.map { it.chainId }
        if (networkIds != ids || binding.networkTabs.childCount == 0) {
            networkIds = ids
            binding.networkTabs.setOnCheckedChangeListener(null)
            binding.networkTabs.removeAllViews()
            val tabs = listOf(null to getString(R.string.All)) + chains.map { it.chainId to it.getChainDisplayName() }
            tabs.forEach { (chainId, name) ->
                binding.networkTabs.addView(AppCompatRadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    tag = chainId
                    text = name
                    buttonDrawable = null
                    setBackgroundResource(R.drawable.selector_radio)
                    setTextColor(ContextCompat.getColorStateList(requireContext(), R.drawable.radio_button_text_selector))
                    textSize = 14f
                    minHeight = 0
                    minimumHeight = 0
                    setPadding(16.dp, 9.dp, 16.dp, 9.dp)
                    layoutParams = RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = 12.dp }
                })
            }
        }
        binding.networkTabs.setOnCheckedChangeListener(null)
        for (index in 0 until binding.networkTabs.childCount) {
            val button = binding.networkTabs.getChildAt(index) as AppCompatRadioButton
            button.isChecked = button.tag == selectedNetwork
        }
        binding.networkTabs.setOnCheckedChangeListener { group, checkedId ->
            val button = group.findViewById<AppCompatRadioButton>(checkedId) ?: return@setOnCheckedChangeListener
            selectedNetwork = button.tag as? String
            renderSelection()
        }
        binding.allocationCard.isVisible = selectedTokens.size > 1
        binding.allocationRows.removeAllViews()
        if (binding.allocationCard.isVisible) {
            selectedTokens.sortedByDescending { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO }.forEach { asset ->
                val row = ItemAssetAllocationBinding.inflate(layoutInflater, binding.allocationRows, false)
                row.avatar.loadToken(asset)
                row.symbol.text = asset.symbol
                row.network.text = getString(R.string.asset_on_network, asset.getChainDisplayName())
                row.value.text = "${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                row.amount.text = "${asset.balance.numberFormat()} ${asset.symbol}"
                row.root.setOnClickListener {
                    selectedNetwork = asset.chainId
                    renderSelection()
                }
                binding.allocationRows.addView(row.root)
            }
        }
        val selectedIds = selectedTokens.map { it.assetId }.sorted()
        if (transactionAssetIds.value != selectedIds) {
            binding.transactionsRv.list = emptyList()
            transactionAssetIds.value = selectedIds
        }
        binding.titleView.setSubTitle(token.name, if (selectedNetwork == null && groupedTokens.size > 1) getString(R.string.Common_Wallet) else token.getChainDisplayName())
        updateHeader(token)
        val stakeToken = selectedTokens.singleOrNull()?.takeIf { it.isNativeSolAsset() }
        val stakeKey = stakeToken?.let { "${it.assetId}:$address" }
        binding.stake.root.isVisible = stakeToken != null && stakeAssetId == stakeKey && stakeAccounts != null
        if (stakeAssetId != stakeKey) {
            stakeAssetId = stakeKey
            stakeAccounts = null
            if (stakeToken != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val wallet = web3ViewModel.findWalletById(stakeToken.walletId)
                    if (stakeAssetId != stakeKey) return@launch
                    if (wallet != null && (wallet.category == WalletCategory.CLASSIC.value || (wallet.isImported() && wallet.hasLocalPrivateKey))) {
                        binding.stake.root.isVisible = true
                        address?.let { getStakeAccounts(it) }
                    }
                }
            }
        }
    }

    private fun chooseToken(action: (Web3TokenItem, String?) -> Unit) {
        fun select(asset: Web3TokenItem) {
            viewLifecycleOwner.lifecycleScope.launch {
                val latest = web3ViewModel.web3TokenItemById(asset.walletId, asset.assetId) ?: asset
                val destination = web3ViewModel.getAddressesByChainId(asset.walletId, asset.chainId)?.destination
                    ?: initialAddress.takeIf { asset.chainId == initialToken.chainId }
                action(latest, destination)
            }
        }
        val assets = selectedTokens
        showTokenNetworks(assets.map { it.toSwapToken() }) { selected ->
            assets.firstOrNull { it.walletId == selected.walletId && it.assetId == selected.assetId }?.let(::select)
        }
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        _bottomBinding = ViewWalletWeb3TokenBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(
                    requireActivity(),
                    R.style.Custom
                ), R.layout.view_wallet_web3_token_bottom, null
            )
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            title.text = token.name
            addressTv.text = token.assetKey
            addressTv.isVisible = selectedTokens.size == 1
            explorer.setOnClickListener {
                chooseToken { token, _ ->
                    val url = "${Constants.API.URL}external/explore/${token.chainId}/assets/${token.assetKey}"
                    context?.openUrl(url)
                }
                bottomSheet.dismiss()
            }
            stakeSolTv.isVisible = selectedTokens.size == 1 && token.isNativeSolAsset() && binding.stake.root.isVisible
            stakeSolTv.setOnClickListener {
                this@Web3TransactionsFragment.navTo(
                    ValidatorsFragment.newInstance().apply {
                        setOnSelect { v ->
                            this@Web3TransactionsFragment.navTo(
                                StakeFragment.newInstance(
                                    v,
                                    token.balance
                                ), StakeFragment.TAG
                            )
                        }
                    }, ValidatorsFragment.TAG
                )
                bottomSheet.dismiss()
            }
            copy.setOnClickListener {
                chooseToken { token, _ ->
                    context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, token.assetKey))
                    toast(R.string.copied_to_clipboard)
                }
                bottomSheet.dismiss()
            }
            
            hide.setText(if (token.hidden == true) R.string.Show else R.string.Hide)
            hide.setOnClickListener {
                val hidden = token.hidden != true
                AnalyticsTracker.trackAssetVisibility(hidden, TradeWallet.WEB3, AnalyticsTracker.AssetSource.ASSET_DETAIL)
                lifecycleScope.launch(Dispatchers.IO) {
                    web3ViewModel.updateTokenHidden(token.assetId, token.walletId, hidden)
                }
                bottomSheet.dismiss()
                mainThreadDelayed({ activity?.onBackPressedDispatcher?.onBackPressed() }, 200)
            }
            
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }


    private fun updateHeader(asset: Web3TokenItem) {
        binding.apply {
            val group = Web3TokenGroup(selectedTokens)
            val amount = group.balance.toPlainString()
            val amountText =
                try {
                    if (amount.toFloat() == 0f) {
                        "0.00"
                    } else {
                        amount.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    amount.numberFormat()
                }
            val color = requireContext().colorFromAttribute(R.attr.text_primary)
            balance.text = buildBalanceAmountSymbol(requireContext(), amountText, asset.symbol, color, color)
            balanceAs.text =
                try {
                    if (group.fiat.toFloat() == 0f) {
                        "≈ ${Fiats.getSymbol()}0.00"
                    } else {
                        "≈ ${Fiats.getSymbol()}${group.fiat.numberFormat2()}"
                    }
                } catch (ignored: NumberFormatException) {
                    "≈ ${Fiats.getSymbol()}${group.fiat.numberFormat2()}"
                }
            avatar.loadToken(asset)
            avatar.badge.isVisible = selectedNetwork != null || groupedTokens.size == 1
            avatar.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                        if (token.chainId !in Constants.Web3UtxoChainIds) return
                        val fromAddress: String = address ?: return
                        requireView().navigate(
                            R.id.action_web3_transactions_to_web3_btc_outputs,
                            Bundle().apply {
                                putString(Web3BtcOutputsFragment.ARGS_WALLET_ID, token.walletId)
                                putString(Web3BtcOutputsFragment.ARGS_ADDRESS, fromAddress)
                                putString(Web3BtcOutputsFragment.ARGS_CHAIN_ID, token.chainId)
                            },
                        )
                    }

                    override fun onSingleClick() {
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshJob = PendingTransactionRefreshHelper.startRefreshData(
            fragment = this,
            web3ViewModel = web3ViewModel,
            jobManager = jobManager,
            refreshJob = refreshJob
        )
    }

    override fun onPause() {
        super.onPause()
        refreshJob = PendingTransactionRefreshHelper.cancelRefreshData(refreshJob)
    }

    private suspend fun getStakeAccounts(address: String) {
        val stakeAccounts = web3ViewModel.getStakeAccounts(address)
        if (!isAdded || selectedTokens.size != 1 || this.address != address) { return }
        
        this.stakeAccounts = stakeAccounts.orEmpty()
        if (stakeAccounts.isNullOrEmpty()) {
            updateStake(StakeAccountSummary(0, "0"))
            return
        }

        var amount: Long = 0
        var count = 0
        stakeAccounts.forEach { a ->
            count++
            amount += (a.account.data.parsed.info.stake.delegation.stake.toLongOrNull() ?: 0)
        }

        val amountStr = amount.solLamportToAmount().stripTrailingZeros().toPlainString()
        
        val stakeAccountSummary = StakeAccountSummary(count, amountStr)
        
        updateStake(stakeAccountSummary)
    }

    var stakeAccounts: List<StakeAccount>? = null

    private fun updateStake(stakeAccountSummary: StakeAccountSummary?) {
        binding.stake.apply {
            if (stakeAccountSummary == null) {
                iconVa.displayedChild = 0
                amountTv.text = "0 SOL"
                countTv.text = "0 account"
            } else {
                iconVa.displayedChild = 1
                amountTv.text = "${stakeAccountSummary.amount} SOL"
                countTv.text = "${stakeAccountSummary.count} account"
                stakeRl.setOnClickListener {
                    navTo(StakingFragment.newInstance(ArrayList(stakeAccounts ?: emptyList()), token.balance), StakingFragment.TAG)
                }
            }
        }
    }

    private fun refreshToken(assetId: String, destination: String? = address) {
        jobManager.addJobInBackground(RefreshWeb3TokenJob(null, assetId, destination))
    }

    private fun showRecoveryReminderForRiskAction(onContinue: (() -> Unit)? = null): Boolean {
        return RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager, onContinue)
    }

    private fun showImportKeyReminderIfNeeded(wallet: Web3Wallet?, chainId: String = token.chainId): Boolean {
        if (wallet?.isImported() != true || wallet.hasLocalPrivateKey) return false
        ImportKeyBottomSheetDialogFragment.newInstance(
            if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) {
                ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase
            } else {
                ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey
            },
            walletId = wallet.id,
            chainId = chainId,
        ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
        return true
    }

    override fun <T> onNormalItemClick(item: T) {
        item as Web3TransactionItem
        lifecycleScope.launch {
            val wallet = web3ViewModel.findWalletById(initialToken.walletId)
            val transactionToken = groupedTokens.firstOrNull {
                it.chainId == item.chainId && (it.assetId == item.sendAssetId || it.assetId == item.receiveAssetId)
            } ?: token
            val bundle = Bundle().apply {
                putParcelable(Web3TransactionFragment.ARGS_TRANSACTION, item)
                putParcelable(ARGS_TOKEN, transactionToken)
                putParcelable(Web3TransactionFragment.ARGS_WALLET, wallet?.toWeb3Wallet())
            }
            findNavController().navigate(
                R.id.action_web3_transactions_to_web3_transaction,
                bundle
            )
        }
    }

    override fun onUserClick(userId: String) {
    }

    override fun onMoreClick() {
        requireView().navigate(
            R.id.action_web3_transactions_to_all_web3_transactions,
            Bundle().apply {
                putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams(walletId = token.walletId, level = if (token.isSpam()) FILTER_GOOD_AND_SPAM else Web3FilterParams.FILTER_GOOD_ONLY, tokenItems = selectedTokens))
            }
        )
    }

    override fun onScrollChanged() {
        if (isAdded) web3ViewModel.scrollOffset = binding.scrollView.scrollY
    }
}
