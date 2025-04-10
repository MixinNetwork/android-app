package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.crypto.PrivacyPreference.getPrefPinInterval
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.databinding.FragmentPrivacyWalletBinding
import one.mixin.android.databinding.ViewWalletFragmentHeaderBinding
import one.mixin.android.event.BadgeEvent
import one.mixin.android.event.QuoteColorEvent
import one.mixin.android.extension.config
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.web3.swap.SwapFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_SEND
import one.mixin.android.ui.wallet.adapter.AssetItemCallback
import one.mixin.android.ui.wallet.adapter.WalletAssetAdapter
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.reportException
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import one.mixin.android.widget.calcPercent
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.abs

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
        binding.apply {
            _headBinding =
                ViewWalletFragmentHeaderBinding.bind(layoutInflater.inflate(R.layout.view_wallet_fragment_header, coinsRv, false)).apply {
                    sendReceiveView.send.setOnClickListener {
                        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_SEND)
                            .setOnAssetClick {
                                navTo(TransferDestinationInputFragment.newInstance(it),
                                    TransferDestinationInputFragment.TAG)
                            }.setOnDepositClick {
                                showReceiveAssetList()
                            }
                            .showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
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
                        AnalyticsTracker.trackSwapStart("mixin", "wallet")
                        navTo(SwapFragment.newInstance<TokenItem>(), SwapFragment.TAG)
                        if (defaultSharedPreferences.getInt(Constants.Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) != 0) {
                            sendReceiveView.badge.isVisible = false
                            RxBus.publish(BadgeEvent(Account.PREF_HAS_USED_SWAP))
                        }
                    }
                }
            assetsAdapter.headerView = _headBinding!!.root
            coinsRv.itemAnimator = null
            coinsRv.setHasFixedSize(true)
            ItemTouchHelper(
                AssetItemCallback(
                    object : AssetItemCallback.ItemCallbackListener {
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
                            val hiddenPos = viewHolder.absoluteAdapterPosition
                            val asset = assetsAdapter.data!![assetsAdapter.getPosition(hiddenPos)]
                            val deleteItem = assetsAdapter.removeItem(hiddenPos)!!
                            lifecycleScope.launch {
                                walletViewModel.updateAssetHidden(asset.assetId, true)
                                val anchorView = coinsRv

                                snackBar =
                                    Snackbar.make(anchorView, getString(R.string.wallet_already_hidden, asset.symbol), 3500)
                                        .setAction(R.string.UNDO) {
                                            assetsAdapter.restoreItem(deleteItem, hiddenPos)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                walletViewModel.updateAssetHidden(asset.assetId, false)
                                            }
                                        }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                                            (this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)!!)
                                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                                        }.apply {
                                            snackBar?.config(anchorView.context)
                                        }
                                snackBar?.show()
                                distance = 0
                            }
                        }
                    },
                ),
            ).apply { attachToRecyclerView(coinsRv) }
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

        val swap = defaultSharedPreferences.getBoolean(Account.PREF_HAS_USED_SWAP, true) || defaultSharedPreferences.getInt(Constants.Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) == 0
        _headBinding?.sendReceiveView?.badge?.isVisible = swap

        RxBus.listen(BadgeEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { e ->
                lifecycleScope.launch{
                    when (e.badge) {
                        Account.PREF_HAS_USED_SWAP -> {
                            _headBinding?.sendReceiveView?.badge?.isVisible = defaultSharedPreferences.getBoolean(Account.PREF_HAS_USED_SWAP, true) || defaultSharedPreferences.getInt(Constants.Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) == 0
                        }
                    }
                }
            }
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
                    val pendingDeposits = it.data
                    if (pendingDeposits.isNullOrEmpty()) {
                        walletViewModel.clearAllPendingDeposits()
                        return@handleMixinResponse
                    }
                    val destinationTags = walletViewModel.findDepositEntryDestinations()
                    pendingDeposits
                        .filter { pd ->
                            destinationTags.any { dt ->
                                dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                            }
                        }
                        .map { pd -> pd.toSnapshot() }.let { snapshots ->
                            lifecycleScope.launch {
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
        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_RECEIVE)
            .setOnAssetClick { asset ->
                WalletActivity.showWithToken(requireActivity(), asset, WalletActivity.Destination.Deposit)
            }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
    }

    override fun <T> onNormalItemClick(item: T) {
        WalletActivity.showWithToken(requireActivity(), item as TokenItem, WalletActivity.Destination.Transactions)
    }
}
