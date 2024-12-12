package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID
import one.mixin.android.Constants.AssetId.XIN_ASSET_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentTransactionsBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.setQuoteText
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.job.RefreshPriceJob
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.swap.SwapFragment
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_ASSET_ID
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
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
    }

    private val binding by viewBinding(FragmentTransactionsBinding::bind)
    private var _bottomBinding: ViewWalletTransactionsBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }
    private val sendBottomSheet = SendBottomSheet(this, R.id.action_transactions_to_single_friend_select, R.id.action_transactions_to_address_management)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    lateinit var asset: TokenItem

    private val fromMarket by lazy {
        requireArguments().getBoolean(ARGS_FROM_MARKET, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)!!
    }

    private var scrollY = 0

    override fun onPause() {
        super.onPause()
        scrollY = binding.scrollView.scrollY
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(asset.assetId))))
        jobManager.addJobInBackground(RefreshPriceJob(asset.assetId))

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
                lifecycleScope.launch {
                    val assets = walletViewModel.allAssetItems()
                    val output = if (asset.assetId == USDT_ASSET_ID) {
                        XIN_ASSET_ID
                    } else {
                        USDT_ASSET_ID
                    }
                    navTo(SwapFragment.newInstance<TokenItem>(assets, input = asset.assetId, output = output), SwapFragment.TAG)
                }
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
                    },
                )
            }
            transactionsRv.listener = this@TransactionsFragment
            bottomCard.post {
                bottomCard.isVisible = true
                val remainingHeight = requireContext().screenHeight() - requireContext().statusBarHeight() - requireContext().navigationBarHeight() - titleView.height - topLl.height - marketRl.height - 70.dp
                bottomRl.updateLayoutParams {
                    height = remainingHeight
                }
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
                        },
                    )
                }
            }
        }

        walletViewModel.snapshotsLimit(asset.assetId).observe(viewLifecycleOwner) { list ->
            binding.apply {
                transactionsRv.isVisible = list.isNotEmpty()
                bottomRl.isVisible = list.isEmpty()
                if (snapshotItems != list) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        snapshotItems = list.map {
                            if (!it.withdrawal?.receiver.isNullOrBlank()) {
                                val receiver = it.withdrawal!!.receiver
                                val index: Int = receiver.indexOf(":")
                                if (index == -1) {
                                    it.label = walletViewModel.findAddressByReceiver(receiver, "")
                                } else {
                                    val destination: String = receiver.substring(0, index)
                                    val tag: String = receiver.substring(index + 1)
                                    it.label = walletViewModel.findAddressByReceiver(destination, tag)
                                }
                            }
                            it
                        }
                        withContext(Dispatchers.Main) {
                            transactionsRv.list = snapshotItems
                        }
                    }
                }
            }
        }

        walletViewModel.assetItem(asset.assetId).observe(
            viewLifecycleOwner,
        ) { assetItem ->
            assetItem?.let {
                asset = it
                bindHeader()
            }
        }

        walletViewModel.refreshAsset(asset.assetId)
        lifecycleScope.launch {
            val depositEntry = walletViewModel.findDepositEntry(asset.chainId)
            if (depositEntry != null && depositEntry.destination.isNotBlank()) {
                refreshPendingDeposits(asset, depositEntry)
            }
        }
    }

    private var snapshotItems: List<SnapshotItem> = emptyList()

    override fun onDestroyView() {
        _bottomBinding = null
        sendBottomSheet.release()
        super.onDestroyView()
    }

    private fun refreshPendingDeposits(
        asset: TokenItem,
        depositEntry: DepositEntry,
    ) {
        if (viewDestroyed()) return
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    walletViewModel.refreshPendingDeposits(asset.assetId, depositEntry)
                },
                successBlock = { list ->
                    withContext(Dispatchers.IO) {
                        walletViewModel.clearPendingDepositsByAssetId(asset.assetId)
                        val pendingDeposits = list.data
                        if (pendingDeposits.isNullOrEmpty()) {
                            return@withContext
                        }

                        pendingDeposits.chunked(100) { chunk ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                chunk.map {
                                    it.toSnapshot()
                                }.let {
                                    walletViewModel.insertPendingDeposit(it)
                                }
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
        bottomBinding.apply {
            hide.setText(if (asset.hidden == true) R.string.Show else R.string.Hide)
            hide.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    walletViewModel.updateAssetHidden(asset.assetId, asset.hidden != true)
                }
                bottomSheet.dismiss()
                mainThreadDelayed({ activity?.onBackPressedDispatcher?.onBackPressed() }, 200)
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    override fun <T> onNormalItemClick(item: T) {
        view?.navigate(
            R.id.action_transactions_fragment_to_transaction_fragment,
            Bundle().apply {
                putParcelable(TransactionFragment.ARGS_SNAPSHOT, item as SnapshotItem)
                putParcelable(ARGS_ASSET, asset)
            },
        )
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
        view?.navigate(
            R.id.action_transactions_fragment_to_all_transactions_fragment,
            Bundle().apply {
                putParcelable(ARGS_TOKEN, asset)
            },
        )
    }

    private fun bindHeader() {
        binding.apply {
            if (asset.collectionHash.isNullOrEmpty()) {
                topRl.setOnClickListener {
                    AssetKeyBottomSheetDialogFragment.newInstance(asset)
                        .showNow(parentFragmentManager, AssetKeyBottomSheetDialogFragment.TAG)
                }
            }
            updateHeader(asset)
            sendReceiveView.send.setOnClickListener {
                sendBottomSheet.show(asset)
            }
            sendReceiveView.receive.setOnClickListener {
                if (!Session.saltExported() && Session.isAnonymous()) {
                    BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                        .apply {
                            laterCallback = {
                                sendReceiveView.navigate(
                                    R.id.action_transactions_to_deposit,
                                    Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                                )
                            }
                        }
                        .show(parentFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
                } else {
                    sendReceiveView.navigate(
                        R.id.action_transactions_to_deposit,
                        Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                    )
                }
            }
            marketView.setContent {
                Market(asset.assetId)
            }
        }
    }

    private fun updateHeader(asset: TokenItem) {
        binding.apply {
            val amountText =
                try {
                    if (asset.balance.toFloat() == 0f) {
                        "0.00"
                    } else {
                        asset.balance.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    asset.balance.numberFormat()
                }
            val color = requireContext().colorFromAttribute(R.attr.text_primary)
            balance.text = buildAmountSymbol(requireContext(), amountText, asset.symbol, color, color)
            balanceAs.text =
                try {
                    if (asset.fiat().toFloat() == 0f) {
                        "≈ ${Fiats.getSymbol()}0.00"
                    } else {
                        "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                    }
                } catch (ignored: NumberFormatException) {
                    "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                }
            avatar.loadToken(asset)
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
