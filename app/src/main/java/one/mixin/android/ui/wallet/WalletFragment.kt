package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_wallet_bottom.view.*
import kotlinx.android.synthetic.main.view_wallet_fragment_header.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.PrivacyPreference.getPrefPinInterval
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.navigate
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.AssetItemCallback
import one.mixin.android.ui.wallet.adapter.WalletAssetAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PercentItemView
import one.mixin.android.widget.PercentView
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@AndroidEntryPoint
class WalletFragment : BaseFragment(), HeaderAdapter.OnItemListener {

    companion object {
        const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment = WalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()
    private var assets: List<AssetItem> = listOf()
    private val assetsAdapter by lazy { WalletAssetAdapter(false) }
    private lateinit var header: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshAssetsJob())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.rightAnimator.setOnClickListener { showBottom() }
        title_view.leftIb.setOnClickListener { activity?.onBackPressed() }
        search_ib.setOnClickListener { view.navigate(R.id.action_wallet_to_wallet_search) }

        header = layoutInflater.inflate(R.layout.view_wallet_fragment_header, coins_rv, false)
        assetsAdapter.headerView = header
        (coins_rv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        coins_rv.setHasFixedSize(true)
        ItemTouchHelper(
            AssetItemCallback(
                object : AssetItemCallback.ItemCallbackListener {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
                        val hiddenPos = viewHolder.absoluteAdapterPosition
                        val asset = assetsAdapter.data!![assetsAdapter.getPosition(hiddenPos)]
                        val deleteItem = assetsAdapter.removeItem(hiddenPos)!!
                        lifecycleScope.launch {
                            walletViewModel.updateAssetHidden(asset.assetId, true)
                            val anchorView = coins_rv ?: return@launch

                            Snackbar.make(anchorView, getString(R.string.wallet_already_hidden, asset.symbol), Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo_capital) {
                                    assetsAdapter.restoreItem(deleteItem, hiddenPos)
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        walletViewModel.updateAssetHidden(asset.assetId, false)
                                    }
                                }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                                    this.view.setBackgroundResource(R.color.call_btn_icon_checked)
                                    (this.view.findViewById(R.id.snackbar_text) as TextView)
                                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                                }.show()
                        }
                    }
                }
            )
        ).apply { attachToRecyclerView(coins_rv) }
        assetsAdapter.onItemListener = this
        coins_rv.adapter = assetsAdapter

        walletViewModel.assetItems().observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                setEmpty()
            } else {
                assets = it
                assetsAdapter.setAssetList(it)
                lifecycleScope.launch(Dispatchers.IO) {
                    renderPie(assets)
                }
            }
        }
        checkPin()
    }

    private suspend fun renderPie(assets: List<AssetItem>) {
        var totalBTC = BigDecimal.ZERO
        var totalFiat = BigDecimal.ZERO
        assets.map {
            totalBTC = totalBTC.add(it.btc())
            totalFiat = totalFiat.add(it.fiat())
        }
        withContext(Dispatchers.Main) {
            header.total_as_tv.text = try {
                if (totalBTC.numberFormat8().toFloat() == 0f) {
                    "0.00"
                } else {
                    totalBTC.numberFormat8()
                }
            } catch (ignored: NumberFormatException) {
                totalBTC.numberFormat8()
            }
            header.total_tv.text = try {
                if (totalFiat.numberFormat2().toFloat() == 0f) {
                    "0.00"
                } else {
                    totalFiat.numberFormat2()
                }
            } catch (ignored: NumberFormatException) {
                totalFiat.numberFormat2()
            }
            header.symbol.text = Fiats.getSymbol()

            if (totalFiat.compareTo(BigDecimal.ZERO) == 0) {
                header.pie_item_container.visibility = GONE
                header.percent_view.visibility = GONE
                header.btc_rl.updateLayoutParams<LinearLayout.LayoutParams> {
                    bottomMargin = requireContext().dpToPx(32f)
                }
                return@withContext
            }

            header.btc_rl.updateLayoutParams<LinearLayout.LayoutParams> {
                bottomMargin = requireContext().dpToPx(16f)
            }
            header.pie_item_container.visibility = VISIBLE
            header.percent_view.visibility = VISIBLE
            setPieView(assets, totalFiat)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setEmpty() {
        header.pie_item_container.visibility = GONE
        header.percent_view.visibility = GONE
        assetsAdapter.setAssetList(emptyList())
        header.total_as_tv.text = "0.00"
        header.total_tv.text = "0.00"
    }

    private fun setPieView(r: List<AssetItem>, totalUSD: BigDecimal) {
        val list = r.asSequence().filter { BigDecimal(it.balance).compareTo(BigDecimal.ZERO) != 0 }.map {
            val p = (it.fiat() / totalUSD).setScale(2, RoundingMode.DOWN).toFloat()
            PercentView.PercentItem(it.symbol, p)
        }.toMutableList()
        if (list.isNotEmpty()) {
            header.pie_item_container.removeAllViews()
            list.sortWith { o1, o2 -> ((o2.percent - o1.percent) * 100).toInt() }
            context?.mainThread {
                header.percent_view.setPercents(list)
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
                    addItem(PercentView.PercentItem(getString(R.string.other), 0.01f), 1)
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
                    item.setPercentItem(PercentView.PercentItem(getString(R.string.other), other), 2)
                    header.pie_item_container.addView(item)
                }
            }

            header.pie_item_container.visibility = VISIBLE
        }
    }

    private fun checkPin() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_CHECK, 0)
        var interval = getPrefPinInterval(requireContext(), 0)
        val account = Session.getAccount()
        if (account != null && account.hasPin && last == 0L) {
            interval = Constants.INTERVAL_24_HOURS
            putPrefPinInterval(requireContext(), Constants.INTERVAL_24_HOURS)
        }
        if (cur - last > interval) {
            val pinCheckDialog = PinCheckDialogFragment.newInstance()
            pinCheckDialog.show(parentFragmentManager, PinCheckDialogFragment.TAG)
        }
    }

    private fun addItem(p: PercentView.PercentItem, index: Int) {
        val item = PercentItemView(requireContext())
        item.setPercentItem(p, index)
        header.pie_item_container.addView(item)
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        val rootView = this.view
        view.hide.setOnClickListener {
            rootView?.navigate(R.id.action_wallet_fragment_to_hidden_assets_fragment)
            bottomSheet.dismiss()
        }
        view.transactions_tv.setOnClickListener {
            rootView?.navigate(R.id.action_wallet_fragment_to_all_transactions_fragment)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    override fun <T> onNormalItemClick(item: T) {
        item as AssetItem
        view?.navigate(
            R.id.action_wallet_fragment_to_transactions_fragment,
            Bundle().apply { putParcelable(ARGS_ASSET, item) }
        )
    }
}
