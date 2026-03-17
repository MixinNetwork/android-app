package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentPrivacyWalletBinding
import one.mixin.android.databinding.ViewWalletFragmentHeaderBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.job.RefreshWeb3TokenJob
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.reminder.VerifyMobileReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.AddPhoneBeforeFragment
import one.mixin.android.ui.wallet.adapter.WalletWeb3TokenAdapter
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeSource
import one.mixin.android.util.analytics.AnalyticsTracker.TradeWallet
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import one.mixin.android.widget.calcPercent
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.measureTime
import java.time.Instant

@AndroidEntryPoint
class ClassicWalletFragment : BaseFragment(R.layout.fragment_privacy_wallet), HeaderAdapter.OnItemListener {
    companion object {
        const val TAG = "ClassicWalletFragment"

        fun newInstance(): ClassicWalletFragment = ClassicWalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentPrivacyWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _headBinding: ViewWalletFragmentHeaderBinding? = null

    private val web3ViewModel by viewModels<Web3ViewModel>()
    private var assets: List<Web3TokenItem> = listOf()
    private val assetsAdapter by lazy { WalletWeb3TokenAdapter(false) }

    private var distance = 0
    private var snackBar: Snackbar? = null
    private var lastFiatCurrency :String? = null

    private val _walletId = MutableLiveData<String>()
    var walletId: String = ""
        set(value) {
            if (value != field) {
                field = value
                _walletId.value = value
            }
            Timber.e("walletId set to $value")
        }

    private val tokensLiveData by lazy {
        _walletId.switchMap { id ->
            if (id.isNullOrEmpty()) {
                MutableLiveData()
            } else {
                web3ViewModel.web3TokensExcludeHidden(id)
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
        _binding = FragmentPrivacyWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("onViewCreated called in ClassicWalletFragment")
        lifecycleScope.launch {
            val queryDuration = measureTime {
                val data = web3ViewModel.web3TokensExcludeHiddenRaw(walletId)
                assets = data
                Timber.e("web3TokensExcludeHiddenRaw query completed: data size: ${data.size}, walletId: $walletId")
            }
            Timber.e("web3TokensExcludeHiddenRaw query took: $queryDuration")
            if (isAdded) {
                assetsAdapter.setAssetList(assets)
                if (lastFiatCurrency != Session.getFiatCurrency()) {
                    lastFiatCurrency = Session.getFiatCurrency()
                    assetsAdapter.notifyDataSetChanged()
                }

                val bitcoin = web3ViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                renderPie(assets, bitcoin)
            }
        }

        binding.apply {
            _headBinding =
                ViewWalletFragmentHeaderBinding.bind(layoutInflater.inflate(R.layout.view_wallet_fragment_header, coinsRv, false)).apply {
                    sendReceiveView.enableBuy()
                    sendReceiveView.buy.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                            if (wallet?.isImported() == true && !wallet.hasLocalPrivateKey) {
                                ImportKeyBottomSheetDialogFragment.newInstance(
                                    if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase else ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey,
                                    walletId = walletId, chainId = chainId
                                ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
                                return@launch
                            }
                            WalletActivity.showBuy(requireActivity(), true, null, null, walletId)
                        }
                    }
                    sendReceiveView.send.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                            if (wallet?.isImported() == true && !wallet.hasLocalPrivateKey) {
                                ImportKeyBottomSheetDialogFragment.newInstance(
                                    if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase else ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey,
                                    walletId = walletId, chainId = chainId
                                ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
                                return@launch
                            }
                            Web3TokenListBottomSheetDialogFragment.newInstance(walletId = walletId, TYPE_FROM_SEND).apply {
                                setOnClickListener { token ->
                                    this@ClassicWalletFragment.lifecycleScope.launch {
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
                                        WalletActivity.navigateToWalletActivity(this@ClassicWalletFragment.requireActivity(), address?.destination, token, chain, wallet)
                                    }
                                    dismissNow()
                                }
                            }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
                        }
                    }
                    sendReceiveView.receive.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            if (wallet?.isImported() == true && !wallet.hasLocalPrivateKey) {
                                val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                                ImportKeyBottomSheetDialogFragment.newInstance(
                                    if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase else ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey,
                                    walletId = walletId, chainId
                                ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
                                return@launch
                            }
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
                    }
                    sendReceiveView.swap.setOnClickListener {
                        lifecycleScope.launch {
                            val wallet = web3ViewModel.findWalletById(walletId)
                            if (wallet?.isImported() == true && !wallet.hasLocalPrivateKey) {
                                val chainId = web3ViewModel.getAddresses(walletId).first().chainId
                                ImportKeyBottomSheetDialogFragment.newInstance(
                                    if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value) ImportKeyBottomSheetDialogFragment.PopupType.ImportMnemonicPhrase else ImportKeyBottomSheetDialogFragment.PopupType.ImportPrivateKey,
                                    walletId = walletId, chainId = chainId
                                ).showNow(parentFragmentManager, ImportKeyBottomSheetDialogFragment.TAG)
                                return@launch
                            }
                            AnalyticsTracker.trackTradeStart(TradeWallet.WEB3, TradeSource.WALLET_HOME)
                            SwapActivity.show(requireActivity(), inMixin = false, walletId = walletId)
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
            assetsAdapter.onItemListener = this@ClassicWalletFragment

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
                    val wallet = web3ViewModel.findWalletById(id)
                    _headBinding?.sendReceiveView?.isVisible = wallet?.isWatch() == false
                    _headBinding?.watchLayout?.isVisible = wallet?.isWatch() == true

                    if (wallet?.isWatch() == true) {
                        val addresses = web3ViewModel.getAddressesGroupedByDestination(id)
                        if (addresses.isNotEmpty()) {
                            if (addresses.size == 1) {
                                val address = addresses.first().destination
                                _headBinding?.watchTv?.text = getString(R.string.watching_address, "${address.take(6)}..${address.takeLast(4)}")
                            } else {
                                _headBinding?.watchTv?.text = getString(R.string.watching_addresses, addresses.size)
                            }
                        }
                    }
                }
            }
        }

        _headBinding?.web3PendingView?.observePendingCount(viewLifecycleOwner, pendingTxCountLiveData)
        tokensLiveData.observe(viewLifecycleOwner, observer)

        RxBus.listen(QuoteColorEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { _ ->
                assetsAdapter.notifyDataSetChanged()
            }
    }

    private val observer = Observer<List<Web3TokenItem>> { data ->
        Timber.e("observe web3TokensExcludeHidden data size: ${data.size}, walletId: $walletId")
        if (data.isEmpty()) {
            setEmpty()
            assets = data
            assetsAdapter.setAssetList(data)
            if (lastFiatCurrency != Session.getFiatCurrency()) {
                lastFiatCurrency = Session.getFiatCurrency()
                assetsAdapter.notifyDataSetChanged()
            }
        } else {
            assets = data
            assetsAdapter.setAssetList(data)
            if (lastFiatCurrency != Session.getFiatCurrency()) {
                lastFiatCurrency = Session.getFiatCurrency()
                assetsAdapter.notifyDataSetChanged()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val bitcoin = web3ViewModel.findOrSyncAsset(Constants.ChainId.BITCOIN_CHAIN_ID)
                renderPie(assets, bitcoin)
            }
        }
    }

    fun update() {
        jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
        if (walletId.isEmpty().not()) {
            jobManager.addJobInBackground(RefreshWeb3TokenJob(walletId = walletId))
        }
    }

    override fun onResume() {
        super.onResume()
        jobManager.addJobInBackground(RefreshSingleWalletJob(Web3Signer.currentWalletId))
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
                this@ClassicWalletFragment.lifecycleScope.launch {
                    val address = web3ViewModel.getAddressesByChainId(this@ClassicWalletFragment.walletId, token.chainId)
                    WalletActivity.showWithAddress(this@ClassicWalletFragment.requireActivity(), address?.destination, token, WalletActivity.Destination.Address)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
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
