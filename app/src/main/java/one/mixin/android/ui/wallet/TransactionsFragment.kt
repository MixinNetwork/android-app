package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.Constants.AssetId.XIN_ASSET_ID
import one.mixin.android.Constants.MIXIN_EARN_USER_ID
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.EarnProduct
import one.mixin.android.databinding.FragmentTransactionsBinding
import one.mixin.android.databinding.ItemAssetAllocationBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.extension.buildBalanceAmountSymbol
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigate
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.setQuoteText
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.job.RefreshPriceJob
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_ASSET_ID
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET_SOURCE
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.home.toWalletEarnDetails
import one.mixin.android.ui.wallet.home.tokenAmountText
import one.mixin.android.ui.wallet.home.usdCurrencyAmountText
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.getChainName
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.TokenGroup
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.web3.swap.showTokenNetworks
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DebugClickListener
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : BaseFragment(R.layout.fragment_transactions), OnSnapshotListener {
    companion object {
        const val TAG = "TransactionsFragment"
        const val ARGS_ASSET = "args_asset"
        const val ARGS_FROM_MARKET = "args_from_market"
        const val ARGS_SOURCE = "args_source"
        const val ARGS_NETWORK = "args_network"
        const val ARGS_ASSET_IDS = "args_asset_ids"
    }

    private val binding by viewBinding(FragmentTransactionsBinding::bind)
    private var _bottomBinding: ViewWalletTransactionsBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    lateinit var asset: TokenItem
    private var groupedAssets: List<TokenItem> = emptyList()
    private var selectedNetwork: String? = null
    private val snapshotAssetIds = MutableLiveData<List<String>>()
    private var snapshotJob: Job? = null
    private var networkIds: List<String> = emptyList()
    private val selectedAssets: List<TokenItem>
        get() = groupedAssets.filter { selectedNetwork == null || it.chainId == selectedNetwork }
            .ifEmpty { listOf(asset) }

    private val fromMarket by lazy {
        requireArguments().getBoolean(ARGS_FROM_MARKET, false)
    }
    private val source by lazy {
        requireArguments().getString(ARGS_SOURCE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)!!
        selectedNetwork = if (savedInstanceState != null) savedInstanceState.getString(ARGS_NETWORK)
            else requireArguments().getString(ARGS_NETWORK)
        groupedAssets = listOf(asset)
    }

    private var scrollY = 0

    override fun onPause() {
        super.onPause()
        scrollY = binding.scrollView.scrollY
    }

    override fun onResume() {
        super.onResume()
        refreshEarnDetails()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsTracker.trackAssetDetail(TradeWallet.MAIN, resolveAssetDetailSource(fromMarket, source))
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(asset.assetId))))
        jobManager.addJobInBackground(RefreshPriceJob(asset.assetId))
        jobManager.addJobInBackground(RefreshMarketJob(asset.assetId))

        binding.titleView.apply {
            val sub = getChainName(asset.chainId, asset.chainName, asset.assetKey)
            if (sub != null)
                setSubTitle(asset.name, sub)
            else
                titleTv.setTextOnly(asset.name)
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            rightAnimator.setOnClickListener {
                showBottom()
            }
        }
        binding.apply {
            sendReceiveView.swap.setOnClickListener {
                val swap = {
                    chooseAsset { token ->
                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.ASSET_DETAIL)
                        SwapActivity.show(
                            requireActivity(),
                            inMixin = true,
                            input = token.assetId,
                            output = if (token.assetId == USDT_ASSET_ETH_ID) XIN_ASSET_ID else USDT_ASSET_ETH_ID,
                            entrySource = TradeSource.ASSET_DETAIL,
                            entryType = AnalyticsTracker.SpotTradeType.SIMPLE,
                        )
                    }
                }
                if (!showRecoveryReminderForRiskAction(swap)) swap()
            }
            value.text = try {
                if (asset.priceFiat().toFloat() == 0f) {
                    getString(R.string.NA)
                } else {
                    "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
                }
            } catch (ignored: NumberFormatException) {
                "${Fiats.getSymbol()}${asset.priceFiat().priceFormat()}"
            }

            walletViewModel.marketById(asset.assetId).observe(viewLifecycleOwner) { market ->
                if (market != null) {
                    val priceChangePercentage24H = BigDecimal(market.priceChangePercentage24H)
                    val isRising = priceChangePercentage24H >= BigDecimal.ZERO
                    rise.setQuoteText("${(priceChangePercentage24H).numberFormat2()}%", isRising)
                } else if (asset.priceUsd == "0") {
                    rise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                    rise.text = "0.00%"
                } else if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
                    val isRising = changeUsd >= BigDecimal.ZERO
                    rise.setQuoteText("${(changeUsd * BigDecimal(100)).numberFormat2()}%", isRising)
                }
            }
            transactionsTitleLl.setOnClickListener {
                view.navigate(
                    R.id.action_transactions_fragment_to_all_transactions_fragment,
                    Bundle().apply {
                        putParcelable(ARGS_TOKEN, asset)
                        putStringArrayList(ARGS_ASSET_IDS, ArrayList(selectedAssets.map { it.assetId }))
                    },
                )
            }
            earnCard.setOnClickListener {
                openEarnHome()
            }
            transactionsRv.listener = this@TransactionsFragment
            bottomCard.post {
                bottomCard.isVisible = true
                updateBottomEmptyHeight()
                transactionsRv.list = snapshotItems
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
            marketRl.setOnClickListener {
                lifecycleScope.launch {
                    if (fromMarket) {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                        return@launch
                    }
                    var market = walletViewModel.findMarketItemByAssetId(asset.assetId)
                    if (market == null) {
                        jobManager.addJobInBackground(RefreshMarketJob(asset.assetId))
                        market = MarketItem(
                            "", asset.name, asset.symbol, asset.iconUrl, asset.priceUsd,
                            "", "", "", "", "", runCatching {
                                (BigDecimal(asset.priceUsd) * BigDecimal(asset.changeUsd)).toPlainString()
                            }.getOrNull() ?: "0", "", asset.changeUsd, "", "", "", "", "", "", "", "", "",
                            "", "", "", "", listOf(asset.assetId), "", "", null
                        )
                    }
                    view.navigate(
                        R.id.action_transactions_to_market_details,
                        Bundle().apply {
                            putParcelable(ARGS_MARKET, market)
                            putString(ARGS_ASSET_ID, asset.assetId)
                            putString(ARGS_MARKET_SOURCE, AnalyticsTracker.MarketSource.TOKEN_DETAIL)
                        },
                    )
                }
            }
        }

        snapshotAssetIds.switchMap { walletViewModel.snapshotsLimit(it) }.observe(viewLifecycleOwner) { list ->
            binding.apply {
                transactionsRv.isVisible = list.isNotEmpty()
                bottomRl.isVisible = list.isEmpty()
                if (snapshotItems != list) {
                    snapshotJob?.cancel()
                    val chains = groupedAssets.associate { it.assetId to it.chainId }
                    snapshotJob = viewLifecycleOwner.lifecycleScope.launch {
                        val items = withContext(Dispatchers.IO) {
                            list.map {
                                if (!it.withdrawal?.receiver.isNullOrBlank()) {
                                    val receiver = it.withdrawal.receiver
                                    val index: Int = receiver.indexOf(":")
                                    if (index == -1) {
                                        it.label = walletViewModel.findAddressByReceiver(receiver, "", chains[it.assetId])
                                    } else {
                                        val destination: String = receiver.substring(0, index)
                                        val tag: String = receiver.substring(index + 1)
                                        it.label = walletViewModel.findAddressByReceiver(destination, tag, chains[it.assetId])
                                    }
                                }
                                it
                            }
                        }
                        snapshotItems = items
                        transactionsRv.list = items
                    }
                }
            }
        }

        walletViewModel.groupedAssetItems(asset.assetId).observe(viewLifecycleOwner) { tokens ->
            if (tokens.isEmpty()) return@observe
            groupedAssets = tokens
            if (selectedNetwork != null && tokens.none { it.chainId == selectedNetwork }) selectedNetwork = null
            renderSelection()
        }
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String?>(ARGS_NETWORK)?.observe(viewLifecycleOwner) { network ->
                if (network == null) return@observe
                selectedNetwork = network
                renderSelection()
                findNavController().currentBackStackEntry?.savedStateHandle?.set<String?>(ARGS_NETWORK, null)
            }

        walletViewModel.refreshAsset(asset.assetId)
        lifecycleScope.launch {
            val depositEntry = walletViewModel.findAndSyncDepositEntry(asset.chainId, asset.assetId)
            if (depositEntry != null && depositEntry.destination.isNotBlank()) {
                refreshPendingDeposits(asset)
            }
        }
    }

    private var snapshotItems: List<SnapshotItem> = emptyList()
    private var earnProductionId: String? = null

    private var earnProducts: List<EarnProduct> = emptyList()
    private var earnJob: Job? = null

    private fun refreshEarnDetails() {
        if (!isAdded) return
        earnJob?.cancel()
        earnJob = viewLifecycleOwner.lifecycleScope.launch {
            earnProducts = runCatching {
                val response = walletViewModel.earnAccounts()
                if (response.isSuccess) response.data.orEmpty() else emptyList()
            }.getOrDefault(emptyList())
            if (viewDestroyed()) return@launch
            bindEarnDetails()
        }
    }

    private fun bindEarnDetails() {
        val details = earnProducts.toWalletEarnDetails(selectedAssets)
        binding.earnCard.isVisible = details != null
        if (details != null) {
            earnProductionId = details.productionId
            binding.earnTotalEarnings.text = usdCurrencyAmountText(details.totalEarningsUsd)
            binding.earnTotalAmountValue.text = "${tokenAmountText(details.totalPrincipal)} ${asset.symbol}"
            binding.earnPendingValue.text = "${tokenAmountText(details.yesterdayEarnings)} ${asset.symbol}"
            binding.earnRateValue.text = details.rewardRate
                ?.let { getString(R.string.cash_account_apy, it) }
                ?: getString(R.string.N_A)
        }
        updateBottomEmptyHeight()
    }

    private fun openEarnHome() {
        Uri.parse("${Scheme.APPS}/$MIXIN_EARN_USER_ID")
            .buildUpon()
            .appendQueryParameter("action", "open")
            .apply {
                earnProductionId?.let { appendQueryParameter("production", it) }
            }
            .build()
            .toString()
            .openAsUrlOrWeb(requireActivity(), null, parentFragmentManager, lifecycleScope)
    }

    private fun updateBottomEmptyHeight() {
        if (!isAdded) return
        binding.bottomCard.post {
            if (viewDestroyed()) return@post
            val earnCardHeight = if (binding.earnCard.isVisible) binding.earnCard.height + 12.dp else 0
            val remainingHeight = requireContext().screenHeight() -
                requireContext().statusBarHeight() -
                requireContext().navigationBarHeight() -
                binding.titleView.height -
                binding.topLl.height -
                binding.marketRl.height -
                (if (binding.allocationCard.isVisible) binding.allocationCard.height + 10.dp else 0) -
                (if (binding.networkTabsScroll.isVisible) binding.networkTabsScroll.height else 0) -
                earnCardHeight -
                70.dp
            binding.bottomRl.updateLayoutParams {
                height = remainingHeight.coerceAtLeast(0)
            }
        }
    }

    override fun onDestroyView() {
        _bottomBinding = null
        networkIds = emptyList()
        super.onDestroyView()
    }

    private fun refreshPendingDeposits(
        asset: TokenItem,
    ) {
        if (viewDestroyed()) return
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    walletViewModel.refreshPendingDeposits(asset.assetId)
                },
                exceptionBlock = { e ->
                    reportException(e)
                    false
                },
                successBlock = { list ->
                    withContext(Dispatchers.IO) {
                        val pendingDeposits = list.data ?: emptyList()
                        val destinationTags = walletViewModel.findDepositEntryDestinations()
                        pendingDeposits.filter { pd ->
                            destinationTags.any { dt ->
                                dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                            }
                        }.map { pd -> pd.toSnapshot() }.let { snapshots ->
                            // If there are no pending deposit snapshots belonging to the current user, clear token pending deposits
                            if (snapshots.isEmpty()) {
                                walletViewModel.clearPendingDepositsByAssetId(asset.assetId)
                                return@let
                            }
                            lifecycleScope.launch {
                                snapshots.map { it.assetId }.distinct().forEach {
                                    walletViewModel.findOrSyncAsset(it)
                                }
                                walletViewModel.insertPendingDeposit(snapshots)
                            }
                        }
                    }
                },
            )
        }
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        _bottomBinding = ViewWalletTransactionsBottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_bottom, null))
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        val assets = selectedAssets
        val hidden = shouldHideAssets(assets)
        bottomBinding.apply {
            hide.setText(if (hidden) R.string.Hide else R.string.Show)
            hide.setOnClickListener {
                AnalyticsTracker.trackAssetVisibility(hidden, TradeWallet.MAIN, AnalyticsTracker.AssetSource.ASSET_DETAIL)
                val ids = assets.map { it.assetId }
                lifecycleScope.launch(Dispatchers.IO) {
                    walletViewModel.updateAssetsHidden(ids, hidden)
                }
                bottomSheet.dismiss()
                mainThreadDelayed({ activity?.onBackPressedDispatcher?.onBackPressed() }, 200)
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    override fun <T> onNormalItemClick(item: T) {
        val snapshot = item as SnapshotItem
        viewLifecycleOwner.lifecycleScope.launch {
            val token = walletViewModel.simpleAssetItem(snapshot.assetId) ?: return@launch
            AnalyticsTracker.trackTransactionDetail(AnalyticsTracker.AssetSource.ASSET_DETAIL)
            view?.navigate(
                R.id.action_transactions_fragment_to_transaction_fragment,
                Bundle().apply {
                    putParcelable(TransactionFragment.ARGS_SNAPSHOT, snapshot)
                    putParcelable(ARGS_ASSET, token)
                },
            )
        }
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch {
            val user =
                withContext(Dispatchers.IO) {
                    walletViewModel.getUser(userId)
                } ?: return@launch

            if (user.notMessengerUser()) {
                NonMessengerUserBottomSheetDialogFragment.newInstance(user)
                    .showNow(parentFragmentManager, NonMessengerUserBottomSheetDialogFragment.TAG)
            } else {
                val f = UserBottomSheetDialogFragment.newInstance(user)
                f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onMoreClick() {
        AnalyticsTracker.trackAllTransactions(AnalyticsTracker.AssetSource.ASSET_DETAIL)
        view?.navigate(
            R.id.action_transactions_fragment_to_all_transactions_fragment,
            Bundle().apply {
                putParcelable(ARGS_TOKEN, asset)
                putStringArrayList(ARGS_ASSET_IDS, ArrayList(selectedAssets.map { it.assetId }))
            },
        )
    }

    private fun navigateToTransferDestination(asset: TokenItem) {
        AnalyticsTracker.trackAssetSendStart(TradeWallet.MAIN, AnalyticsTracker.AssetSource.ASSET_DETAIL)
        findNavController().navigate(
            R.id.action_transactions_to_transfer_destination,
            Bundle().apply {
                putParcelable(TransactionsFragment.ARGS_ASSET, asset)
            }
        )
    }

    private fun bindHeader() {
        binding.apply {
            if (asset.collectionHash.isNullOrEmpty()) {
                topRl.setOnClickListener {
                    chooseAsset { token ->
                        AssetKeyBottomSheetDialogFragment.newInstance(token)
                            .showNow(parentFragmentManager, AssetKeyBottomSheetDialogFragment.TAG)
                    }
                }
            }
            updateHeader(asset)
            sendReceiveView.send.setOnClickListener {
                val send = { chooseAsset(::navigateToTransferDestination) }
                if (!showRecoveryReminderForRiskAction(send)) send()
            }
            sendReceiveView.receive.setOnClickListener {
                if (
                    showRecoveryReminderForRiskAction {
                        AnalyticsTracker.trackAssetReceiveStart(AnalyticsTracker.AssetSource.ASSET_DETAIL, TradeWallet.MAIN)
                        sendReceiveView.navigate(
                            R.id.action_transactions_to_deposit,
                            Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                        )
                    }
                ) {
                    return@setOnClickListener
                }
                AnalyticsTracker.trackAssetReceiveStart(AnalyticsTracker.AssetSource.ASSET_DETAIL, TradeWallet.MAIN)
                sendReceiveView.navigate(
                    R.id.action_transactions_to_deposit,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                )
            }
            marketView.setContent {
                Market(asset.assetId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARGS_NETWORK, selectedNetwork)
        super.onSaveInstanceState(outState)
    }

    private fun chooseAsset(callback: (TokenItem) -> Unit) {
        val tokens = selectedAssets
        showTokenNetworks(tokens.map { it.toSwapToken() }) { selected ->
            tokens.firstOrNull { it.assetId == selected.assetId }?.let(callback)
        }
    }

    private fun renderSelection() {
        asset = selectedAssets.first()
        val chains = groupedAssets.distinctBy { it.chainId }.sortedBy { it.chainName }
        binding.networkTabsScroll.isVisible = chains.size > 1
        val ids = chains.map { it.chainId }
        if (networkIds != ids) {
            networkIds = ids
            binding.networkTabs.setOnCheckedChangeListener(null)
            binding.networkTabs.removeAllViews()
            val tabs = listOf(null to getString(R.string.All)) + chains.map { it.chainId to (it.chainName ?: it.chainSymbol ?: it.chainId) }
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
        binding.allocationCard.isVisible = chains.size > 1 && selectedNetwork == null
        binding.allocationRows.removeAllViews()
        if (binding.allocationCard.isVisible) {
            groupedAssets.sortedByDescending { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO }.forEach { token ->
                val row = ItemAssetAllocationBinding.inflate(layoutInflater, binding.allocationRows, false)
                row.avatar.loadToken(token)
                row.symbol.text = token.symbol
                row.network.text = getString(R.string.asset_on_network, token.chainName ?: token.chainSymbol ?: token.chainId)
                row.value.text = "${Fiats.getSymbol()}${token.fiat().numberFormat2()}"
                row.amount.text = "${token.balance.numberFormat()} ${token.symbol}"
                row.root.setOnClickListener {
                    selectedNetwork = token.chainId
                    renderSelection()
                }
                binding.allocationRows.addView(row.root)
            }
        }
        val selectedIds = selectedAssets.map { it.assetId }.sorted()
        if (snapshotAssetIds.value != selectedIds) {
            snapshotJob?.cancel()
            snapshotItems = emptyList()
            binding.transactionsRv.list = emptyList()
            snapshotAssetIds.value = selectedIds
        }
        binding.titleView.setSubTitle(asset.symbol, getString(R.string.Privacy_Wallet), R.drawable.ic_wallet_privacy)
        bindHeader()
        bindEarnDetails()
        updateBottomEmptyHeight()
    }

    private fun showRecoveryReminderForRiskAction(onContinue: (() -> Unit)? = null): Boolean {
        return RecoveryReminderBottomSheetDialogFragment.showForRiskAction(parentFragmentManager, onContinue)
    }

    private fun updateHeader(asset: TokenItem) {
        binding.apply {
            val group = TokenGroup(selectedAssets)
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
            avatar.badge.isVisible = false
            avatar.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                        view?.navigate(
                            R.id.action_transactions_to_utxo,
                            Bundle().apply {
                                putParcelable(ARGS_ASSET, asset)
                            },
                        )
                    }

                    override fun onSingleClick() {
                    }
                },
            )
        }
    }
}

internal fun resolveAssetDetailSource(fromMarket: Boolean, source: String?): String =
    if (fromMarket) AnalyticsTracker.AssetSource.MARKET_DETAIL else source ?: AnalyticsTracker.AssetSource.WALLET_HOME

internal fun shouldHideAssets(tokens: List<TokenItem>): Boolean = tokens.none { it.hidden == true }
