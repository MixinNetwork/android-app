package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_HAS_USED_BUY
import one.mixin.android.Constants.Account.PREF_HAS_USED_SWAP
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentPrivacyWalletBinding
import one.mixin.android.databinding.ViewWalletFragmentHeaderBinding
import one.mixin.android.event.BadgeEvent
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.reminder.VerifyMobileReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.AddPhoneBeforeFragment
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.ui.wallet.adapter.WalletAssetAdapter
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.util.reportException
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import one.mixin.android.widget.calcPercent
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.measureTime

@AndroidEntryPoint
class PrivacyWalletFragment : BaseFragment(R.layout.fragment_privacy_wallet), HeaderAdapter.OnItemListener {
    companion object {
        const val TAG = "PrivacyWalletFragment"

        fun newInstance(): PrivacyWalletFragment = PrivacyWalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentPrivacyWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _headBinding: ViewWalletFragmentHeaderBinding? = null

    private val walletViewModel by viewModels<WalletViewModel>()
    private var assets: List<TokenItem> = listOf()
    private val assetsAdapter by lazy { WalletAssetAdapter(false) }

    private var distance = 0
    private var snackBar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPrivacyWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("onViewCreated called in PrivacyWalletFragment")
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
                var bitcoin = assets.find { a -> a.assetId == Constants.ChainId.BITCOIN_CHAIN_ID }
                if (bitcoin == null) {
                    bitcoin = walletViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                }
                renderPie(assets, bitcoin)
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
                        TokenListBottomSheetDialogFragment.newInstance(TYPE_FROM_SEND)
                            .setOnAssetClick {
                                WalletActivity.navigateToWalletActivity(this@PrivacyWalletFragment.requireActivity(), it)
                            }.setOnDepositClick {
                                // do nothing
                            }
                            .showNow(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                    }
                    sendReceiveView.receive.setOnClickListener {
                        if (!Session.saltExported() && Session.isAnonymous()) {
                            BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                                .apply {
                                    laterCallback = {
                                        showReceiveAssetList()
                                    }
                                }
                                .show(parentFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
                        } else {
                            showReceiveAssetList()
                        }
                    }
                    sendReceiveView.swap.setOnClickListener {
                        AnalyticsTracker.trackTradeStart(TradeWallet.MAIN, TradeSource.WALLET_HOME)
                        SwapActivity.show(requireActivity(), inMixin = true)
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
            if (it.isEmpty()) {
                setEmpty()
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
                }
            }
        }

        walletViewModel.getPendingDisplays().observe(viewLifecycleOwner) {
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
    }

    override fun onResume() {
        super.onResume()
        jobManager.addJobInBackground(RefreshTokensJob())
        jobManager.addJobInBackground(RefreshSnapshotsJob())
        jobManager.addJobInBackground(SyncOutputJob())
        refreshAllPendingDeposit()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
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

    override fun <T> onNormalItemClick(item: T) {
        WalletActivity.showWithToken(requireActivity(), item as TokenItem, WalletActivity.Destination.Transactions)
    }
}
